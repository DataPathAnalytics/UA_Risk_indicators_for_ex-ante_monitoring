package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.persistence.entities.Award;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.entities.Lot;
import com.datapath.persistence.entities.Tender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Period;
import java.time.ZonedDateTime;
import java.util.*;

import static java.time.ZonedDateTime.now;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.*;
import static org.springframework.util.CollectionUtils.isEmpty;


@Service
@Slf4j
public class Risk_1_12_AprilExtractor extends BaseExtractor {

    private static final String INDICATOR_CODE = "RISK-1-12";
    private boolean indicatorsResolverAvailable;

    public Risk_1_12_AprilExtractor() {
        indicatorsResolverAvailable = true;
    }

    @Transactional
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
    @Transactional
    @Scheduled(cron = "${risk-1-12.cron}")
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
                        ? now().minus(Period.ofYears(1)).withHour(0)
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

            List<Tender> tenders = findTenders(dateTime, indicator);

            if (tenders.isEmpty()) break;

            List<String> tenderIds = tenders.stream().map(Tender::getOuterId).collect(toList());

            //fixme avoid getting last iteration while receive dimensions because we will receive it later
            Map<String, TenderDimensions> dimensionsMap = getTenderDimensionsWithIndicatorLastIteration(
                    tenders, INDICATOR_CODE);

            Map<String, Long> maxTendersIndicatorIteration = extractDataService
                    .getMaxTenderIndicatorIteration(new HashSet<>(tenderIds), INDICATOR_CODE);

            Map<String, Map<String, Integer>> maxTendersLotIterationData = extractDataService
                    .getMaxTendersLotIterationData(maxTendersIndicatorIteration, INDICATOR_CODE);

            tenders.forEach(tender -> {
                Map<String, Integer> lotIndicators = new HashMap<>();
                String tenderOuterId = tender.getOuterId();
                tender.getLots().forEach(lot -> {
                    try {
                        if (maxTendersLotIterationData.get(tenderOuterId).containsKey(lot.getOuterId())
                                && maxTendersLotIterationData.get(tenderOuterId).get(lot.getOuterId()).equals(RISK)) {
                            lotIndicators.put(lot.getOuterId(), RISK);
                        } else {
                            int indicatorValue = checkLotIndicatorValue(lot);
                            lotIndicators.put(lot.getOuterId(), indicatorValue);
                        }
                    } catch (Exception e) {
                        logService.lotIndicatorFailed(INDICATOR_CODE, tenderOuterId, lot.getOuterId(), e);
                        lotIndicators.put(lot.getOuterId(), IMPOSSIBLE_TO_DETECT);
                    }
                });

                Map<Integer, List<String>> lotGroupedByRiskValue = lotIndicators.entrySet().stream()
                        .collect(groupingBy(Map.Entry::getValue, mapping(Map.Entry::getKey, toList())));

                List<TenderIndicator> tenderIndicators = new ArrayList<>();
                lotGroupedByRiskValue.forEach((indicatorValue, lots) -> {
                    tenderIndicators.add(new TenderIndicator(dimensionsMap.get(tenderOuterId), indicator, indicatorValue, lots));
                });

                uploadIndicatorIfNotExists(tenderOuterId, INDICATOR_CODE, tenderIndicators);

            });

            ZonedDateTime maxTenderDateCreated = getMaxTenderDateCreated(dimensionsMap, dateTime);
            indicator.setLastCheckedDateCreated(maxTenderDateCreated);
            indicatorRepository.save(indicator);

            dateTime = maxTenderDateCreated;
        }

        ZonedDateTime now = now();
        indicator.setDateChecked(now);
        indicatorRepository.save(indicator);

        log.info("{} indicator finished", INDICATOR_CODE);
    }

    private int checkLotIndicatorValue(Lot lot) {
        List<Award> awards = !isEmpty(lot.getAwards()) ? lot.getAwards() : Collections.emptyList();

        boolean hasAwardComplaints = awards.stream().anyMatch(a -> !isEmpty(a.getComplaints()));
        if (hasAwardComplaints) {
            return CONDITIONS_NOT_MET;
        }

        boolean hasPendingAwards = awards.stream().anyMatch(a -> "pending".equalsIgnoreCase(a.getStatus()));
        if (!hasPendingAwards) {
            return NOT_RISK;
        }

        ZonedDateTime awardMaxDate = getDateOfCurrentDateMinusNWorkingDays(20);
        boolean hasOutdatedAwards = awards.stream().anyMatch(a -> a.getDate().isBefore(awardMaxDate));

        if (hasPendingAwards && hasOutdatedAwards) {
            return RISK;
        }

        return IMPOSSIBLE_TO_DETECT;
    }
}
