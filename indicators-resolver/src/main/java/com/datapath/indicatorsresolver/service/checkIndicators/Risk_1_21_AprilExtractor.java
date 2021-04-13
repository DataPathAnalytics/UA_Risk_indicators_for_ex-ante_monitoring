package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.persistence.entities.Indicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.datapath.persistence.utils.DateUtils.toZonedDateTime;
import static java.util.Objects.isNull;


@Service
@Slf4j
public class Risk_1_21_AprilExtractor extends BaseExtractor {

    private final String INDICATOR_CODE = "RISK-1-21";
    private final Integer DAYS_LIMIT = 15;

    private boolean indicatorsResolverAvailable;

    public Risk_1_21_AprilExtractor() {
        indicatorsResolverAvailable = true;
    }

    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive()) {
                checkIndicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }

    @Async
    @Scheduled(cron = "${risk-1-21.cron}")
    public void checkIndicator() {
        if (!indicatorsResolverAvailable) {
            log.info(String.format(INDICATOR_NOT_AVAILABLE_MESSAGE_FORMAT, INDICATOR_CODE));
            return;
        }
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive()) {
                ZonedDateTime dateTime = isNull(indicator.getDateChecked())
                        ? ZonedDateTime.now(ZoneId.of("UTC")).minus(Period.ofYears(1)).withHour(0)
                        : indicator.getLastCheckedDateCreated();
                checkIndicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }


    private void checkIndicator(Indicator indicator, ZonedDateTime dateTime) {
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


            List<TenderIndicator> tenderIndicators = tenderRepository.getTenderWithEndDateAndMinDocDatePublished(
                    String.join(",", tenders))
                    .stream()
                    .map(tenderInfo -> {
                        String tenderId = tenderInfo[0].toString();
                        TenderDimensions tenderDimensions = dimensionsMap.get(tenderId);

                        log.info("Process tender {}", tenderId);

                        Integer indicatorValue;
                        try {
                            ZonedDateTime endDate = isNull(tenderInfo[1]) ? null : toZonedDateTime((Timestamp) tenderInfo[1]);
                            ZonedDateTime minDocDatePublishDate = isNull(tenderInfo[2]) ? null : toZonedDateTime((Timestamp) tenderInfo[2]);

                            if (isNull(endDate)) {
                                indicatorValue = CONDITIONS_NOT_MET;
                            } else {
                                indicatorValue = (isNull(minDocDatePublishDate)) ||
                                        getDaysBetween(minDocDatePublishDate, endDate) < DAYS_LIMIT ? RISK : NOT_RISK;
                            }
                        } catch (Exception e) {
                            logService.tenderIndicatorFailed(INDICATOR_CODE, tenderId, e);
                            indicatorValue = IMPOSSIBLE_TO_DETECT;
                        }

                        return new TenderIndicator(tenderDimensions, indicator, indicatorValue);
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
