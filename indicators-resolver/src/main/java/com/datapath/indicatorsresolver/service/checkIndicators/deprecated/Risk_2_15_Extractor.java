package com.datapath.indicatorsresolver.service.checkIndicators.deprecated;

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
public class Risk_2_15_Extractor extends BaseExtractor {

    private final String INDICATOR_CODE = "RISK2-15";

    private boolean indicatorsResolverAvailable;

    public Risk_2_15_Extractor() {
        indicatorsResolverAvailable = true;
    }

    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive() && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                checkRisk_2_15Indicator(indicator, dateTime);
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
                checkRisk_2_15Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }


    private void checkRisk_2_15Indicator(Indicator indicator, ZonedDateTime dateTime) {
        log.info("{} indicator started", INDICATOR_CODE);
        while (true) {

            List<Object[]> tendersInfo = tenderRepository.findTendersWithUnuniqueTenderersCount(
                    dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind()));

            if (tendersInfo.isEmpty()) {
                break;
            }

            HashSet<String> tenders = new HashSet<>();

            List<TenderIndicator> tenderIndicators = tendersInfo.stream().map(tenderInfo -> {
                String tenderId = tenderInfo[0].toString();

                log.info("Process tender {}", tenderId);

                tenders.add(tenderId);
                try {
                    TenderDimensions tenderDimensions = new TenderDimensions(tenderId);
                    Integer ununiqueBiddersCount = Integer.parseInt(tenderInfo[1].toString());

                    Integer indicatorValue = ununiqueBiddersCount.equals(0) ? NOT_RISK : RISK;
                    return new TenderIndicator(tenderDimensions, indicator,
                            indicatorValue, new ArrayList<>());
                } catch (Exception ex) {
                    log.info(String.format(TENDER_INDICATOR_ERROR_MESSAGE, INDICATOR_CODE, tenderId));
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());

            Map<String, TenderDimensions> dimensionsMap = getTenderDimensionsWithIndicatorLastIteration(tenders,
                    INDICATOR_CODE);


            tenderIndicators.forEach(tenderIndicator -> {
                tenderIndicator.setTenderDimensions(dimensionsMap.get(tenderIndicator.getTenderDimensions().getId()));
                uploadIndicatorIfNotExists(tenderIndicator.getTenderDimensions().getId(), INDICATOR_CODE, tenderIndicator);
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
