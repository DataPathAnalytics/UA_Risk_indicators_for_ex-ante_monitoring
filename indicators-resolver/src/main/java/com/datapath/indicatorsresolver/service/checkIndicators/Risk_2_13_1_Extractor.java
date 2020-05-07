package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.persistence.entities.Indicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;


@Service
@Slf4j
public class Risk_2_13_1_Extractor extends BaseExtractor {

    /*
    Усі Учасники торгів були дискваліфіковані, за винятком переможця (роботи).
     */

    private final String INDICATOR_CODE = "RISK2-13-1";
    private boolean indicatorsResolverAvailable;

    public Risk_2_13_1_Extractor() {
        indicatorsResolverAvailable = true;
    }


    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getActiveIndicator(INDICATOR_CODE);
            if (nonNull(indicator) && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                checkRiskDasu2_13_1Indicator(indicator, dateTime);
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
                checkRiskDasu2_13_1Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }

    private void checkRiskDasu2_13_1Indicator(Indicator indicator, ZonedDateTime dateTime) {
        int size = 100;
        int page = 0;
        while (true) {

            List<String> tenders = tenderRepository.findWorksTenderIdByProcedureStatusAndProcedureType(
                    dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind()),
                    PageRequest.of(page, size));

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
                int winner = Integer.parseInt(lotInfo[2].toString());
                int disquals = Integer.parseInt(lotInfo[3].toString());
                int participation = Integer.parseInt(lotInfo[4].toString());

                Integer indicatorValue;

                if (disquals == 0 || winner == 0) {
                    indicatorValue = -2;
                } else {
                    indicatorValue = (disquals > 2) && ((winner + disquals) == participation) ? RISK : NOT_RISK;
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

            result.forEach((tenderId, tenderIndicators) -> uploadIndicators(tenderIndicators, dimensionsMap.get(tenderId).getDruidCheckIteration()));

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
