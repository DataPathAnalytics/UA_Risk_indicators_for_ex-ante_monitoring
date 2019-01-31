package com.datapath.web.services;

import com.datapath.indicatorsqueue.domain.IndicatorsImpactRange;
import com.datapath.indicatorsqueue.domain.PageableResource;
import com.datapath.indicatorsqueue.domain.RegionIndicatorsQueueItemDTO;
import com.datapath.indicatorsqueue.services.IndicatorsQueueConfigurationProvider;
import com.datapath.indicatorsqueue.services.RegionIndicatorsQueueItemService;
import com.datapath.indicatorsqueue.services.RegionIndicatorsQueueUpdaterService;
import com.datapath.web.domain.common.ImpactCategory;
import com.datapath.web.domain.queue.IndicatorsQueueDataPage;
import com.datapath.web.domain.queue.IndicatorsQueueInfo;
import com.datapath.web.domain.queue.IndicatorsQueueItem;
import com.datapath.web.domain.queue.IndicatorsQueuePagination;
import com.datapath.web.mappers.GeneralBeanMapper;
import com.datapath.web.util.PaginationUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RegionIndicatorsQueueService {

    private RegionIndicatorsQueueItemService regionIndicatorsQueueItemService;
    private RegionIndicatorsQueueUpdaterService regionIndicatorsQueueUpdaterService;
    private IndicatorsQueueConfigurationProvider queueConfigProvider;

    public RegionIndicatorsQueueService(RegionIndicatorsQueueItemService regionIndicatorsQueueItemService,
                                        RegionIndicatorsQueueUpdaterService regionIndicatorsQueueUpdaterService,
                                        IndicatorsQueueConfigurationProvider queueConfigProvider) {

        this.regionIndicatorsQueueItemService = regionIndicatorsQueueItemService;
        this.regionIndicatorsQueueUpdaterService = regionIndicatorsQueueUpdaterService;
        this.queueConfigProvider = queueConfigProvider;
    }

    public IndicatorsQueueDataPage getIndicatorsQueue(String path,
                                                      Integer page,
                                                      Integer limit,
                                                      String region) {

        PageableResource<RegionIndicatorsQueueItemDTO> indicatorsQueuePage = regionIndicatorsQueueItemService
                .getAllIndicatorsQueueItems(page, limit, region);

        List<IndicatorsQueueItem> indicatorsQueueItems = indicatorsQueuePage.getContent()
                .stream()
                .map(item -> (IndicatorsQueueItem) GeneralBeanMapper.map(
                        item, IndicatorsQueueItem.class))
                .collect(Collectors.toList());

        int totalPages = indicatorsQueuePage.getTotalPages();
        long totalElements = indicatorsQueuePage.getTotalElements();

        List<String> regions = new ArrayList<>();
        if (region != null && !region.isEmpty()) {
            regions.add(region);
        }

        IndicatorsQueuePagination pagination = PaginationUtils.createIndicatorsQueuePagination(
                page, limit, path, totalPages, totalElements, regions
        );

        IndicatorsQueueInfo info = new IndicatorsQueueInfo();
        info.setDateCreated(regionIndicatorsQueueUpdaterService.getDateCreated());
        info.setQueueId(regionIndicatorsQueueUpdaterService.getQueueId());
        info.setImpactCategory(ImpactCategory.MIXED.categoryName());
        info.setExpectedValueImportanceCoefficient(0.5d);
        info.setTenderScoreImportanceCoefficient(0.5d);
        info.setNumberOfTopRiskedTenders(indicatorsQueuePage.getNumberOfTopRiskedTenders());
        info.setTopRiskPercentage(queueConfigProvider.getMixedTopRiskPercentage());
        info.setTenderScoreRange(queueConfigProvider.getMixedIndicatorImpactRange());
        info.setTopRiskProcuringEntityPercentage(null);

        IndicatorsQueueDataPage<IndicatorsQueueItem> queueDataPage = new IndicatorsQueueDataPage<>();
        queueDataPage.setData(indicatorsQueueItems);
        queueDataPage.setQueueInfo(info);

        queueDataPage.setPagination(pagination);

        return queueDataPage;
    }

    public IndicatorsQueueDataPage getLowIndicatorsQueue(String path,
                                                         Integer page,
                                                         Integer limit,
                                                         String region) {

        IndicatorsImpactRange impactRange = queueConfigProvider.getLowIndicatorImpactRange();

        PageableResource<RegionIndicatorsQueueItemDTO> indicatorsQueuePage = regionIndicatorsQueueItemService
                .getAllIndicatorsQueueItemsByImpactRange(page, limit, impactRange, region);

        List<IndicatorsQueueItem> indicatorsQueueItems = indicatorsQueuePage.getContent()
                .stream()
                .map(item -> (IndicatorsQueueItem) GeneralBeanMapper.map(
                        item, IndicatorsQueueItem.class))
                .collect(Collectors.toList());

        int totalPages = indicatorsQueuePage.getTotalPages();
        long totalElements = indicatorsQueuePage.getTotalElements();

        List<String> regions = new ArrayList<>();
        if (region != null && !region.isEmpty()) {
            regions.add(region);
        }

        IndicatorsQueuePagination pagination = PaginationUtils.createIndicatorsQueuePagination(
                page, limit, path, totalPages, totalElements, regions
        );

        IndicatorsQueueInfo info = new IndicatorsQueueInfo();
        info.setDateCreated(regionIndicatorsQueueUpdaterService.getDateCreated());
        info.setQueueId(regionIndicatorsQueueUpdaterService.getQueueId());
        info.setImpactCategory(ImpactCategory.LOW.categoryName());
        info.setExpectedValueImportanceCoefficient(0.5d);
        info.setTenderScoreImportanceCoefficient(0.5d);
        info.setNumberOfTopRiskedTenders(indicatorsQueuePage.getNumberOfTopRiskedTenders());
        info.setTopRiskPercentage(queueConfigProvider.getLowTopRiskPercentage());
        info.setTenderScoreRange(impactRange);
        info.setTopRiskProcuringEntityPercentage(queueConfigProvider.getLowTopRiskProcuringEntityPercentage());

        IndicatorsQueueDataPage<IndicatorsQueueItem> queueDataPage = new IndicatorsQueueDataPage<>();
        queueDataPage.setQueueInfo(info);
        queueDataPage.setData(indicatorsQueueItems);

        queueDataPage.setPagination(pagination);

        return queueDataPage;
    }

    public IndicatorsQueueDataPage getMediumIndicatorsQueue(String path,
                                                            Integer page,
                                                            Integer limit,
                                                            String region) {

        IndicatorsImpactRange impactRange = queueConfigProvider.getMediumIndicatorImpactRange();

        PageableResource<RegionIndicatorsQueueItemDTO> indicatorsQueuePage = regionIndicatorsQueueItemService
                .getAllIndicatorsQueueItemsByImpactRange(page, limit, impactRange, region);

        List<IndicatorsQueueItem> indicatorsQueueItems = indicatorsQueuePage.getContent()
                .stream()
                .map(item -> (IndicatorsQueueItem) GeneralBeanMapper.map(
                        item, IndicatorsQueueItem.class))
                .collect(Collectors.toList());

        int totalPages = indicatorsQueuePage.getTotalPages();
        long totalElements = indicatorsQueuePage.getTotalElements();

        List<String> regions = new ArrayList<>();
        if (region != null && !region.isEmpty()) {
            regions.add(region);
        }

        IndicatorsQueuePagination pagination = PaginationUtils.createIndicatorsQueuePagination(
                page, limit, path, totalPages, totalElements, regions
        );

        IndicatorsQueueInfo info = new IndicatorsQueueInfo();
        info.setDateCreated(regionIndicatorsQueueUpdaterService.getDateCreated());
        info.setQueueId(regionIndicatorsQueueUpdaterService.getQueueId());
        info.setImpactCategory(ImpactCategory.MEDIUM.categoryName());

        info.setExpectedValueImportanceCoefficient(0.5d);
        info.setTenderScoreImportanceCoefficient(0.5d);
        info.setNumberOfTopRiskedTenders(indicatorsQueuePage.getNumberOfTopRiskedTenders());
        info.setTopRiskPercentage(queueConfigProvider.getMediumTopRiskPercentage());
        info.setTenderScoreRange(impactRange);
        info.setTopRiskProcuringEntityPercentage(queueConfigProvider.getMediumTopRiskProcuringEntityPercentage());

        IndicatorsQueueDataPage<IndicatorsQueueItem> queueDataPage = new IndicatorsQueueDataPage<>();
        queueDataPage.setQueueInfo(info);
        queueDataPage.setData(indicatorsQueueItems);

        queueDataPage.setPagination(pagination);

        return queueDataPage;
    }

    public IndicatorsQueueDataPage getHighIndicatorsQueue(String path,
                                                          Integer page,
                                                          Integer limit,
                                                          String region) {

        IndicatorsImpactRange impactRange = queueConfigProvider.getHighIndicatorImpactRange();

        PageableResource<RegionIndicatorsQueueItemDTO> indicatorsQueuePage = regionIndicatorsQueueItemService
                .getAllIndicatorsQueueItemsByImpactRange(page, limit, impactRange, region);

        List<IndicatorsQueueItem> indicatorsQueueItems = indicatorsQueuePage.getContent()
                .stream()
                .map(item -> (IndicatorsQueueItem) GeneralBeanMapper.map(
                        item, IndicatorsQueueItem.class))
                .collect(Collectors.toList());

        int totalPages = indicatorsQueuePage.getTotalPages();
        long totalElements = indicatorsQueuePage.getTotalElements();

        List<String> regions = new ArrayList<>();
        if (region != null && !region.isEmpty()) {
            regions.add(region);
        }

        IndicatorsQueuePagination pagination = PaginationUtils.createIndicatorsQueuePagination(
                page, limit, path, totalPages, totalElements, regions
        );

        IndicatorsQueueInfo info = new IndicatorsQueueInfo();
        info.setDateCreated(regionIndicatorsQueueUpdaterService.getDateCreated());
        info.setQueueId(regionIndicatorsQueueUpdaterService.getQueueId());
        info.setImpactCategory(ImpactCategory.HIGH.categoryName());

        info.setExpectedValueImportanceCoefficient(0.5d);
        info.setTenderScoreImportanceCoefficient(0.5d);
        info.setNumberOfTopRiskedTenders(indicatorsQueuePage.getNumberOfTopRiskedTenders());
        info.setTopRiskPercentage(30d);
        info.setTenderScoreRange(impactRange);
        info.setTopRiskProcuringEntityPercentage(queueConfigProvider.getHighTopRiskProcuringEntityPercentage());

        IndicatorsQueueDataPage<IndicatorsQueueItem> queueDataPage = new IndicatorsQueueDataPage<>();
        queueDataPage.setQueueInfo(info);
        queueDataPage.setData(indicatorsQueueItems);

        queueDataPage.setPagination(pagination);

        return queueDataPage;
    }

    public Set<String> getRegions() {
        return regionIndicatorsQueueItemService.getRegions();
    }
}
