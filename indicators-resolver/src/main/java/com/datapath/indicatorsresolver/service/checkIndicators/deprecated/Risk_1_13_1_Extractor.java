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
import java.util.stream.Collectors;

import static com.datapath.persistence.utils.DateUtils.toZonedDateTime;
import static java.util.Objects.isNull;


@Slf4j
@Deprecated
public class Risk_1_13_1_Extractor extends BaseExtractor {

    private final String INDICATOR_CODE = "RISK1-13_1";

    private boolean indicatorsResolverAvailable;

    public Risk_1_13_1_Extractor() {
        indicatorsResolverAvailable = true;
    }

    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive() && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                checkRisk_1_13_1Indicator(indicator, dateTime);
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
                checkRisk_1_13_1Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }


    private void checkRisk_1_13_1Indicator(Indicator indicator, ZonedDateTime dateTime) {
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

                            ZonedDateTime minAwardDate = isNull(tenderItem[1])
                                    ? null
                                    : toZonedDateTime((Timestamp) tenderItem[1]);
                            ZonedDateTime minDatePublished = isNull(tenderItem[2])
                                    ? null
                                    : toZonedDateTime((Timestamp) tenderItem[2]);
                            TenderDimensions tenderDimensions = new TenderDimensions(tenderId);
                            Integer indicatorValue;
                            if (isNull(minAwardDate)) {
                                indicatorValue = CONDITIONS_NOT_MET;
                            } else {
                                indicatorValue = isNull(minDatePublished) || minDatePublished.isAfter(minAwardDate)
                                        ? RISK
                                        : NOT_RISK;
                            }
                            result.put(tenderId, new TenderIndicator(tenderDimensions, indicator, indicatorValue));

                        }
                );
        return result;
    }
}
