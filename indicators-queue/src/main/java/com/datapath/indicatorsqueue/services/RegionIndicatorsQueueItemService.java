package com.datapath.indicatorsqueue.services;

import com.datapath.indicatorsqueue.domain.IndicatorsImpactRange;
import com.datapath.indicatorsqueue.domain.PageableResource;
import com.datapath.indicatorsqueue.domain.RegionIndicatorsQueueItemDTO;
import com.datapath.indicatorsqueue.exceptions.InvalidIndicatorsImpactRangeException;
import com.datapath.indicatorsqueue.mappers.GeneralBeanMapper;
import com.datapath.persistence.entities.queue.RegionIndicatorsQueueItem;
import com.datapath.persistence.repositories.queue.RegionIndicatorsQueueItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RegionIndicatorsQueueItemService {

    private RegionIndicatorsQueueItemRepository regionIndicatorsQueueItemRepository;
    private IndicatorsQueueRegionProvider indicatorsQueueRegionProvider;

    public RegionIndicatorsQueueItemService(RegionIndicatorsQueueItemRepository regionIndicatorsQueueItemRepository,
                                            IndicatorsQueueRegionProvider indicatorsQueueRegionProvider) {
        this.regionIndicatorsQueueItemRepository = regionIndicatorsQueueItemRepository;
        this.indicatorsQueueRegionProvider = indicatorsQueueRegionProvider;
    }

    public PageableResource<RegionIndicatorsQueueItemDTO> getAllIndicatorsQueueItems(Integer page,
                                                                                     Integer limit,
                                                                                     String region) {
        List<String> regions = Collections.singletonList(region);

        PageRequest pageRequest = PageRequest.of(page, limit);
        Page<RegionIndicatorsQueueItem> queuePage = regionIndicatorsQueueItemRepository
                .findAllByRegionInOrderByMaterialityScoreDesc(regions, pageRequest);

        Integer numberOfTopRiskedTenders = regionIndicatorsQueueItemRepository
                .countByTopRiskIsTrueAndRegionIn(regions);

        return createIndicatorsQueuePage(queuePage, numberOfTopRiskedTenders);
    }

    public PageableResource<RegionIndicatorsQueueItemDTO> getAllIndicatorsQueueItemsByImpactRange(Integer page,
                                                                                                  Integer limit,
                                                                                                  IndicatorsImpactRange impactRange,
                                                                                                  String region) {
        List<String> regions = Collections.singletonList(region);

        PageRequest pageRequest = PageRequest.of(page, limit);

        if (impactRange.getMax() != null && impactRange.getMin() != null) {
            Page<RegionIndicatorsQueueItem> queuePage = regionIndicatorsQueueItemRepository
                    .findAllByTenderScoreGreaterThanEqualAndTenderScoreLessThanAndRegionInOrderByMaterialityScoreDesc(
                            impactRange.getMin(),
                            impactRange.getMax(),
                            regions,
                            pageRequest
                    );

            Integer numberOfTopRiskedTenders = regionIndicatorsQueueItemRepository
                    .countByTopRiskIsTrueAndTenderScoreGreaterThanEqualAndTenderScoreLessThanAndRegionIn(
                            impactRange.getMin(),
                            impactRange.getMax(),
                            regions
                    );

            return createIndicatorsQueuePage(queuePage, numberOfTopRiskedTenders);
        }

        if (impactRange.getMax() == null && impactRange.getMin() != null) {
            Page<RegionIndicatorsQueueItem> queuePage = regionIndicatorsQueueItemRepository
                    .findAllByTenderScoreGreaterThanEqualAndRegionInOrderByMaterialityScoreDesc(impactRange.getMin(), regions, pageRequest);

            Integer numberOfTopRiskedTenders = regionIndicatorsQueueItemRepository
                    .countByTopRiskIsTrueAndTenderScoreGreaterThanEqualAndRegionIn(
                            impactRange.getMin(),
                            regions
                    );
            return createIndicatorsQueuePage(queuePage, numberOfTopRiskedTenders);
        }

        if (impactRange.getMin() == null && impactRange.getMax() != null) {
            Page<RegionIndicatorsQueueItem> queuePage = regionIndicatorsQueueItemRepository
                    .findAllByTenderScoreLessThanAndRegionInOrderByMaterialityScoreDesc(impactRange.getMax(), regions, pageRequest);

            Integer numberOfTopRiskedTenders = regionIndicatorsQueueItemRepository
                    .countByTopRiskIsTrueAndTenderScoreLessThanAndRegionIn(
                            impactRange.getMax(),
                            regions
                    );

            return createIndicatorsQueuePage(queuePage, numberOfTopRiskedTenders);
        }

        throw new InvalidIndicatorsImpactRangeException(impactRange);
    }

    public List<RegionIndicatorsQueueItem> saveAll(List<RegionIndicatorsQueueItem> indicatorsQueueItems) {
        return regionIndicatorsQueueItemRepository.saveAll(indicatorsQueueItems);
    }

    public void deleteAll() {
        regionIndicatorsQueueItemRepository.deleteAllInBatch();
    }

    private PageableResource<RegionIndicatorsQueueItemDTO> createIndicatorsQueuePage(Page<RegionIndicatorsQueueItem> page,
                                                                                     Integer numberOfTopRiskedTenders) {
        List<RegionIndicatorsQueueItemDTO> content = page.getContent().stream()
                .map(item -> (RegionIndicatorsQueueItemDTO) GeneralBeanMapper.map(
                        item, RegionIndicatorsQueueItemDTO.class))
                .peek(item -> item.setImpactCategory(resolveImpactCategory(item.getTenderScore())))
                .collect(Collectors.toList());

        PageableResource<RegionIndicatorsQueueItemDTO> queuePage = new PageableResource<>();
        queuePage.setContent(content);
        queuePage.setTotalPages(page.getTotalPages());
        queuePage.setTotalElements(page.getTotalElements());
        queuePage.setNumberOfTopRiskedTenders(numberOfTopRiskedTenders);

        return queuePage;
    }

    public Set<String> getRegions() {
        return this.indicatorsQueueRegionProvider.getRegions();
    }

    private String resolveImpactCategory(Double tenderScore) {
        if (tenderScore < 0.5) {
            return "low";
        }
        if (tenderScore <= 1.2) {
            return "medium";
        }

        return "high";
    }
}
