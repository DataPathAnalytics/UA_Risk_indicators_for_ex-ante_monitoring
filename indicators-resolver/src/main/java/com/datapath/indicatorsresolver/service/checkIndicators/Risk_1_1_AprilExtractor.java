package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.repositories.derivatives.UnsuccessfulAboveRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Period;
import java.time.ZonedDateTime;
import java.util.*;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;


@Service
@Slf4j
public class Risk_1_1_AprilExtractor extends BaseExtractor {

    private static final String INDICATOR_CODE = "RISK-1-1";
    private boolean indicatorsResolverAvailable;
    private final UnsuccessfulAboveRepository unsuccessfulAboveRepository;


    public Risk_1_1_AprilExtractor(UnsuccessfulAboveRepository unsuccessfulAboveRepository) {
        this.unsuccessfulAboveRepository = unsuccessfulAboveRepository;
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

    @Scheduled(cron = "${risk-1-1.cron}")
    public void checkIndicator() {
        if (!indicatorsResolverAvailable) {
            log.info(String.format(INDICATOR_NOT_AVAILABLE_MESSAGE_FORMAT, INDICATOR_CODE));
            return;
        }
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive()) {
                ZonedDateTime dateTime = isNull(indicator.getLastCheckedDateCreated())
                        ? ZonedDateTime.now().minus(Period.ofYears(1)).withHour(0)
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

            List<String> tenders = findTenders(dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind()));
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
                    maxTendersIterationData.get(tender).equals(CONDITIONS_NOT_MET)).collect(toList());

            List<Object[]> tenderIdPAndProcuringEntityAndCPVListWithPendingContract = tenderRepository
                    .findTenderIdPAndProcuringEntityAndCPVListWithPendingContract(String.join(",", tenders));

            List<TenderIndicator> tenderIndicators = tenderIdPAndProcuringEntityAndCPVListWithPendingContract
                    .stream().map(tenderWithCPVInfo -> {
                        String tenderId = tenderWithCPVInfo[0].toString();
                        log.info("Process tender {}", tenderId);

                        TenderDimensions tenderDimensions = dimensionsMap.get(tenderId);
                        try {
                            String procuringEntity = tenderWithCPVInfo[1].toString();
                            int pendingContractsCount = Integer.parseInt(tenderWithCPVInfo[2].toString());
                            List<String> tenderCpv = Arrays.asList(tenderWithCPVInfo[3].toString().split(COMMA_SEPARATOR));
                            boolean twiceUnsuccessful = Boolean.parseBoolean(tenderWithCPVInfo[4].toString());

                            Integer indicatorValue;

                            if (!twiceUnsuccessful || pendingContractsCount == 0) {
                                indicatorValue = CONDITIONS_NOT_MET;
                            } else {
                                List<Integer> lotsCount = unsuccessfulAboveRepository
                                        .getUnsuccessfulAboveCountByProcuringEntityAndCpv(procuringEntity, tenderCpv);

                                if (lotsCount.isEmpty()) {
                                    indicatorValue = RISK;
                                } else {

                                    List<Integer> lessThenTwoUnsuccessfulProcedures = lotsCount.stream()
                                            .filter(item -> item < 2)
                                            .collect(toList());

                                    indicatorValue = (lessThenTwoUnsuccessfulProcedures.size() == lotsCount.size())
                                            ? RISK
                                            : NOT_RISK;
                                }
                            }
                            return new TenderIndicator(tenderDimensions, indicator, indicatorValue, new ArrayList<>());
                        } catch (Exception e) {
                            logService.tenderIndicatorFailed(INDICATOR_CODE, tenderId, e);
                            return new TenderIndicator(tenderDimensions, indicator, IMPOSSIBLE_TO_DETECT, new ArrayList<>());
                        }
                    }).collect(toList());

            List<String> checkedTenders = tenderIndicators.stream()
                    .map(item -> item.getTenderDimensions().getId()).collect(toList());

            tenders.forEach(tender -> {
                        TenderDimensions tenderDimensions = dimensionsMap.get(tender);
                        if (!checkedTenders.contains(tender)) {
                            tenderIndicators.add(new TenderIndicator(tenderDimensions, indicator, CONDITIONS_NOT_MET, new ArrayList<>()));
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

        log.info("{} indicator finished", INDICATOR_CODE);
    }
}
