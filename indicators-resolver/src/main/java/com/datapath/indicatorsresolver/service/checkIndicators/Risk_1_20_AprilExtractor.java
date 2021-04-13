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
public class Risk_1_20_AprilExtractor extends BaseExtractor {

    private final String INDICATOR_CODE = "RISK-1-20";

    private boolean indicatorsResolverAvailable;

    public Risk_1_20_AprilExtractor() {
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
    @Scheduled(cron = "${risk-1-20.cron}")
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

            Map<String, TenderDimensions> dimensionsMap = getTenderDimensionsWithIndicatorLastIteration(new HashSet<>(tenders), INDICATOR_CODE);

            Map<String, TenderIndicator> tenderIndicatorsMap = checkIndicator(tenders, indicator);

            tenderIndicatorsMap.forEach((tenderId, tenderIndicator) -> {
                tenderIndicator.setTenderDimensions(dimensionsMap.get(tenderId));
                uploadIndicatorIfNotExists(tenderId, INDICATOR_CODE, tenderIndicator);
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

    private Map<String, TenderIndicator> checkIndicator(List<String> tenderIds, Indicator indicator) {

        Map<String, TenderIndicator> result = new HashMap<>();
        tenderRepository.getTenderIdMinAwardDateMinDocumentDatePublished(
                tenderIds.stream().collect(Collectors.joining(",")))
                .forEach(
                        tenderItem -> {
                            String tenderId = tenderItem[0].toString();

                            log.info("Process tender {}", tenderId);

                            TenderDimensions tenderDimensions = new TenderDimensions(tenderId);
                            Integer indicatorValue;
                            try {
                                ZonedDateTime minAwardDate = isNull(tenderItem[1])
                                        ? null
                                        : toZonedDateTime((Timestamp) tenderItem[1]);
                                ZonedDateTime minDatePublished = isNull(tenderItem[2])
                                        ? null
                                        : toZonedDateTime((Timestamp) tenderItem[2]);

                                if (isNull(minAwardDate)) {
                                    indicatorValue = CONDITIONS_NOT_MET;
                                } else {
                                    indicatorValue = isNull(minDatePublished) || minDatePublished.isAfter(minAwardDate)
                                            ? RISK
                                            : NOT_RISK;
                                }
                            } catch (Exception e) {
                                logService.tenderIndicatorFailed(INDICATOR_CODE, tenderId, e);
                                indicatorValue = IMPOSSIBLE_TO_DETECT;
                            }
                            result.put(tenderId, new TenderIndicator(tenderDimensions, indicator, indicatorValue));
                        }
                );
        return result;
    }
}
