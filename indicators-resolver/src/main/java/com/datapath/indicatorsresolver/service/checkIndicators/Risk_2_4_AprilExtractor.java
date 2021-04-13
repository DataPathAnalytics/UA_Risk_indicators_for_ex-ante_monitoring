package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.persistence.entities.Indicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;


@Service
@Slf4j
public class Risk_2_4_AprilExtractor extends BaseExtractor {

    private final String INDICATOR_CODE = "RISK-2-4";
    private boolean indicatorsResolverAvailable;


    public Risk_2_4_AprilExtractor() {
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
    @Scheduled(cron = "${risk-2-4.cron}")
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

            List<String> tenders = tenderRepository.getIndicator2_4TenderData(
                    dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind()));

            if (tenders.isEmpty()) {
                break;
            }

            Map<String, TenderDimensions> dimensionsMap = getTenderDimensionsWithIndicatorLastIteration(new HashSet<>(tenders), INDICATOR_CODE);

            List<Object[]> lotWinnerDisqualsParticipationsByTenderId = lotRepository.findLotWinnerDisqualsParticipationsByTenderId(tenders.stream().collect(Collectors.joining(",")));

            Map<String, Map<Integer, List<String>>> resultMap = new HashMap<>();
            Map<String, List<TenderIndicator>> result = new HashMap<>();

            lotWinnerDisqualsParticipationsByTenderId.forEach(lotInfo -> {
                String tenderId = lotInfo[0].toString();
                String lotId = lotInfo[1].toString();

                log.info("Process tender {} lot {}", tenderId, lotId);

                int indicatorValue;
                try {
                    int winner = Integer.parseInt(lotInfo[2].toString());
                    int disquals = Integer.parseInt(lotInfo[3].toString());
                    int participation = Integer.parseInt(lotInfo[4].toString());

                    if (disquals == 0 || winner == 0) {
                        indicatorValue = CONDITIONS_NOT_MET;
                    } else {
                        indicatorValue = (disquals > 2) && ((winner + disquals) == participation) ? RISK : NOT_RISK;
                    }
                } catch (Exception e) {
                    logService.lotIndicatorFailed(INDICATOR_CODE, tenderId, lotId, e);
                    indicatorValue = IMPOSSIBLE_TO_DETECT;
                }

                if (!resultMap.containsKey(tenderId)) {
                    resultMap.put(tenderId, new HashMap<>());
                }
                if (!resultMap.get(tenderId).containsKey(indicatorValue)) {
                    resultMap.get(tenderId).put(indicatorValue, new ArrayList<>());
                }
                resultMap.get(tenderId).get(indicatorValue).add(lotId);

            });

            resultMap.forEach((tenderOuterId, value) -> {
                TenderDimensions tenderDimensions = dimensionsMap.get(tenderOuterId);
                value.forEach((indicatorValue, lots) -> {
                    if (!result.containsKey(tenderOuterId)) result.put(tenderOuterId, new ArrayList<>());
                    result.get(tenderOuterId).add(new TenderIndicator(tenderDimensions, indicator, indicatorValue, lots));
                });
            });

            result.forEach((tenderId, tenderIndicators) -> uploadIndicatorIfNotExists(tenderId, INDICATOR_CODE, tenderIndicators));

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
