package com.datapath.indicatorsresolver.service.checkIndicators.deprecated;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.indicatorsresolver.service.checkIndicators.BaseExtractor;
import com.datapath.persistence.entities.Indicator;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.datapath.indicatorsresolver.IndicatorConstants.UA_ZONE;
import static com.datapath.persistence.utils.DateUtils.toZonedDateTime;
import static java.util.Objects.isNull;

@Slf4j
@Deprecated
public class Risk_1_13_3_Extractor extends BaseExtractor {

    private final String INDICATOR_CODE = "RISK1-13_3";
    private final Integer DAYS_LIMIT = 30;

    private boolean indicatorsResolverAvailable;

    public Risk_1_13_3_Extractor() {
        indicatorsResolverAvailable = true;
    }

    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive() && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                checkDasu13_3Indicator(indicator, dateTime);
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
                checkDasu13_3Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }


    private void checkDasu13_3Indicator(Indicator indicator, ZonedDateTime dateTime) {
        log.info("{} indicator started", INDICATOR_CODE);
        while (true) {

            List<String> tenders = findTenders(
                    dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind()));

            if (tenders.isEmpty()) {
                break;
            }

            Set<String> tenderIds = new HashSet<>(tenders);

            Map<String, TenderDimensions> dimensionsMap = getTenderDimensionsWithIndicatorLastIteration(tenderIds, INDICATOR_CODE);

            Map<String, Long> maxTendersIndicatorIteration = extractDataService
                    .getMaxTenderIndicatorIteration(new HashSet<>(tenders), INDICATOR_CODE);

            Map<String, Integer> maxTendersIterationData = extractDataService
                    .getMaxTendersIterationData(maxTendersIndicatorIteration, INDICATOR_CODE);

            tenders = tenders.stream().filter(tender -> !maxTendersIterationData.containsKey(tender) ||
                    maxTendersIterationData.get(tender).equals(-2)).collect(Collectors.toList());


            List<TenderIndicator> tenderIndicators = tenderRepository.getTenderWithStartAwardDateAndMinDocDatePublished(
                    tenders.stream().collect(Collectors.joining(","))).stream().map(tenderInfo -> {
                String tenderId = tenderInfo[0].toString();

                log.info("Process tender {}", tenderId);

                ZonedDateTime awardStartDate = isNull(tenderInfo[1])
                        ? null
                        : toZonedDateTime((Timestamp) tenderInfo[1]).withZoneSameInstant(UA_ZONE)
                        .withHour(0)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0);
                ZonedDateTime minDocDatePublishDate = isNull(tenderInfo[2])
                        ? null
                        : toZonedDateTime((Timestamp) tenderInfo[2]).withZoneSameInstant(UA_ZONE)
                        .withHour(0)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0);

                int indicatorValue;

                if (isNull(awardStartDate)) {
                    indicatorValue = CONDITIONS_NOT_MET;
                } else {
                    indicatorValue = (isNull(minDocDatePublishDate)) ||
                            Duration.between(minDocDatePublishDate, awardStartDate).toDays() < DAYS_LIMIT
                            ? RISK : NOT_RISK;
                }
                TenderDimensions tenderDimensions = dimensionsMap.get(tenderId);
                return new TenderIndicator(tenderDimensions, indicator, indicatorValue, new ArrayList<>());

            }).collect(Collectors.toList());

            tenderIndicators.forEach(tenderIndicator -> {
                tenderIndicator.setTenderDimensions(dimensionsMap.get(tenderIndicator.getTenderDimensions().getId()));
                uploadIndicator(tenderIndicator);
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
