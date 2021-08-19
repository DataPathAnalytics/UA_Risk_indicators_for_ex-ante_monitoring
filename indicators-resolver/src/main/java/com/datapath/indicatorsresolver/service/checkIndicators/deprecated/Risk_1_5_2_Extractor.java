package com.datapath.indicatorsresolver.service.checkIndicators.deprecated;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.indicatorsresolver.service.checkIndicators.BaseExtractor;
import com.datapath.persistence.entities.Indicator;
import lombok.extern.slf4j.Slf4j;

import java.time.Period;
import java.time.ZonedDateTime;
import java.util.*;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@Deprecated
public class Risk_1_5_2_Extractor extends BaseExtractor {

    private final String INDICATOR_CODE = "RISK1-5_2";

    private final Double PERCENTAGE_DIFF_LIMIT = 0.500001;

    private boolean indicatorsResolverAvailable;

    public Risk_1_5_2_Extractor() {
        indicatorsResolverAvailable = true;
    }


    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive() && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                checkRisk_1_5_2Indicator(indicator, dateTime);
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
                        ? ZonedDateTime.now().minus(Period.ofYears(1)).withHour(0)
                        : indicator.getLastCheckedDateCreated();
                checkRisk_1_5_2Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }


    private void checkRisk_1_5_2Indicator(Indicator indicator, ZonedDateTime dateTime) {
        log.info("{} indicator started", INDICATOR_CODE);
        while (true) {

            List<String> tenders = tenderRepository.findWorksTenderIdByProcedureStatusAndProcedureType(
                    dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind()));
            if (tenders.isEmpty()) {
                break;
            }

            Map<String, TenderDimensions> dimensionsMap = getTenderDimensionsWithIndicatorLastIteration(new HashSet<>(tenders), INDICATOR_CODE);

            Map<String, List<TenderIndicator>> tenderIndicatorsMap = checkIndicator(tenders, indicator);
            tenderIndicatorsMap.forEach((tenderId, tenderIndicators) -> {
                tenderIndicators.forEach(tenderIndicator -> tenderIndicator.setTenderDimensions(dimensionsMap.get(tenderId)));
                uploadIndicators(tenderIndicators, dimensionsMap.get(tenderId).getDruidCheckIteration());
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

    private Map<String, List<TenderIndicator>> checkIndicator(List<String> tenderIds, Indicator indicator) {

        Map<String, Map<Integer, List<String>>> resultMap = new HashMap<>();
        Map<String, List<TenderIndicator>> result = new HashMap<>();
        List<Object[]> activeLotsAmountAndGuaranteeAmountWithCurrencies = lotRepository
                .getActiveLotsAmountAndGuaranteeAmountWithCurrencies(tenderIds);

        activeLotsAmountAndGuaranteeAmountWithCurrencies.forEach(lotInfo -> {
            String tenderId = lotInfo[0].toString();

            log.info("Process tender {}", tenderId);

            String lotId = lotInfo[1].toString();
            Double amount = Double.parseDouble(lotInfo[2].toString());
            Integer indicatorValue;
            if (nonNull(lotInfo[3])) {
                Double guaranteeAmount = Double.parseDouble(lotInfo[3].toString());
                double guaranteeShare = (guaranteeAmount / amount) * 100;
                indicatorValue = guaranteeShare > PERCENTAGE_DIFF_LIMIT ? RISK : NOT_RISK;
            } else {
                indicatorValue = CONDITIONS_NOT_MET;
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
            TenderDimensions tenderDimensions = new TenderDimensions(tenderOuterId);
            value.forEach((indicatorValue, lots) -> {
                if (!result.containsKey(tenderOuterId)) {
                    result.put(tenderOuterId, new ArrayList<>());
                }
                result.get(tenderOuterId).add(new TenderIndicator(tenderDimensions, indicator,
                        indicatorValue, lots));
            });
        });
        return result;

    }

}
