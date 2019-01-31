package com.datapath.web.services;

import com.datapath.indicatorsqueue.domain.IndicatorsImpactRange;
import com.datapath.indicatorsqueue.domain.IndicatorsQueueItemDTO;
import com.datapath.indicatorsqueue.domain.PageableResource;
import com.datapath.indicatorsqueue.services.IndicatorsQueueConfigurationProvider;
import com.datapath.indicatorsqueue.services.IndicatorsQueueItemService;
import com.datapath.indicatorsqueue.services.IndicatorsQueueUpdaterService;
import com.datapath.web.domain.common.ImpactCategory;
import com.datapath.web.domain.queue.IndicatorsQueueDataPage;
import com.datapath.web.domain.queue.IndicatorsQueueInfo;
import com.datapath.web.domain.queue.IndicatorsQueueItem;
import com.datapath.web.domain.queue.IndicatorsQueuePagination;
import com.datapath.web.mappers.GeneralBeanMapper;
import com.datapath.web.util.PaginationUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class IndicatorsQueueService {

    private IndicatorsQueueItemService indicatorsQueueItemService;
    private IndicatorsQueueUpdaterService indicatorsQueueUpdaterService;
    private IndicatorsQueueConfigurationProvider queueConfigProvider;

    public IndicatorsQueueService(IndicatorsQueueItemService indicatorsQueueItemService,
                                  IndicatorsQueueUpdaterService indicatorsQueueUpdaterService,
                                  IndicatorsQueueConfigurationProvider queueConfigProvider) {

        this.indicatorsQueueItemService = indicatorsQueueItemService;
        this.indicatorsQueueUpdaterService = indicatorsQueueUpdaterService;
        this.queueConfigProvider = queueConfigProvider;
    }

    public IndicatorsQueueDataPage getIndicatorsQueue(String path,
                                                      Integer page,
                                                      Integer limit,
                                                      List<String> regions) {

        PageableResource<IndicatorsQueueItemDTO> indicatorsQueuePage = indicatorsQueueItemService
                .getAllIndicatorsQueueItems(page, limit, regions);

        List<IndicatorsQueueItem> indicatorsQueueItems = indicatorsQueuePage.getContent()
                .stream()
                .map(item -> (IndicatorsQueueItem) GeneralBeanMapper.map(
                        item, IndicatorsQueueItem.class))
                .collect(Collectors.toList());

        int totalPages = indicatorsQueuePage.getTotalPages();
        long totalElements = indicatorsQueuePage.getTotalElements();

        IndicatorsQueuePagination pagination = PaginationUtils.createIndicatorsQueuePagination(
                page, limit, path, totalPages, totalElements, regions
        );

        IndicatorsQueueInfo info = new IndicatorsQueueInfo();
        info.setDateCreated(indicatorsQueueUpdaterService.getDateCreated());
        info.setQueueId(indicatorsQueueUpdaterService.getQueueId());
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
                                                         List<String> regions) {

        IndicatorsImpactRange impactRange = queueConfigProvider.getLowIndicatorImpactRange();

        PageableResource<IndicatorsQueueItemDTO> indicatorsQueuePage = indicatorsQueueItemService
                .getAllIndicatorsQueueItemsByImpactRange(page, limit, impactRange, regions);

        List<IndicatorsQueueItem> indicatorsQueueItems = indicatorsQueuePage.getContent()
                .stream()
                .map(item -> (IndicatorsQueueItem) GeneralBeanMapper.map(
                        item, IndicatorsQueueItem.class))
                .collect(Collectors.toList());

        int totalPages = indicatorsQueuePage.getTotalPages();
        long totalElements = indicatorsQueuePage.getTotalElements();

        IndicatorsQueuePagination pagination = PaginationUtils.createIndicatorsQueuePagination(
                page, limit, path, totalPages, totalElements, regions
        );

        IndicatorsQueueInfo info = new IndicatorsQueueInfo();
        info.setDateCreated(indicatorsQueueUpdaterService.getDateCreated());
        info.setQueueId(indicatorsQueueUpdaterService.getQueueId());
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
                                                            List<String> regions) {

        IndicatorsImpactRange impactRange = queueConfigProvider.getMediumIndicatorImpactRange();

        PageableResource<IndicatorsQueueItemDTO> indicatorsQueuePage = indicatorsQueueItemService
                .getAllIndicatorsQueueItemsByImpactRange(page, limit, impactRange, regions);

        List<IndicatorsQueueItem> indicatorsQueueItems = indicatorsQueuePage.getContent()
                .stream()
                .map(item -> (IndicatorsQueueItem) GeneralBeanMapper.map(
                        item, IndicatorsQueueItem.class))
                .collect(Collectors.toList());

        int totalPages = indicatorsQueuePage.getTotalPages();
        long totalElements = indicatorsQueuePage.getTotalElements();

        IndicatorsQueuePagination pagination = PaginationUtils.createIndicatorsQueuePagination(
                page, limit, path, totalPages, totalElements, regions
        );

        IndicatorsQueueInfo info = new IndicatorsQueueInfo();
        info.setDateCreated(indicatorsQueueUpdaterService.getDateCreated());
        info.setQueueId(indicatorsQueueUpdaterService.getQueueId());
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
                                                          List<String> regions) {

        IndicatorsImpactRange impactRange = queueConfigProvider.getHighIndicatorImpactRange();

        PageableResource<IndicatorsQueueItemDTO> indicatorsQueuePage = indicatorsQueueItemService
                .getAllIndicatorsQueueItemsByImpactRange(page, limit, impactRange, regions);

        List<IndicatorsQueueItem> indicatorsQueueItems = indicatorsQueuePage.getContent()
                .stream()
                .map(item -> (IndicatorsQueueItem) GeneralBeanMapper.map(
                        item, IndicatorsQueueItem.class))
                .collect(Collectors.toList());

        int totalPages = indicatorsQueuePage.getTotalPages();
        long totalElements = indicatorsQueuePage.getTotalElements();

        IndicatorsQueuePagination pagination = PaginationUtils.createIndicatorsQueuePagination(
                page, limit, path, totalPages, totalElements, regions
        );

        IndicatorsQueueInfo info = new IndicatorsQueueInfo();
        info.setDateCreated(indicatorsQueueUpdaterService.getDateCreated());
        info.setQueueId(indicatorsQueueUpdaterService.getQueueId());
        info.setImpactCategory(ImpactCategory.HIGH.categoryName());

        info.setExpectedValueImportanceCoefficient(0.5d);
        info.setTenderScoreImportanceCoefficient(0.5d);
        info.setNumberOfTopRiskedTenders(indicatorsQueuePage.getNumberOfTopRiskedTenders());
        info.setTopRiskPercentage(queueConfigProvider.getHighTopRiskPercentage());
        info.setTenderScoreRange(impactRange);
        info.setTopRiskProcuringEntityPercentage(queueConfigProvider.getHighTopRiskProcuringEntityPercentage());

        IndicatorsQueueDataPage<IndicatorsQueueItem> queueDataPage = new IndicatorsQueueDataPage<>();
        queueDataPage.setQueueInfo(info);
        queueDataPage.setData(indicatorsQueueItems);

        queueDataPage.setPagination(pagination);

        return queueDataPage;
    }

    public List<String> getRegions(String impactCategory) {
        return indicatorsQueueItemService.getRegions(impactCategory);
    }
}
