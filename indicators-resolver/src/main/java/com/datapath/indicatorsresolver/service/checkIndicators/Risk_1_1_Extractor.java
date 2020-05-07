package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.repositories.derivatives.UnsuccessfulAboveRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;


@Service
@Slf4j
public class Risk_1_1_Extractor extends BaseExtractor {

    /*
   Переговорна процедура проведена за відсутності двох неуспішних процедур
     */

    private final String INDICATOR_CODE = "RISK1-1";
    private boolean indicatorsResolverAvailable;
    private final UnsuccessfulAboveRepository unsuccessfulAboveRepository;


    public Risk_1_1_Extractor(UnsuccessfulAboveRepository unsuccessfulAboveRepository) {
        this.unsuccessfulAboveRepository = unsuccessfulAboveRepository;
        indicatorsResolverAvailable = true;
    }


    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getActiveIndicator(INDICATOR_CODE);
            if (nonNull(indicator) && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                checkDasu1_1Indicator(indicator, dateTime);
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
                        ? ZonedDateTime.now(ZoneId.of("UTC")).minus(Period.ofYears(1)).withHour(0)
                        : indicator.getLastCheckedDateCreated();
                checkDasu1_1Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }

    private void checkDasu1_1Indicator(Indicator indicator, ZonedDateTime dateTime) {
        int size = 100;
        int page = 0;
        while (true) {

            List<String> tenders = findTenders(dateTime,
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

            tenders = tenders.stream().filter(tender -> !maxTendersIterationData.containsKey(tender) ||
                    maxTendersIterationData.get(tender).equals(-2)).collect(toList());

            List<Object[]> tenderIdPAndProcuringEntityAndCPVListWithPendingContract = tenderRepository
                    .findTenderIdPAndProcuringEntityAndCPVListWithPendingContract(String.join(",", tenders));

            List<TenderIndicator> tenderIndicators = tenderIdPAndProcuringEntityAndCPVListWithPendingContract
                    .stream().map(tenderWithCPVInfo -> {
                        String tenderId = tenderWithCPVInfo[0].toString();

                        String procuringEntity = tenderWithCPVInfo[1].toString();
                        Integer pendingContractsCount = Integer.parseInt(tenderWithCPVInfo[2].toString());
                        List<String> tenderCpv = Arrays.asList(tenderWithCPVInfo[3].toString().split(COMMA_SEPARATOR));
                        Boolean twiceUnsuccessful = Boolean.parseBoolean(tenderWithCPVInfo[4].toString());
                        TenderDimensions tenderDimensions = dimensionsMap.get(tenderId);

                        Integer indicatorValue;

                        List<Integer> proceduresCount = unsuccessfulAboveRepository
                                .getUnsuccessfulAboveCountByProcuringEntityAndTenderCpv(procuringEntity, tenderCpv);

                        if (!twiceUnsuccessful || pendingContractsCount == 0) {
                            indicatorValue = -2;
                        } else {
                            if (proceduresCount.isEmpty()) {
                                indicatorValue = RISK;
                            } else {

                                List<Integer> lessThenTwoUnsuccessfulProcedures = proceduresCount.stream()
                                        .filter(item -> item < 2)
                                        .collect(toList());

                                indicatorValue = (lessThenTwoUnsuccessfulProcedures.size() == proceduresCount.size())
                                        ? RISK
                                        : NOT_RISK;
                            }
                        }
                        return new TenderIndicator(tenderDimensions, indicator, indicatorValue, new ArrayList<>());

                    }).collect(toList());

            List<String> checkedTenders = tenderIndicators.stream()
                    .map(item -> item.getTenderDimensions().getId()).collect(toList());

            tenders.forEach(tender -> {
                        TenderDimensions tenderDimensions = dimensionsMap.get(tender);
                        if (!checkedTenders.contains(tender)) {
                            tenderIndicators.add(new TenderIndicator(tenderDimensions, indicator, -2, new ArrayList<>()));
                        }
                    }
            );

            tenderIndicators.forEach(this::uploadIndicator);

            ZonedDateTime maxTenderDateCreated = getMaxTenderDateCreated(dimensionsMap, dateTime);
            indicator.setLastCheckedDateCreated(maxTenderDateCreated);
            indicatorRepository.save(indicator);

            dateTime = maxTenderDateCreated;
        }
        ZonedDateTime now = ZonedDateTime.now();
        indicator.setDateChecked(now);
        indicatorRepository.save(indicator);
    }
}
