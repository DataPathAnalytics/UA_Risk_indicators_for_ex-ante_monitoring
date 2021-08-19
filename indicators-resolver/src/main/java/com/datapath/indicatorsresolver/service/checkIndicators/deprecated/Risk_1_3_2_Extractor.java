package com.datapath.indicatorsresolver.service.checkIndicators.deprecated;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.indicatorsresolver.service.checkIndicators.BaseExtractor;
import com.datapath.persistence.entities.Indicator;
import lombok.extern.slf4j.Slf4j;

import java.time.Period;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;


@Slf4j
@Deprecated
public class Risk_1_3_2_Extractor extends BaseExtractor {

    private final String INDICATOR_CODE = "RISK1-3_2";
    private final String PKCS7_SIGNATURE_FORMAT = "application/pkcs7-signature";
    private boolean indicatorsResolverAvailable;


    public Risk_1_3_2_Extractor() {
        indicatorsResolverAvailable = true;
    }


    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive() && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                checkRisk_1_3_2_Indicator(indicator, dateTime);
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
                checkRisk_1_3_2_Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }

    private void checkRisk_1_3_2_Indicator(Indicator indicator, ZonedDateTime dateTime) {
        log.info("{} indicator started", INDICATOR_CODE);
        while (true) {

            List<String> tenderIds = findTenders(dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind()));

            if (tenderIds.isEmpty()) {
                break;
            }

            Map<String, TenderDimensions> dimensionsMap = getTenderDimensionsWithIndicatorLastIteration(new HashSet<>(tenderIds), INDICATOR_CODE);

            String tenderIdsString = tenderIds.stream().collect(Collectors.joining(","));
            List<Object[]> awardWithDocumentTypesByTenderId = awardRepository.getActiveAwardWithDocumentTypesByTenderId(tenderIdsString);
            Map<String, Map<Integer, List<String>>> resultMap = new HashMap<>();
            Map<String, List<TenderIndicator>> result = new HashMap<>();

            awardWithDocumentTypesByTenderId.forEach(award -> {
                String tenderId = award[0].toString();

                log.info("Process tender {}", tenderId);

                String lotId = award[1].toString();
                List<String> documentFormats = isNull(award[2])
                        ? null
                        : Arrays.asList(award[2].toString().split(COMMA_SEPARATOR));
                Integer indicatorValue;
                if (nonNull(documentFormats)) {
                    indicatorValue = documentFormats.contains(PKCS7_SIGNATURE_FORMAT) ? NOT_RISK : RISK;
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
                    if (!result.containsKey(tenderOuterId)) result.put(tenderOuterId, new ArrayList<>());
                    result.get(tenderOuterId)
                            .add(new TenderIndicator(tenderDimensions, indicator, indicatorValue, lots));
                });
            });

            result.forEach((tenderId, tenderIndicators) -> {
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
}
