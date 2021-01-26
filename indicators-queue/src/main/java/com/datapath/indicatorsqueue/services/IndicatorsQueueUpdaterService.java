package com.datapath.indicatorsqueue.services;

import com.datapath.druidintegration.service.TenderRateService;
import com.datapath.indicatorsqueue.comparators.IndicatorsMapByMaterialityScoreComparator;
import com.datapath.indicatorsqueue.mappers.TenderScoreMapper;
import com.datapath.persistence.entities.MonitoringEntity;
import com.datapath.persistence.entities.queue.IndicatorsQueueHistory;
import com.datapath.persistence.entities.queue.IndicatorsQueueItem;
import com.datapath.persistence.repositories.MonitoringRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class IndicatorsQueueUpdaterService {

    private Long queueId;
    private ZonedDateTime dateCreated;
    private TenderRateService tenderRateService;
    private IndicatorsQueueHistoryService indicatorsQueueHistoryService;
    private IndicatorsQueueItemService indicatorsQueueItemService;
    private IndicatorsQueueConfigurationProvider configProvider;
    private MonitoringRepository monitoringRepository;

    public IndicatorsQueueUpdaterService(IndicatorsQueueHistoryService indicatorsQueueHistoryService,
                                         TenderRateService tenderRateService,
                                         IndicatorsQueueItemService indicatorsQueueItemService,
                                         IndicatorsQueueConfigurationProvider configProvider,
                                         MonitoringRepository monitoringRepository) {
        this.indicatorsQueueHistoryService = indicatorsQueueHistoryService;
        this.tenderRateService = tenderRateService;
        this.indicatorsQueueItemService = indicatorsQueueItemService;
        this.configProvider = configProvider;
        this.monitoringRepository = monitoringRepository;
    }

    public ZonedDateTime getDateCreated() {
        return this.dateCreated;
    }

    public Long getQueueId() {
        return this.queueId;
    }

    public void updateIndicatorsQueue() {
        configProvider.init();
        log.info("Updating queue items starts");

        clearQueue();
        List<IndicatorsQueueItem> indicatorsQueue = categorizeIndicatorsQueueItems(getIndicatorsQueue());

        disableTendersOnMonitoring(indicatorsQueue);

        List<IndicatorsQueueItem> indicatorsQueueItems = saveQueue(indicatorsQueue);

        log.info("Updating queue items finished. Saved {} items.", indicatorsQueueItems.size());

        IndicatorsQueueHistory history = new IndicatorsQueueHistory();
        history.setId(indicatorsQueueHistoryService.getMaxId() + 1);
        history.setDateCreated(ZonedDateTime.now(ZoneId.of("UTC")));

        IndicatorsQueueHistory savedHistory = indicatorsQueueHistoryService.save(history);
        queueId = savedHistory.getId();
        dateCreated = savedHistory.getDateCreated();

        log.info("Save indicator history {}", savedHistory);
    }

    private List<IndicatorsQueueItem> getIndicatorsQueue() {
        return tenderRateService.getResult().stream()
                .map(TenderScoreMapper::mapToIndicatorsQueueItem)
                .collect(Collectors.toList());
    }

    private List<IndicatorsQueueItem> saveQueue(List<IndicatorsQueueItem> indicatorsQueueItems) {
        log.info("Save {} indicators queue items...", indicatorsQueueItems.size());
        return indicatorsQueueItemService.saveAll(indicatorsQueueItems);
    }

    private void clearQueue() {
        log.info("Clear existing indicators queue items...");
        indicatorsQueueItemService.deleteAll();
    }

    private List<IndicatorsQueueItem> categorizeIndicatorsQueueItems(List<IndicatorsQueueItem> items) {
        Comparator<IndicatorsQueueItem> scoreComparator = Comparator.comparing(
                IndicatorsQueueItem::getMaterialityScore);

        List<IndicatorsQueueItem> low = items.stream()
                .filter(item -> item.getTenderScore() >= configProvider.getLowIndicatorImpactRange().getMin())
                .filter(item -> item.getTenderScore() < configProvider.getLowIndicatorImpactRange().getMax())
                .sorted(scoreComparator)
                .collect(Collectors.toList());

        List<IndicatorsQueueItem> medium = items.stream()
                .filter(item -> item.getTenderScore() >= configProvider.getMediumIndicatorImpactRange().getMin()
                        && item.getTenderScore() < configProvider.getMediumIndicatorImpactRange().getMax())
                .sorted(scoreComparator)
                .collect(Collectors.toList());

        List<IndicatorsQueueItem> high = items.stream()
                .filter(item -> item.getTenderScore() >= configProvider.getHighIndicatorImpactRange().getMin())
                .sorted(scoreComparator)
                .collect(Collectors.toList());

        Collections.reverse(low);
        Collections.reverse(medium);
        Collections.reverse(high);

        int lowSize = low.isEmpty() ? 1 : low.size();
        int maxLowIndex = ((Double) (configProvider.getLowTopRiskPercentage() * lowSize / 100)).intValue();

        int lowIndex = 0;
        for (IndicatorsQueueItem item : low) {
            item.setTopRisk(lowIndex <= maxLowIndex);
            if (lowIndex <= maxLowIndex) {
                item.setRiskStage("Materiality score");
            }
            lowIndex++;
        }

        int mediumSize = medium.isEmpty() ? 1 : medium.size();
        int maxMediumIndex = ((Double) (configProvider.getMediumTopRiskPercentage() * mediumSize / 100)).intValue();

        int mediumIndex = 0;
        for (IndicatorsQueueItem item : medium) {
            item.setTopRisk(mediumIndex <= maxMediumIndex);
            if (mediumIndex <= maxMediumIndex) {
                item.setRiskStage("Materiality score");
            }
            mediumIndex++;
        }

        int highSize = high.isEmpty() ? 1 : high.size();
        int maxHighIndex = ((Double) (configProvider.getHighTopRiskPercentage() * highSize / 100)).intValue();

        int highIndex = 0;
        for (IndicatorsQueueItem item : high) {
            item.setTopRisk(highIndex <= maxHighIndex);
            if (highIndex <= maxHighIndex) {
                item.setRiskStage("Materiality score");
            }
            highIndex++;
        }

        enableTopRiskByProcuringEntity(low, configProvider.getLowTopRiskProcuringEntityPercentage());
        enableTopRiskByProcuringEntity(medium, configProvider.getMediumTopRiskProcuringEntityPercentage());
        enableTopRiskByProcuringEntity(high, configProvider.getHighTopRiskProcuringEntityPercentage());

        List<IndicatorsQueueItem> allItems = new ArrayList<>();

        allItems.addAll(low);
        allItems.addAll(medium);
        allItems.addAll(high);

        return allItems;
    }

    private void enableTopRiskByProcuringEntity(List<IndicatorsQueueItem> items, Double percents) {
        Map<String, List<IndicatorsQueueItem>> itemsByProcuringEntity = new HashMap<>();
        items.stream().filter(item -> !item.getTopRisk()).forEach(item -> {
            List<IndicatorsQueueItem> existingItems = itemsByProcuringEntity.get(item.getProcuringEntityId());
            if (existingItems != null) {
                existingItems.add(item);
            } else {
                existingItems = new ArrayList<>();
                existingItems.add(item);
                itemsByProcuringEntity.put(item.getProcuringEntityId(), existingItems);
            }
        });

        IndicatorsMapByMaterialityScoreComparator comparator = new IndicatorsMapByMaterialityScoreComparator(itemsByProcuringEntity);
        Map<String, List<IndicatorsQueueItem>> sortedItemsByProcuringEntity = new TreeMap<>(comparator);
        sortedItemsByProcuringEntity.putAll(itemsByProcuringEntity);


        int size = sortedItemsByProcuringEntity.isEmpty() ? 1 : sortedItemsByProcuringEntity.size();
        int maxIndex = ((Double) (percents * size / 100)).intValue();

        int index = 0;
        for (Map.Entry<String, List<IndicatorsQueueItem>> entry : sortedItemsByProcuringEntity.entrySet()) {
            if (index <= maxIndex) {
                entry.getValue().forEach(item -> {
                    item.setTopRisk(true);
                    item.setRiskStage("Procuring entity");
                });
            }
            index++;
        }
    }

    private void disableTendersOnMonitoring(List<IndicatorsQueueItem> items) {
        try {
            List<String> activeMonitorings = monitoringRepository.findAllByActiveStatus().stream()
                    .map(MonitoringEntity::getTenderId)
                    .collect(Collectors.toList());
            items.forEach(item -> {
                if (activeMonitorings.contains(item.getTenderOuterId())) {
                    item.setTopRisk(false);
                    item.setMonitoring(true);
                    item.setRiskStage(null);
                    log.info("Tender {} in monitoring now.", item.getTenderOuterId());
                } else {
                    item.setMonitoring(false);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Monitorings loading failed.");
        }
    }
}
