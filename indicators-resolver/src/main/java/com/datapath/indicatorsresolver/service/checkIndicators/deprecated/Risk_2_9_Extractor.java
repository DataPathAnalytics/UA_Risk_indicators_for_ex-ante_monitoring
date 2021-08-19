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
public class Risk_2_9_Extractor extends BaseExtractor {

    private final String INDICATOR_CODE = "RISK2-9";
    private boolean indicatorsResolverAvailable;


    public Risk_2_9_Extractor() {
        indicatorsResolverAvailable = true;
    }


    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive() && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                checkRiskDasu1Indicator(indicator, dateTime);
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
                checkRiskDasu1Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }

    private void checkRiskDasu1Indicator(Indicator indicator, ZonedDateTime dateTime) {
        log.info("{} indicator started", INDICATOR_CODE);
        while (true) {

            List<Object[]> tenders = tenderRepository.findTenderIdsWithCPVListWithPendingContractsAndNotMonopolySupplier(
                    dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind()));

            if (tenders.isEmpty()) {
                break;
            }

            Set<String> tenderIds = tenders.stream().map(item -> item[0].toString()).collect(Collectors.toSet());

            Map<String, TenderDimensions> dimensionsMap = getTenderDimensionsWithIndicatorLastIteration(tenderIds, INDICATOR_CODE);

            Map<String, Long> maxTendersIndicatorIteration = extractDataService
                    .getMaxTenderIndicatorIteration(tenderIds, INDICATOR_CODE);

            Map<String, Integer> maxTendersIterationData = extractDataService
                    .getMaxTendersIterationData(maxTendersIndicatorIteration, INDICATOR_CODE);

            tenders = tenders.stream()
                    .filter(tender -> !maxTendersIterationData.containsKey(tender[0].toString()) ||
                            maxTendersIterationData.get(tender[0].toString()).equals(-2)).collect(Collectors.toList());


            tenders.forEach(tenderCPVsInfo -> {
                String tenderId = tenderCPVsInfo[0].toString();

                log.info("Process tender {}", tenderId);

                try {
                    tenderIds.add(tenderId);
                    Integer pendingContractsCount = Integer.parseInt(tenderCPVsInfo[1].toString());
                    Boolean containsCpv = Boolean.parseBoolean(tenderCPVsInfo[2].toString());
                    Boolean amountLimitCondition = Boolean.parseBoolean(tenderCPVsInfo[3].toString());
                    Boolean monopolySupplier = Boolean.parseBoolean(tenderCPVsInfo[4].toString());
                    Boolean noCompetition = Boolean.parseBoolean(tenderCPVsInfo[5].toString());
                    Integer indicatorValue;

                    if (!amountLimitCondition || !noCompetition || monopolySupplier) {
                        indicatorValue = CONDITIONS_NOT_MET;
                    } else {
                        indicatorValue = pendingContractsCount > 0 && containsCpv ? RISK : NOT_RISK;
                    }

                    TenderDimensions tenderDimensions = dimensionsMap.get(tenderId);

                    TenderIndicator tenderIndicator = new TenderIndicator(tenderDimensions, indicator, indicatorValue, new ArrayList<>());
                    uploadIndicatorIfNotExists(tenderIndicator.getTenderDimensions().getId(), INDICATOR_CODE, tenderIndicator);

                } catch (Exception ex) {
                    log.info(String.format(TENDER_INDICATOR_ERROR_MESSAGE, INDICATOR_CODE, tenderId));
                }
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
