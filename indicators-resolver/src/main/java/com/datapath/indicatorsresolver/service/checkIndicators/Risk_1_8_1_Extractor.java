package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.persistence.entities.Indicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Period;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;


@Service
@Slf4j
public class Risk_1_8_1_Extractor extends BaseExtractor {

    /*
    Порушення термінів укладення договору (договір укладено занадто рано)
    */

    private final String INDICATOR_CODE = "RISK1-8_1";
    private final Integer DAYS_LIMIT = 10;

    private boolean indicatorsResolverAvailable;

    public Risk_1_8_1_Extractor() {
        indicatorsResolverAvailable = true;
    }

    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getActiveIndicator(INDICATOR_CODE);
            if (nonNull(indicator) && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                checkRisk_1_8_1Indicator(indicator, dateTime);
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
                checkRisk_1_8_1Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }


    private void checkRisk_1_8_1Indicator(Indicator indicator, ZonedDateTime dateTime) {
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

            Map<String, Long> maxTendersIndicatorIteration = extractDataService
                    .getMaxTenderIndicatorIteration(new HashSet<>(tenders), INDICATOR_CODE);

            Map<String, Integer> maxTendersIterationData = extractDataService
                    .getMaxTendersIterationData(maxTendersIndicatorIteration, INDICATOR_CODE);

            tenders = tenders.stream().filter(tender -> {
                if (!maxTendersIterationData.containsKey(tender)) {
                    return true;
                }

                Integer val = maxTendersIterationData.get(tender);
                return !val.equals(0) && !val.equals(1);
            }).collect(Collectors.toList());

            Map<String, List<TenderIndicator>> indicatorsMap = checkIndicator(tenders, indicator);
            indicatorsMap.forEach((tenderId, tenderIndicators) -> {
                tenderIndicators.forEach(tenderIndicator -> tenderIndicator.setTenderDimensions(dimensionsMap.get(tenderId)));
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

        String tenderIdsStr = tenderIds.stream().collect(Collectors.joining(","));

        List<Object[]> tenderLots = tenderRepository
                .getTenderWithLotAndDaysBetweenAwardDateAndDocumentDateModified(tenderIdsStr);

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
                if (null == lotInfo[2]) {
                    lotIndicators.put(lotId, -2);
                    continue;
                }

                int days = Integer.parseInt(lotInfo[2].toString());
                lotIndicators.put(lotId, days < DAYS_LIMIT ? 1 : 0);
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
