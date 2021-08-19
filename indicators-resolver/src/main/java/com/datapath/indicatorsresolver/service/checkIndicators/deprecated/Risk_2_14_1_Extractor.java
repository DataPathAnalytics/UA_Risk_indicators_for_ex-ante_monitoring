package com.datapath.indicatorsresolver.service.checkIndicators.deprecated;

import com.datapath.druidintegration.model.druid.response.common.Event;
import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.indicatorsresolver.service.checkIndicators.BaseExtractor;
import com.datapath.persistence.entities.Indicator;
import lombok.extern.slf4j.Slf4j;

import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Slf4j
@Deprecated
public class Risk_2_14_1_Extractor extends BaseExtractor {

    private final String INDICATOR_CODE = "RISK2-14_1";
    private boolean indicatorsResolverAvailable;

    public Risk_2_14_1_Extractor() {
        indicatorsResolverAvailable = true;
    }


    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive() && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                checkRisk2_14_1Indicator(indicator, dateTime);
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
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive() && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                ZonedDateTime dateTime = isNull(indicator.getLastCheckedDateCreated())
                        ? ZonedDateTime.now(ZoneId.of("UTC")).minus(Period.ofYears(1)).withHour(0)
                        : indicator.getLastCheckedDateCreated();
                checkRisk2_14_1Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }

    private void checkRisk2_14_1Indicator(Indicator indicator, ZonedDateTime dateTime) {
        log.info("{} indicator started", INDICATOR_CODE);
        while (true) {

            List<String> tenders = tenderRepository.findGoodsServicesTenderIdByProcedureStatusAndProcedureType(
                    dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind()));

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
        ZonedDateTime now = ZonedDateTime.now();
        indicator.setDateChecked(now);
        indicatorRepository.save(indicator);

        log.info("{} indicator finished", INDICATOR_CODE);
    }

    private Map<String, List<TenderIndicator>> checkIndicator(List<String> tenderIds, Indicator indicator) {
        String tenderIdsStr = tenderIds.stream().collect(Collectors.joining(","));
        Map<String, List<TenderIndicator>> indicatorsMap = new HashMap<>();

        List<Object[]> tenderLots = tenderRepository
                .findTenderLotWithUnsuccessfulQualificationsCountByTenderId(tenderIdsStr);

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

        for (String tenderId : tendersMap.keySet()) {
            log.info("Process tender {}", tenderId);

            List<Object> lotWithUnsuccessfulQualificationsCountByTenderId = tendersMap.get(tenderId);

            Map<Integer, Set<String>> existedResultMap = new HashMap<>();
            TenderDimensions tenderDimensions = new TenderDimensions(tenderId);

            Long maxTenderIndicatorIteration = extractDataService.getMaxIndicatorIteration(tenderId, indicator.getId());
            List<Event> lastIterationData = extractDataService
                    .getLastIterationData(tenderId, indicator.getId(), maxTenderIndicatorIteration);

            lastIterationData.forEach(event -> existedResultMap.put(event.getIndicatorValue(), new HashSet<>(event.getLotIds())));

            List<String> checkedLots = existedResultMap.values().stream()
                    .flatMap(Set::stream)
                    .collect(Collectors.toList());

            lotWithUnsuccessfulQualificationsCountByTenderId.forEach(lot -> {
                Object[] lotInfo = (Object[]) lot;
                String lotId = lotInfo[1].toString();
                if (!checkedLots.contains(lotId)) {

                    int unsuccessfulQualifications = Integer.parseInt(lotInfo[2].toString());

                    Integer indicatorValue = unsuccessfulQualifications >= 3 ? RISK : NOT_RISK;

                    if (!existedResultMap.containsKey(indicatorValue)) {
                        existedResultMap.put(indicatorValue, new HashSet<>());
                    }
                    existedResultMap.get(indicatorValue).add(lotId);
                }
            });

            indicatorsMap.put(tenderId, existedResultMap.entrySet().stream()
                    .map(item -> new TenderIndicator(tenderDimensions, indicator,
                            item.getKey(), new ArrayList<String>(item.getValue())))
                    .collect(Collectors.toList()));
        }

        return indicatorsMap;
    }
}
