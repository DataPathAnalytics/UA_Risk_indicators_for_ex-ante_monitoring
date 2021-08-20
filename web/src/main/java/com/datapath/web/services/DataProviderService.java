package com.datapath.web.services;

import com.datapath.druidintegration.model.druid.response.common.Event;
import com.datapath.druidintegration.service.ExtractTenderDataService;
import com.datapath.persistence.domain.ConfigurationDomain;
import com.datapath.persistence.entities.Couse;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.entities.MonitoringEntity;
import com.datapath.persistence.entities.queue.RegionIndicatorsQueueItem;
import com.datapath.persistence.entities.queue.RegionIndicatorsQueueItemHistory;
import com.datapath.persistence.repositories.IndicatorRepository;
import com.datapath.persistence.repositories.MonitoringRepository;
import com.datapath.persistence.repositories.TenderRepository;
import com.datapath.persistence.repositories.queue.RegionIndicatorsQueueItemHistoryRepository;
import com.datapath.persistence.repositories.queue.RegionIndicatorsQueueItemRepository;
import com.datapath.persistence.service.ConfigurationDaoService;
import com.datapath.persistence.utils.DateUtils;
import com.datapath.web.dto.TenderInfo;
import com.datapath.web.dto.TendersInfoResponse;
import com.datapath.web.util.TenderScoreRank;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Service
@Slf4j
@AllArgsConstructor
public class DataProviderService {

    private final TenderRepository tenderRepository;
    private final IndicatorRepository indicatorRepository;
    private final MonitoringRepository monitoringRepository;
    private final RegionIndicatorsQueueItemRepository regionIndicatorsQueueItemRepository;
    private final RegionIndicatorsQueueItemHistoryRepository regionIndicatorsQueueItemHistoryRepository;
    private final ExtractTenderDataService extractTenderDataService;
    private final ConfigurationDaoService configurationService;

    public TendersInfoResponse getTendersInfo(ZonedDateTime since, int size) {

        Map<String, Boolean> topTendersMap = getTopTendersMap();
        Map<String, MonitoringEntity> monitoringsMap = getMonitoringsMap();
        TendersInfoResponse response = new TendersInfoResponse();
        response.setTenders(provide(since, size, monitoringsMap, topTendersMap));
        return response;
    }

    public TendersInfoResponse getTendersMonitoringInfo(ZonedDateTime since, int size) {

        Map<String, Boolean> topTendersMap = getTopTendersMap();
        Map<String, MonitoringEntity> monitoringsMap = getMonitoringsMap();
        TendersInfoResponse response = new TendersInfoResponse();
        response.setTenders(provideBasedOnMonitoring(since, size, monitoringsMap, topTendersMap));
        return response;
    }

    public TendersInfoResponse getTendersQueueInfo(ZonedDateTime since, int size) {
        Map<String, Boolean> topTendersMap = getTopTendersMap();
        Map<String, MonitoringEntity> monitoringMap = getMonitoringsMap();
        TendersInfoResponse response = new TendersInfoResponse();
        response.setTenders(provideBasedOnQueue(since, size, monitoringMap, topTendersMap));
        return response;
    }

    private Map<String, MonitoringEntity> getMonitoringsMap() {
        log.info("Start getting monitoring map");
        Map<String, MonitoringEntity> tendersMonitoringMap = new HashMap<>();
        monitoringRepository
                .findAll()
                .forEach(monitoring -> tendersMonitoringMap.put(monitoring.getTenderId(), monitoring));
        log.info("Finish getting monitoring map");
        return tendersMonitoringMap;
    }

    private Map<String, Boolean> getTopTendersMap() {
        log.info("Start getting top tenders map");
        return regionIndicatorsQueueItemRepository.getAllTendersIdsAndTops()
                .stream()
                .map(item -> new HashMap.SimpleEntry<>(item[0].toString(), Boolean.parseBoolean(item[1].toString())))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    private List<TenderInfo> provide(ZonedDateTime since, int size, Map<String, MonitoringEntity> tendersMonitoringMap, Map<String, Boolean> tendersTopMap) {

        Pageable pageRequest = PageRequest.of(0, size);

        log.info("Getting tenders batch");
        List<String> tenderIds = tenderRepository.findAllAfterDateModified(since, pageRequest).getContent();
        log.info("Finished getting tenders batch");

        log.info("map tender indicator info start");
        Map<String, TenderInfo> tenderIndicatorsCommonInfoMap =
                tenderIds.stream().collect(Collectors.toMap(
                        tenderId -> tenderId,
                        tenderId -> TenderInfo.builder()
                                .tenderRiskScore(0d)
                                .tenderOuterId(tenderId)
                                .indicators(new HashSet<>())
                                .indicatorsWithRisk(new HashSet<>())
                                .indicatorsWithOutRisk(new HashSet<>())
                                .inQueue(tendersTopMap.containsKey(tenderId))
                                .hasPriorityStatus(tendersTopMap.getOrDefault(tenderId, false))
                                .build()));

        fillMonitoringInfo(tenderIndicatorsCommonInfoMap, tendersMonitoringMap);
        fillIndicatorsInfo(tenderIndicatorsCommonInfoMap);
        fillTenderInfo(tenderIndicatorsCommonInfoMap);

        ArrayList<TenderInfo> tenderInfos = new ArrayList<>(tenderIndicatorsCommonInfoMap.values());
        calcScoreRank(tenderInfos);
        return tenderInfos;
    }

    private List<TenderInfo> provideBasedOnMonitoring(ZonedDateTime since, int size, Map<String, MonitoringEntity> tendersMonitoringMap, Map<String, Boolean> tendersTopMap) {

        log.info("map tender indicator info start");

        Map<String, TenderInfo> tenderIndicatorsCommonInfoMap = tendersMonitoringMap.values()
                .stream()
                .filter(m -> ZonedDateTime.parse(m.getModifiedDate()).isAfter(since))
                .sorted(Comparator.comparing(MonitoringEntity::getModifiedDate))
                .limit(size)
                .map(MonitoringEntity::getTenderId)
                .collect(Collectors.toMap(
                        tenderId -> tenderId,
                        tenderId -> TenderInfo.builder()
                                .tenderRiskScore(0d)
                                .tenderOuterId(tenderId)
                                .indicators(new HashSet<>())
                                .indicatorsWithRisk(new HashSet<>())
                                .indicatorsWithOutRisk(new HashSet<>())
                                .inQueue(tendersTopMap.containsKey(tenderId))
                                .hasPriorityStatus(tendersTopMap.getOrDefault(tenderId, false))
                                .build()
                        )
                );


        fillMonitoringInfo(tenderIndicatorsCommonInfoMap, tendersMonitoringMap);
        fillIndicatorsInfo(tenderIndicatorsCommonInfoMap);
        fillTenderInfo(tenderIndicatorsCommonInfoMap);

        ArrayList<TenderInfo> tenderInfos = new ArrayList<>(tenderIndicatorsCommonInfoMap.values());
        calcScoreRank(tenderInfos);
        return tenderInfos;

    }

    private List<TenderInfo> provideBasedOnQueue(ZonedDateTime since, int size, Map<String, MonitoringEntity> tendersMonitoringMap, Map<String, Boolean> tendersTopMap) {

        Map<String, TenderInfo> tenderIndicatorsCommonInfoMap =
                regionIndicatorsQueueItemRepository.findAll()
                        .stream()
                        .sorted(Comparator.comparing(RegionIndicatorsQueueItem::getDateModified))
                        .filter(m -> m.getDateModified().isAfter(since))
                        .limit(size)
                        .collect(Collectors.toMap(
                                RegionIndicatorsQueueItem::getTenderOuterId,
                                queueItem -> TenderInfo.builder()
                                        .tenderRiskScore(0d)
                                        .tenderOuterId(queueItem.getTenderOuterId())
                                        .indicators(new HashSet<>())
                                        .indicatorsWithRisk(new HashSet<>())
                                        .indicatorsWithOutRisk(new HashSet<>())
                                        .inQueue(true)
                                        .hasPriorityStatus(tendersTopMap.getOrDefault(queueItem.getTenderOuterId(), false))
                                        .queueDateModified(queueItem.getDateModified().toOffsetDateTime().toString())
                                        .build()
                                )
                        );

        fillMonitoringInfo(tenderIndicatorsCommonInfoMap, tendersMonitoringMap);
        fillIndicatorsInfo(tenderIndicatorsCommonInfoMap);
        fillTenderInfo(tenderIndicatorsCommonInfoMap);

        ArrayList<TenderInfo> tenderInfos = new ArrayList<>(tenderIndicatorsCommonInfoMap.values());
        calcScoreRank(tenderInfos);
        return tenderInfos;
    }


    private void fillIndicatorsInfo(Map<String, TenderInfo> tenderIndicatorsCommonInfo) {
        List<Indicator> indicators = indicatorRepository.findAllByActiveTrue();
        log.info("fill indicators start");
        List<String> tenderIds = new ArrayList<>(tenderIndicatorsCommonInfo.keySet());
        List<Event> lastTendersData = extractTenderDataService.getLastTendersData(tenderIds);
        lastTendersData.forEach(event -> {
            String tenderOuterId = event.getTenderOuterId();
            String indicatorId = event.getIndicatorId();
            if (indicators.stream().anyMatch(i -> i.getId().equals(indicatorId))) {
                tenderIndicatorsCommonInfo.get(event.getTenderOuterId()).getIndicators().add(indicatorId);
            }
            if (event.getIndicatorValue() == 1) {
                tenderIndicatorsCommonInfo.get(tenderOuterId).getIndicatorsWithRisk().add(indicatorId);
                tenderIndicatorsCommonInfo.get(tenderOuterId).setTenderRiskScore(tenderIndicatorsCommonInfo
                        .get(tenderOuterId).getTenderRiskScore() + event.getIndicatorImpact());
            } else {
                tenderIndicatorsCommonInfo.get(tenderOuterId).getIndicatorsWithOutRisk().add(indicatorId);
            }
        });
        log.info("fill indicators finish");
    }

    private void fillTenderInfo(Map<String, TenderInfo> tendersInfo) {
        log.info("fill tenders start");

        List<Object[]> tenders = tenderRepository.getTendersInfo(
                String.join(",", tendersInfo.keySet()));


        Set<String> queueHistoryTenderIds = regionIndicatorsQueueItemHistoryRepository.findByTenderIdIn(
                tendersInfo.values()
                        .stream()
                        .map(TenderInfo::getTenderId)
                        .collect(Collectors.toList())
        ).stream()
                .map(RegionIndicatorsQueueItemHistory::getTenderId)
                .collect(toSet());

        tenders.forEach(item -> {

            TenderInfo tenderIndicator = tendersInfo.get(isNull(item[0]) ? null : item[0].toString());
            tenderIndicator.setHasQueueHistory(queueHistoryTenderIds.contains(tenderIndicator.getTenderId()));

            String tenderId = isNull(item[1]) ? null : item[1].toString();

            tenderIndicator.setTenderStatus(isNull(item[2]) ? null : item[2].toString());
            tenderIndicator.setProcurementMethodType(isNull(item[3]) ? null : item[3].toString());
            tenderIndicator.setExpectedValue(isNull(item[4]) ? null : Double.parseDouble(item[4].toString()));
            tenderIndicator.setCurrency(isNull(item[5]) ? null : item[5].toString());
            tenderIndicator.setCpv(isNull(item[6]) ? null : item[6].toString());
            tenderIndicator.setCpvName(isNull(item[7]) ? null : item[7].toString());
            tenderIndicator.setCpv2(isNull(item[8]) ? null : item[8].toString());
            tenderIndicator.setCpv2Name(isNull(item[9]) ? null : item[9].toString());
            tenderIndicator.setProcuringEntityEDRPOU(isNull(item[10]) ? null : item[10].toString());
            tenderIndicator.setProcuringEntityKind(isNull(item[11]) ? null : item[11].toString());
            tenderIndicator.setProcuringEntityName(isNull(item[12]) ? null : item[12].toString());
            tenderIndicator.setRegion(isNull(item[13]) ? null : item[13].toString());

            boolean hasAwardComplaints = Boolean.parseBoolean(item[14].toString());
            tenderIndicator.setTenderName(isNull(item[15]) ? null : String.valueOf(item[15].toString()));

            tenderIndicator.setTenderId(tenderId);
            boolean hasTenderComplaints = Boolean.parseBoolean(item[16].toString());
            tenderIndicator.setHasComplaints(hasAwardComplaints || hasTenderComplaints);

            tenderIndicator.setMaterialityScore(isNull(item[17]) ? null : Double.parseDouble(item[17].toString()));
            tenderIndicator.setDatePublished(tenderId.substring(3, 13));

            tenderIndicator.setProcurementMethodRationale(isNull(item[18]) ? null : item[18].toString());
            tenderIndicator.setGsw(isNull(item[19]) ? null : item[19].toString());
            tenderIndicator.setTenderDateModified(DateUtils.toZonedDateTime((Timestamp) item[20]).toOffsetDateTime().toString());


        });
        log.info("fill tenders finish");
    }

    private void fillMonitoringInfo(Map<String, TenderInfo> tenderIndicatorsCommonInfo,
                                    Map<String, MonitoringEntity> monitoringMap) {
        log.info("fill monitoring start");

        tenderIndicatorsCommonInfo.keySet().forEach(tenderId -> {
            if (monitoringMap.containsKey(tenderId)) {
                MonitoringEntity monitoring = monitoringMap.get(tenderId);
                tenderIndicatorsCommonInfo.get(tenderId).setMonitoringDateModified(monitoring.getModifiedDate());
                tenderIndicatorsCommonInfo.get(tenderId).setMonitoringStatus(monitoring.getStatus());
                tenderIndicatorsCommonInfo.get(tenderId).setMonitoringAppeal(monitoring.isAppeal());
                tenderIndicatorsCommonInfo.get(tenderId).setMonitoringCause(
                        monitoring.getCauses()
                                .stream()
                                .map(Couse::getReason)
                                .collect(toList())
                );
                tenderIndicatorsCommonInfo.get(tenderId).setMonitoringId(monitoring.getId());
                tenderIndicatorsCommonInfo.get(tenderId).setMonitoringOffice(monitoring.getOffice());
            } else {
                tenderIndicatorsCommonInfo.get(tenderId).setMonitoringStatus("None");
            }
        });
        log.info("fill monitoring finish");
    }

    private void calcScoreRank(List<TenderInfo> tenders) {
        ConfigurationDomain configuration = configurationService.getConfiguration();

        tenders.forEach(r -> {
            if (r.tenderRiskScore == null) {
                return;
            }
            if (r.tenderRiskScore < configuration.getBucketRiskGroupMediumLeft()) {
                r.tenderRiskScoreRank = TenderScoreRank.LOW;
            } else if (r.tenderRiskScore > configuration.getBucketRiskGroupMediumRight()) {
                r.tenderRiskScoreRank = TenderScoreRank.HIGH;
            } else {
                r.tenderRiskScoreRank = TenderScoreRank.MEDIUM;
            }
        });
    }

}
