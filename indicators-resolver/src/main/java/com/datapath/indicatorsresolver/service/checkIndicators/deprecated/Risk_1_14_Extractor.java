package com.datapath.indicatorsresolver.service.checkIndicators.deprecated;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.indicatorsresolver.service.checkIndicators.BaseExtractor;
import com.datapath.persistence.entities.Indicator;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static com.datapath.persistence.utils.DateUtils.toZonedDateTime;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@Deprecated
public class Risk_1_14_Extractor extends BaseExtractor {

    private final String INDICATOR_CODE = "RISK1-14";

    private boolean indicatorsResolverAvailable;

    public Risk_1_14_Extractor() {
        indicatorsResolverAvailable = true;
    }

    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive() && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                checkDasu1_14Indicator(indicator, dateTime);
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
                ZonedDateTime dateTime = isNull(indicator.getDateChecked())
                        ? ZonedDateTime.now(ZoneId.of("UTC")).minus(Period.ofYears(1)).withHour(0)
                        : indicator.getLastCheckedDateCreated();
                checkDasu1_14Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }


    private void checkDasu1_14Indicator(Indicator indicator, ZonedDateTime dateTime) {
        log.info("{} indicator started", INDICATOR_CODE);
        while (true) {

            List<String> tenders = findTenders(
                    dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind()));

            if (tenders.isEmpty()) {break;
            }

            Set<String> tenderIds = new HashSet<>(tenders);

            Map<String, TenderDimensions> dimensionsMap = getTenderDimensionsWithIndicatorLastIteration(tenderIds, INDICATOR_CODE);

            Map<String, Long> maxTendersIndicatorIteration = extractDataService
                    .getMaxTenderIndicatorIteration(new HashSet<>(tenderIds), INDICATOR_CODE);

            Map<String, Map<String, Integer>> maxTendersLotIterationData = extractDataService
                    .getMaxTendersLotIterationData(maxTendersIndicatorIteration, INDICATOR_CODE);

            List<Object[]> tenderLotWithActiveAwardDateMinDocumentPublishedAndDocsCount =
                    tenderRepository.getTenderLotWithActiveAwardDateMinDocumentPublishedAndDocsCount(String.join(",", tenders));

            Map<String, Map<Integer, List<String>>> resultMap = new HashMap<>();
            Map<String, List<TenderIndicator>> result = new HashMap<>();

            tenderLotWithActiveAwardDateMinDocumentPublishedAndDocsCount.forEach(tenderItem -> {
                String tenderId = tenderItem[0].toString();

                log.info("Process tender {}", tenderId);

                String lotId = tenderItem[1].toString();
                ZonedDateTime activeAwardDate = isNull(tenderItem[2]) ? null : toZonedDateTime((Timestamp) tenderItem[2]);
                ZonedDateTime minDocDatePublished = isNull(tenderItem[3]) ? null : toZonedDateTime((Timestamp) tenderItem[3]);
                int docsCount = Integer.parseInt(tenderItem[4].toString());

                int indicatorValue;
                if (maxTendersLotIterationData.get(tenderId).containsKey(lotId) && maxTendersLotIterationData.get(tenderId).get(lotId).equals(1)) {
                    indicatorValue = RISK;
                } else {
                    if (isNull(activeAwardDate)) indicatorValue = CONDITIONS_NOT_MET;
                    else if ((nonNull(minDocDatePublished) && minDocDatePublished.isAfter(activeAwardDate)) ||
                            isNull(minDocDatePublished) || docsCount == 0) indicatorValue = RISK;
                    else indicatorValue = NOT_RISK;

                }
                if (!resultMap.containsKey(tenderId)) {
                    resultMap.put(tenderId, new HashMap<>());
                }
                if (!resultMap.get(tenderId).containsKey(indicatorValue)) {
                    resultMap.get(tenderId).put(indicatorValue, new ArrayList<>());
                }

                resultMap.get(tenderId).get(indicatorValue).add(lotId);
            });


            resultMap.forEach((tenderOuterId, value) -> {
                TenderDimensions tenderDimensions = dimensionsMap.get(tenderOuterId);
                value.forEach((indicatorValue, lots) -> {
                    if (!result.containsKey(tenderOuterId)) result.put(tenderOuterId, new ArrayList<>());
                    result.get(tenderOuterId).add(new TenderIndicator(tenderDimensions, indicator, indicatorValue, lots));
                });
            });

            result.forEach((tenderId, tenderIndicators) -> {
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

}
