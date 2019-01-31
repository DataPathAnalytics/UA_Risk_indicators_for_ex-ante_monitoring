package com.datapath.indicatorsqueue.services;

import com.datapath.indicatorsqueue.domain.IndicatorsImpactRange;
import com.datapath.indicatorsqueue.domain.IndicatorsQueueItemDTO;
import com.datapath.indicatorsqueue.domain.PageableResource;
import com.datapath.indicatorsqueue.exceptions.InvalidIndicatorsImpactRangeException;
import com.datapath.indicatorsqueue.mappers.GeneralBeanMapper;
import com.datapath.persistence.entities.queue.IndicatorsQueueItem;
import com.datapath.persistence.repositories.queue.IndicatorsQueueItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class IndicatorsQueueItemService {

    private IndicatorsQueueItemRepository indicatorsQueueItemRepository;

    public IndicatorsQueueItemService(IndicatorsQueueItemRepository indicatorsQueueItemRepository) {
        this.indicatorsQueueItemRepository = indicatorsQueueItemRepository;
    }

    public PageableResource<IndicatorsQueueItemDTO> getAllIndicatorsQueueItems(Integer page,
                                                                               Integer limit,
                                                                               List<String> regions) {
        PageRequest pageRequest = PageRequest.of(page, limit);
        Page<IndicatorsQueueItem> queuePage = regions == null ?
                indicatorsQueueItemRepository.findAllByOrderByMaterialityScoreDesc(pageRequest)
                : indicatorsQueueItemRepository.findAllByRegionInOrderByMaterialityScoreDesc(regions, pageRequest);

        Integer numberOfTopRiskedTenders = regions == null ?
                indicatorsQueueItemRepository.countByTopRiskIsTrue()
                : indicatorsQueueItemRepository.countByTopRiskIsTrueAndRegionIn(regions);

        return createIndicatorsQueuePage(queuePage, numberOfTopRiskedTenders);
    }

    public PageableResource<IndicatorsQueueItemDTO> getAllIndicatorsQueueItemsByImpactRange(Integer page,
                                                                                            Integer limit,
                                                                                            IndicatorsImpactRange impactRange,
                                                                                            List<String> regions) {
        PageRequest pageRequest = PageRequest.of(page, limit);

        if (impactRange.getMax() != null && impactRange.getMin() != null) {
            Page<IndicatorsQueueItem> queuePage = regions == null ?
                    indicatorsQueueItemRepository.findAllByTenderScoreGreaterThanEqualAndTenderScoreLessThanOrderByMaterialityScoreDesc(
                            impactRange.getMin(),
                            impactRange.getMax(),
                            pageRequest
                    ) :
                    indicatorsQueueItemRepository.findAllByTenderScoreGreaterThanEqualAndTenderScoreLessThanAndRegionInOrderByMaterialityScoreDesc(
                            impactRange.getMin(),
                            impactRange.getMax(),
                            regions,
                            pageRequest
                    );

            Integer numberOfTopRiskedTenders = regions == null ?
                    indicatorsQueueItemRepository.countByTopRiskIsTrueAndTenderScoreGreaterThanEqualAndTenderScoreLessThan(
                            impactRange.getMin(),
                            impactRange.getMax()
                    ) :
                    indicatorsQueueItemRepository.countByTopRiskIsTrueAndTenderScoreGreaterThanEqualAndTenderScoreLessThanAndRegionIn(
                            impactRange.getMin(),
                            impactRange.getMax(),
                            regions
                    );

            return createIndicatorsQueuePage(queuePage, numberOfTopRiskedTenders);
        }

        if (impactRange.getMax() == null && impactRange.getMin() != null) {
            Page<IndicatorsQueueItem> queuePage = regions == null ?
                    indicatorsQueueItemRepository.findAllByTenderScoreGreaterThanEqualOrderByMaterialityScoreDesc(impactRange.getMin(), pageRequest)
                    : indicatorsQueueItemRepository.findAllByTenderScoreGreaterThanEqualAndRegionInOrderByMaterialityScoreDesc(impactRange.getMin(), regions, pageRequest);

            Integer numberOfTopRiskedTenders = regions == null ?
                    indicatorsQueueItemRepository.countByTopRiskIsTrueAndTenderScoreGreaterThanEqual(
                            impactRange.getMin()
                    ) :
                    indicatorsQueueItemRepository.countByTopRiskIsTrueAndTenderScoreGreaterThanEqualAndRegionIn(
                            impactRange.getMin(),
                            regions
                    );
            return createIndicatorsQueuePage(queuePage, numberOfTopRiskedTenders);
        }

        if (impactRange.getMin() == null && impactRange.getMax() != null) {
            Page<IndicatorsQueueItem> queuePage = regions == null ?
                    indicatorsQueueItemRepository.findAllByTenderScoreLessThanOrderByMaterialityScoreDesc(impactRange.getMax(), pageRequest)
                    : indicatorsQueueItemRepository.findAllByTenderScoreLessThanAndRegionInOrderByMaterialityScoreDesc(impactRange.getMax(), regions, pageRequest);

            Integer numberOfTopRiskedTenders = regions == null ?
                    indicatorsQueueItemRepository.countByTopRiskIsTrueAndTenderScoreLessThan(
                            impactRange.getMax()
                    ) :
                    indicatorsQueueItemRepository.countByTopRiskIsTrueAndTenderScoreLessThanAndRegionIn(
                            impactRange.getMax(),
                            regions
                    );

            return createIndicatorsQueuePage(queuePage, numberOfTopRiskedTenders);
        }

        throw new InvalidIndicatorsImpactRangeException(impactRange);
    }

    public List<IndicatorsQueueItem> saveAll(List<IndicatorsQueueItem> indicatorsQueueItems) {
        return indicatorsQueueItemRepository.saveAll(indicatorsQueueItems);
    }

    public void deleteAll() {
        indicatorsQueueItemRepository.deleteAllInBatch();
    }

    public List<String> getRegions(String impactCategory) {
        if (null != impactCategory) {
            switch (impactCategory) {
                case "low": {
                    return indicatorsQueueItemRepository.findDistinctRegionsByTenderScoreLessThan(0.5);
                }
                case "medium": {
                    return indicatorsQueueItemRepository.findDistinctRegionsByTenderScoreGreaterThanEqualAndTenderScoreLessThan(0.5, 1.2);
                }
                case "high": {
                    return indicatorsQueueItemRepository.findDistinctRegionsByTenderScoreGreaterThanEqual(1.2);
                }
            }
        }
        return indicatorsQueueItemRepository.findDistinctRegions();
    }

    private PageableResource<IndicatorsQueueItemDTO> createIndicatorsQueuePage(Page<IndicatorsQueueItem> page,
                                                                               Integer numberOfTopRiskedTenders) {
        List<IndicatorsQueueItemDTO> content = page.getContent().stream()
                .map(item -> (IndicatorsQueueItemDTO) GeneralBeanMapper.map(
                        item, IndicatorsQueueItemDTO.class))
                .peek(item -> item.setImpactCategory(resolveImpactCategory(item.getTenderScore())))
                .collect(Collectors.toList());

        PageableResource<IndicatorsQueueItemDTO> queuePage = new PageableResource<>();
        queuePage.setContent(content);
        queuePage.setTotalPages(page.getTotalPages());
        queuePage.setTotalElements(page.getTotalElements());
        queuePage.setNumberOfTopRiskedTenders(numberOfTopRiskedTenders);

        return queuePage;
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
