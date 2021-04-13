package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.datapath.indicatorsresolver.IndicatorConstants.ACTIVE;
import static com.datapath.indicatorsresolver.IndicatorConstants.UNSUCCESSFUL;
import static java.time.ZonedDateTime.now;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.*;
import static org.springframework.util.CollectionUtils.isEmpty;


@Service
@Slf4j
public class Risk_2_19_AprilExtractor extends BaseExtractor {

    private static final String INDICATOR_CODE = "RISK-2-19";
    private boolean indicatorsResolverAvailable;

    public Risk_2_19_AprilExtractor() {
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
    @Scheduled(cron = "${risk-2-19.cron}")
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

            //fixme avoid getting last iteration while receive dimensions because we will receive it later
            Map<String, TenderDimensions> dimensionsMap = getTenderDimensionsWithIndicatorLastIteration(
                    tenders, INDICATOR_CODE);

            tenders.forEach(tender -> {

                String tenderOuterId = tender.getOuterId();
                log.info("Process tender {}", tenderOuterId);
                TenderDimensions tenderDimensions = dimensionsMap.get(tenderOuterId);

                boolean hasUnsuccessfulAwards = tender.getAwards().stream().anyMatch(award -> UNSUCCESSFUL.equals(award.getStatus()));
                if (!hasUnsuccessfulAwards) {
                    TenderIndicator tenderIndicator = new TenderIndicator(tenderDimensions, indicator, CONDITIONS_NOT_MET);
                    uploadIndicatorIfNotExists(tenderOuterId, INDICATOR_CODE, singletonList(tenderIndicator));
                    return;
                }

                if (isEmpty(tender.getLots())) {
                    int indicatorValue;
                    try {
                        long participantCount = tender.getBids()
                                .stream().filter(b -> ACTIVE.equals(b.getStatus())).count();
                        long disqualificationCount = tender.getAwards().stream()
                                .filter(award -> UNSUCCESSFUL.equals(award.getStatus())).count();

                        if (disqualificationCount >= 3 && (participantCount - disqualificationCount) > 2) {
                            indicatorValue = RISK;
                        } else {
                            indicatorValue = NOT_RISK;
                        }
                    } catch (Exception e) {
                        logService.tenderIndicatorFailed(INDICATOR_CODE, tenderOuterId, e);
                        indicatorValue = IMPOSSIBLE_TO_DETECT;
                    }

                    TenderIndicator tenderIndicator = new TenderIndicator(tenderDimensions, indicator, indicatorValue);
                    uploadIndicatorIfNotExists(tenderOuterId, INDICATOR_CODE, singletonList(tenderIndicator));

                } else {
                    Map<String, Integer> lotIndicators = new HashMap<>();
                    tender.getLots().stream()
                            .filter(lot -> lot.getAwards().stream().anyMatch(award -> UNSUCCESSFUL.equals(award.getStatus())))
                            .forEach(lot -> {
                                int indicatorValue;
                                try {
                                    indicatorValue = checkLotIndicatorValue(lot);
                                } catch (Exception e) {
                                    logService.lotIndicatorFailed(INDICATOR_CODE, tenderOuterId, lot.getOuterId(), e);
                                    indicatorValue = IMPOSSIBLE_TO_DETECT;
                                }
                                lotIndicators.put(lot.getOuterId(), indicatorValue);
                            });

                    Map<Integer, List<String>> lotGroupedByRiskValue = lotIndicators.entrySet().stream()
                            .collect(groupingBy(Map.Entry::getValue, mapping(Map.Entry::getKey, toList())));

                    List<TenderIndicator> tenderIndicators = new ArrayList<>();
                    lotGroupedByRiskValue.forEach((indicatorValue, lots) ->
                            tenderIndicators.add(new TenderIndicator(tenderDimensions, indicator, indicatorValue, lots)));

                    uploadIndicatorIfNotExists(tenderOuterId, INDICATOR_CODE, tenderIndicators);
                }
            });

            ZonedDateTime maxTenderDateCreated = getMaxTenderDateCreated(dimensionsMap, dateTime);
            indicator.setLastCheckedDateCreated(maxTenderDateCreated);
            indicatorRepository.save(indicator);

            dateTime = maxTenderDateCreated;
        }

        indicator.setDateChecked(now());
        indicatorRepository.save(indicator);

        log.info("{} indicator finished", INDICATOR_CODE);
    }

    private int checkLotIndicatorValue(Lot lot) {
        long participantCount = lot.getBids()
                .stream().filter(b -> ACTIVE.equals(b.getStatus())).count();
        long disqualificationCount = lot.getAwards().stream()
                .filter(award -> UNSUCCESSFUL.equals(award.getStatus())).count();

        return disqualificationCount >= 3 && (participantCount - disqualificationCount) > 2 ? RISK : NOT_RISK;
    }
}
