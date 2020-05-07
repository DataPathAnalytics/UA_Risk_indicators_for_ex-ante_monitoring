package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.persistence.entities.Indicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static com.datapath.persistence.utils.DateUtils.toZonedDateTime;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;


@Service
@Slf4j
public class Risk_1_8_2_Extractor extends BaseExtractor {

    /*
    Договір опубліковано більше ніж через 22 дні пілся визначення переможця
    */

    private final String INDICATOR_CODE = "RISK1-8_2";
    private final Integer DAYS_LIMIT = 22;

    private boolean indicatorsResolverAvailable;

    public Risk_1_8_2_Extractor() {
        indicatorsResolverAvailable = true;
    }

    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getActiveIndicator(INDICATOR_CODE);
            if (nonNull(indicator) && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                checkRisk_1_8_2Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }

    public void checkIndicator() {
        if (!indicatorsResolverAvailable) {
            log.info(String.format(INDICATOR_NOT_AVAILABLE_MESSAGE_FORMAT, INDICATOR_CODE));
            return;
        }
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getActiveIndicator(INDICATOR_CODE);
            if (nonNull(indicator) && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                ZonedDateTime dateTime = isNull(indicator.getLastCheckedDateCreated())
                        ? ZonedDateTime.now().minus(Period.ofYears(1)).withHour(0)
                        : indicator.getLastCheckedDateCreated();
                checkRisk_1_8_2Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }


    private void checkRisk_1_8_2Indicator(Indicator indicator, ZonedDateTime dateTime) {
        int size = 100;
        int page = 0;

        while (true) {

            List<String> tenders = findTenders(
                    dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind()),
                    page, size);

            if (tenders.isEmpty()) {
                break;
            }

            Map<String, TenderDimensions> dimensionsMap = getTenderDimensionsWithIndicatorLastIteration(
                    new HashSet<>(tenders), INDICATOR_CODE);

            Map<String, List<TenderIndicator>> indicatorsMap = checkIndicator(tenders, indicator);
            indicatorsMap.forEach((tenderId, tenderIndicators) -> {
                tenderIndicators.forEach(tenderIndicator -> tenderIndicator
                        .setTenderDimensions(dimensionsMap.get(tenderId)));
                uploadIndicators(tenderIndicators, dimensionsMap.get(tenderId).getDruidCheckIteration());
            });

            ZonedDateTime maxTenderDateCreated = getMaxTenderDateCreated(dimensionsMap, dateTime);
            indicator.setLastCheckedDateCreated(maxTenderDateCreated);
            indicatorRepository.save(indicator);

            dateTime = maxTenderDateCreated;
        }

        log.info("{} Finish indicator processing.", INDICATOR_CODE);

        ZonedDateTime now = ZonedDateTime.now();
        indicator.setDateChecked(now);
        indicatorRepository.save(indicator);

        log.info("{} Updated indicator {}", INDICATOR_CODE, indicator);
    }

    private Map<String, List<TenderIndicator>> checkIndicator(List<String> tenderIds, Indicator indicator) {

        String tenderIdsStr = String.join(",", tenderIds);

        Map<String, Long> maxTendersIndicatorIteration = extractDataService
                .getMaxTenderIndicatorIteration(new HashSet<>(tenderIds), INDICATOR_CODE);

        Map<String, Map<String, Integer>> maxTendersLotIterationData = extractDataService
                .getMaxTendersLotIterationData(maxTendersIndicatorIteration, INDICATOR_CODE);

        List<Object[]> tenderLots = tenderRepository
                .getTenderLotsAndComplaintsCountAndActiveAwardDateAndContractStatus(tenderIdsStr);

        Map<String, List<Object>> tendersMap = new HashMap<>();
        for (Object obj : tenderLots) {
            Object[] objs = (Object[]) obj;
            String tenderId = objs[0].toString();
            List<Object> lotsInfo = tendersMap.get(tenderId);
            if (null != lotsInfo) {
                lotsInfo.add(obj);
            } else {
                lotsInfo = new ArrayList<>();
                lotsInfo.add(obj);
                tendersMap.put(tenderId, lotsInfo);
            }
        }


        Map<String, List<TenderIndicator>> indicatorsMap = new HashMap<>();
        for (String tenderId : tendersMap.keySet()) {

            TenderDimensions tenderDimensions = new TenderDimensions(tenderId);

            List<Object> lotsInfo = tendersMap.get(tenderId);
            Map<String, Integer> lotIndicators = new HashMap<>();
            for (Object lotInfoObj : lotsInfo) {
                Object[] lotInfo = (Object[]) lotInfoObj;
                String lotId = lotInfo[1].toString();
                int complaintsCount = Integer.parseInt(lotInfo[2].toString());
                Timestamp activeAwardTimestamp = lotInfo[3] == null ? null : (Timestamp) lotInfo[3];
                String contractStatus = lotInfo[4] == null ? null : lotInfo[4].toString();
                int nonSignatureDocs = Integer.parseInt(lotInfo[5].toString());
                int contractDocs = Integer.parseInt(lotInfo[6].toString());
                String lotStatus = lotInfo[7].toString();

                if (maxTendersLotIterationData.get(tenderId).containsKey(lotId) && maxTendersLotIterationData.get(tenderId).get(lotId).equals(1)) {
                    lotIndicators.put(lotId, 1);
                } else {
                    if (complaintsCount > 0 || activeAwardTimestamp == null || lotStatus.equals("cancelled") || lotStatus.equals("unsuccessful")) {
                        if (!lotIndicators.containsKey(lotId)) {
                            lotIndicators.put(lotId, -2);
                        }
                        continue;
                    }

                    if ((!contractStatus.equals("active") || (contractStatus.equals("active") && nonSignatureDocs == 0)) && contractDocs == 0) {
                        ZonedDateTime date = toZonedDateTime(activeAwardTimestamp)
                                .withZoneSameInstant(ZoneId.of("Europe/Kiev"))
                                .withHour(0).withMinute(0).withSecond(0).withNano(0);
                        ZonedDateTime now = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("Europe/Kiev"))
                                .withHour(0).withMinute(0).withSecond(0).withNano(0);
                        Duration between = Duration.between(date, now);
                        long days = between.toDays();
                        Integer indicatorValue = days > DAYS_LIMIT ? 1 : 0;
                        if (indicatorValue == 1) {
                            lotIndicators.put(lotId, indicatorValue);
                        } else {
                            if (!lotIndicators.containsKey(lotId) || lotIndicators.get(lotId) != 1) {
                                lotIndicators.put(lotId, indicatorValue);
                            }
                        }
                        continue;
                    }
                    lotIndicators.put(lotId, 0);
                }
            }

            List<TenderIndicator> tenderIndicators = new ArrayList<>();
            for (Integer value : new HashSet<>(lotIndicators.values())) {
                List<String> lotIds = new ArrayList<>();
                lotIndicators.forEach((lotId, val) -> {
                    if (value.equals(val)) {
                        lotIds.add(lotId);
                    }
                });
                tenderIndicators.add(new TenderIndicator(tenderDimensions, indicator, value, lotIds));
            }
            indicatorsMap.put(tenderId, tenderIndicators);
        }
        return indicatorsMap;
    }
}