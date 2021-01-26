package com.datapath.indicatorsqueue.services;

import com.datapath.druidintegration.service.TenderRateService;
import com.datapath.indicatorsqueue.comparators.RegionIndicatorsMapByMaterialityScoreComparator;
import com.datapath.indicatorsqueue.mappers.TenderScoreMapper;
import com.datapath.persistence.entities.Contract;
import com.datapath.persistence.entities.MonitoringEntity;
import com.datapath.persistence.entities.Tender;
import com.datapath.persistence.entities.TenderContract;
import com.datapath.persistence.entities.queue.IndicatorsQueueHistory;
import com.datapath.persistence.entities.queue.RegionIndicatorsQueueItem;
import com.datapath.persistence.entities.queue.RegionIndicatorsQueueItemHistory;
import com.datapath.persistence.repositories.MonitoringRepository;
import com.datapath.persistence.repositories.TenderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Service
public class RegionIndicatorsQueueUpdaterService {

    private List<String> ignoredMethodTypes;
    private List<String> ignoredContractStatuses;

    private Long queueId;
    private ZonedDateTime dateCreated;
    private TenderRateService tenderRateService;
    private IndicatorsQueueHistoryService indicatorsQueueHistoryService;
    private RegionIndicatorsQueueItemService regionIndicatorsQueueItemService;
    private IndicatorsQueueRegionProvider indicatorsQueueRegionProvider;
    private IndicatorsQueueConfigurationProvider configProvider;
    private TenderRepository tenderRepository;
    private MonitoringRepository monitoringRepository;

    private List<String> unresolvedRegions;

    public RegionIndicatorsQueueUpdaterService(@Value("${queue.ignore.method-type}") String ignoreMethodTypes,
                                               @Value("${queue.ignore.contract-statuses}") String ignoreContractStatuses,
                                               IndicatorsQueueHistoryService indicatorsQueueHistoryService,
                                               TenderRateService tenderRateService,
                                               RegionIndicatorsQueueItemService regionIndicatorsQueueItemService,
                                               IndicatorsQueueRegionProvider indicatorsQueueRegionProvider,
                                               IndicatorsQueueConfigurationProvider configProvider,
                                               TenderRepository tenderRepository,
                                               MonitoringRepository monitoringRepository) {
        this.indicatorsQueueHistoryService = indicatorsQueueHistoryService;
        this.tenderRateService = tenderRateService;
        this.regionIndicatorsQueueItemService = regionIndicatorsQueueItemService;
        this.indicatorsQueueRegionProvider = indicatorsQueueRegionProvider;
        this.configProvider = configProvider;
        this.tenderRepository = tenderRepository;
        this.monitoringRepository = monitoringRepository;
        this.unresolvedRegions = new ArrayList<>();
        this.ignoredMethodTypes = Arrays.asList(ignoreMethodTypes.split(","));
        this.ignoredContractStatuses = Arrays.asList(ignoreContractStatuses.split(","));
    }

    public ZonedDateTime getDateCreated() {
        return this.dateCreated;
    }

    public Long getQueueId() {
        return this.queueId;
    }

    @Transactional
    public void updateIndicatorsQueue() {
        configProvider.init();
        log.info("Updating region queue items starts");

        List<RegionIndicatorsQueueItem> indicatorsQueue = getIndicatorsQueue();

        Map<String, String> tenderOuterIdMethodType = new HashMap<>();

        indicatorsQueue = indicatorsQueue.stream()
                .filter(item -> {
                    Tender tender = tenderRepository.findFirstByOuterId(item.getTenderOuterId());

                    if (isNull(tender)) return false;

                    try {
                        log.info("Processing tender {}", tender.getOuterId());

                        if (tender.getTvTenderCPV().startsWith("6611")) {
                            log.info("Tender with outer Id {} skipped due to finance category", item.getTenderId());
                            return false;
                        } else if (!hasNoTerminatedContract(tender)) {
                            return false;
                        }
                        tenderOuterIdMethodType.put(tender.getOuterId(), tender.getProcurementMethodType());
                        return true;
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        tenderOuterIdMethodType.put(tender.getOuterId(), tender.getProcurementMethodType());
                        return true;
                    }
                }).collect(toList());

        mapQueueItemsRegion(indicatorsQueue);

        Map<String, List<RegionIndicatorsQueueItem>> indicatorsByRegion = groupIndicatorsByRegion(indicatorsQueue);

        indicatorsByRegion.forEach((s, regionIndicatorsQueueItems) -> categorizeIndicatorsQueueItems(regionIndicatorsQueueItems, tenderOuterIdMethodType));

        List<RegionIndicatorsQueueItem> allIndicatorsQueue = new ArrayList<>();

        indicatorsByRegion.forEach((s, regionIndicatorsQueueItems) ->
                allIndicatorsQueue.addAll(regionIndicatorsQueueItems));

        List<RegionIndicatorsQueueItem> queueWithoutMonitoringTenders = disableTendersOnMonitoring(allIndicatorsQueue);

        updateLeftDateForPreviousQueueItems();
        clearQueue();

        List<RegionIndicatorsQueueItem> indicatorsQueueItems = saveQueue(queueWithoutMonitoringTenders);

        ZonedDateTime saveNewQueueDate = ZonedDateTime.now(ZoneId.of("UTC"));

        saveQueueHistory(indicatorsQueueItems, saveNewQueueDate);

        log.info("Updating region queue items finished. Saved {} items.", indicatorsQueueItems.size());

        IndicatorsQueueHistory history = new IndicatorsQueueHistory();
        history.setId(indicatorsQueueHistoryService.getMaxId() + 1);
        history.setDateCreated(saveNewQueueDate);

        IndicatorsQueueHistory savedHistory = indicatorsQueueHistoryService.save(history);
        queueId = savedHistory.getId();
        dateCreated = savedHistory.getDateCreated();

        log.info("Save indicator history {}", savedHistory);
    }

    private boolean hasNoTerminatedContract(Tender tender) {
        if (isEmpty(tender.getTenderContracts())) {
            return true;
        }

        List<Contract> contracts = tender.getTenderContracts()
                .stream()
                .filter(c -> nonNull(c.getContract()))
                .map(TenderContract::getContract)
                .collect(toList());

        if (isEmpty(contracts)) {
            return true;
        }

        return contracts.stream()
                .anyMatch(c -> !ignoredContractStatuses.contains(c.getStatus()));
    }

    private void updateLeftDateForPreviousQueueItems() {
        List<RegionIndicatorsQueueItem> previousQueue = getPreviousQueue();

        List<RegionIndicatorsQueueItemHistory> queueHistoryItemByTenderIds = regionIndicatorsQueueItemService.findQueueHistoryItemByTenderIds(
                previousQueue.stream()
                        .map(RegionIndicatorsQueueItem::getTenderId)
                        .collect(toList())
        );

        ZonedDateTime leftQueueDate = ZonedDateTime.now(ZoneId.of("UTC"));
        queueHistoryItemByTenderIds.forEach(i -> i.setLeftQueueDate(leftQueueDate));
        updateQueueHistory(queueHistoryItemByTenderIds);
    }

    private List<RegionIndicatorsQueueItem> getIndicatorsQueue() {
        return tenderRateService.getResult().stream()
                .map(TenderScoreMapper::mapToRegionIndicatorsQueueItem)
                .collect(toList());
    }

    private List<RegionIndicatorsQueueItem> getPreviousQueue() {
        return regionIndicatorsQueueItemService.getAll();
    }

    private List<RegionIndicatorsQueueItem> saveQueue(List<RegionIndicatorsQueueItem> indicatorsQueueItems) {
        log.info("Save {} region indicators queue items...", indicatorsQueueItems.size());
        return regionIndicatorsQueueItemService.saveAll(indicatorsQueueItems);
    }

    private void updateQueueHistory(List<RegionIndicatorsQueueItemHistory> queueHistoryItems) {
        log.info("Updating {} elements in queue history", queueHistoryItems.size());
        regionIndicatorsQueueItemService.updateRegionIndicatorQueueItemHistoryWithLeftQueueDate(queueHistoryItems);
    }

    private void saveQueueHistory(List<RegionIndicatorsQueueItem> indicatorsQueueItems, ZonedDateTime saveNewQueueDate) {
        log.info("Saving {} elements to queue history", indicatorsQueueItems.size());
        regionIndicatorsQueueItemService.saveRegionIndicatorQueueItemHistory(indicatorsQueueItems, saveNewQueueDate);
    }

    private void clearQueue() {
        log.info("Clear existing region indicators queue items...");
        regionIndicatorsQueueItemService.deleteAll();
    }

    private void mapQueueItemsRegion(List<RegionIndicatorsQueueItem> items) {
        items.forEach(item -> {
            String originalName = item.getRegion();
            String correctName = indicatorsQueueRegionProvider.getRegionCorrectName(originalName);
            if (correctName == null) {
                unresolvedRegions.add(originalName);
            }
            item.setRegion(correctName);
        });
    }

    private void categorizeIndicatorsQueueItems(List<RegionIndicatorsQueueItem> items, Map<String, String> tenderOuterIdMethodType) {
        Comparator<RegionIndicatorsQueueItem> scoreComparator = Comparator.comparing(
                RegionIndicatorsQueueItem::getMaterialityScore);

        List<RegionIndicatorsQueueItem> low = items.stream()
                .filter(item -> item.getTenderScore() >= configProvider.getLowIndicatorImpactRange().getMin())
                .filter(item -> item.getTenderScore() < configProvider.getLowIndicatorImpactRange().getMax())
                .sorted(scoreComparator)
                .collect(toList());

        List<RegionIndicatorsQueueItem> medium = items.stream()
                .filter(item -> item.getTenderScore() >= configProvider.getMediumIndicatorImpactRange().getMin()
                        && item.getTenderScore() < configProvider.getMediumIndicatorImpactRange().getMax())
                .sorted(scoreComparator)
                .collect(toList());

        List<RegionIndicatorsQueueItem> high = items.stream()
                .filter(item -> item.getTenderScore() >= configProvider.getHighIndicatorImpactRange().getMin())
                .sorted(scoreComparator)
                .collect(toList());

        Collections.reverse(low);
        Collections.reverse(medium);
        Collections.reverse(high);

        int lowSize = low.isEmpty() ? 1 : low.size();
        int maxLowIndex = ((Double) (configProvider.getLowTopRiskPercentage() * lowSize / 100)).intValue();

        int lowIndex = 0;
        for (RegionIndicatorsQueueItem item : low) {
            if (hasIgnoreMethodType(tenderOuterIdMethodType.get(item.getTenderOuterId()))) {
                item.setTopRisk(false);
                continue;
            }
            item.setTopRisk(lowIndex <= maxLowIndex);
            if (lowIndex <= maxLowIndex) {
                item.setRiskStage("Materiality score");
            }
            lowIndex++;
        }

        int mediumSize = medium.isEmpty() ? 1 : medium.size();
        int maxMediumIndex = ((Double) (configProvider.getMediumTopRiskPercentage() * mediumSize / 100)).intValue();

        int mediumIndex = 0;
        for (RegionIndicatorsQueueItem item : medium) {
            if (hasIgnoreMethodType(tenderOuterIdMethodType.get(item.getTenderOuterId()))) {
                item.setTopRisk(false);
                continue;
            }
            item.setTopRisk(mediumIndex <= maxMediumIndex);
            if (mediumIndex <= maxMediumIndex) {
                item.setRiskStage("Materiality score");
            }
            mediumIndex++;
        }

        int highSize = high.isEmpty() ? 1 : high.size();
        int maxHighIndex = ((Double) (configProvider.getHighTopRiskPercentage() * highSize / 100)).intValue();

        int highIndex = 0;
        for (RegionIndicatorsQueueItem item : high) {
            if (hasIgnoreMethodType(tenderOuterIdMethodType.get(item.getTenderOuterId()))) {
                item.setTopRisk(false);
                continue;
            }
            item.setTopRisk(highIndex <= maxHighIndex);
            if (highIndex <= maxHighIndex) {
                item.setRiskStage("Materiality score");
            }
            highIndex++;
        }

        enableTopRiskByProcuringEntity(low, configProvider.getLowTopRiskProcuringEntityPercentage(), tenderOuterIdMethodType);
        enableTopRiskByProcuringEntity(medium, configProvider.getMediumTopRiskProcuringEntityPercentage(), tenderOuterIdMethodType);
        enableTopRiskByProcuringEntity(high, configProvider.getHighTopRiskProcuringEntityPercentage(), tenderOuterIdMethodType);
    }

    private boolean hasIgnoreMethodType(String methodType) {
        return ignoredMethodTypes.contains(methodType);
    }

    private void enableTopRiskByProcuringEntity(List<RegionIndicatorsQueueItem> items, Double percents, Map<String, String> tenderOuterIdMethodType) {
        Map<String, List<RegionIndicatorsQueueItem>> itemsByProcuringEntity = new HashMap<>();
        items.stream().filter(item -> !item.getTopRisk()).forEach(item -> {
            List<RegionIndicatorsQueueItem> existingItems = itemsByProcuringEntity.get(item.getProcuringEntityId());
            if (existingItems != null) {
                existingItems.add(item);
            } else {
                existingItems = new ArrayList<>();
                existingItems.add(item);
                itemsByProcuringEntity.put(item.getProcuringEntityId(), existingItems);
            }
        });

        RegionIndicatorsMapByMaterialityScoreComparator comparator = new RegionIndicatorsMapByMaterialityScoreComparator(itemsByProcuringEntity);
        Map<String, List<RegionIndicatorsQueueItem>> sortedItemsByProcuringEntity = new TreeMap<>(comparator);
        sortedItemsByProcuringEntity.putAll(itemsByProcuringEntity);


        int size = sortedItemsByProcuringEntity.isEmpty() ? 1 : sortedItemsByProcuringEntity.size();
        int maxIndex = ((Double) (percents * size / 100)).intValue();

        int index = 0;
        for (Map.Entry<String, List<RegionIndicatorsQueueItem>> entry : sortedItemsByProcuringEntity.entrySet()) {
            long noIgnoredByMethodTypeTendersCount = entry.getValue()
                    .stream()
                    .filter(i -> !hasIgnoreMethodType(tenderOuterIdMethodType.get(i.getTenderOuterId())))
                    .count();

            if (noIgnoredByMethodTypeTendersCount == 0) continue;

            if (index <= maxIndex) {
                entry.getValue()
                        .stream()
                        .filter(i -> !hasIgnoreMethodType(tenderOuterIdMethodType.get(i.getTenderOuterId())))
                        .forEach(i -> {
                            i.setTopRisk(true);
                            i.setRiskStage("Procuring entity");
                        });
            }
            index++;
        }
    }

    private List<RegionIndicatorsQueueItem> disableTendersOnMonitoring(List<RegionIndicatorsQueueItem> items) {
        try {

            List<String> activeMonitorings = monitoringRepository.findAllByActiveStatus().stream()
                    .map(MonitoringEntity::getTenderId)
                    .collect(toList());
            List<RegionIndicatorsQueueItem> resultQueue = new ArrayList<>();
            items.forEach(item -> {
                if (activeMonitorings.contains(item.getTenderOuterId())) {
                    item.setTopRisk(false);
                    item.setMonitoring(true);
                    item.setRiskStage(null);
                    log.info("Tender {} in monitoring now.", item.getTenderOuterId());
                } else {
                    item.setMonitoring(false);
                    resultQueue.add(item);
                }
            });
            return resultQueue;
        } catch (Exception e) {
            log.error("Monitorings loading failed.", e);
        }
        return Collections.emptyList();
    }

    private Map<String, List<RegionIndicatorsQueueItem>> groupIndicatorsByRegion(List<RegionIndicatorsQueueItem> items) {
        List<String> regions = items.stream()
                .map(RegionIndicatorsQueueItem::getRegion)
                .collect(toList());

        Map<String, List<RegionIndicatorsQueueItem>> regionsMap = new HashMap<>();

        regions.forEach(region -> regionsMap.put(region, new ArrayList<>()));

        items.forEach(item -> regionsMap.get(item.getRegion()).add(item));

        return regionsMap;
    }
}
