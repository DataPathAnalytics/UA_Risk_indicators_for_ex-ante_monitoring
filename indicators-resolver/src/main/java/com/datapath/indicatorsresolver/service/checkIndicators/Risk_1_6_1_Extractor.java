package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.persistence.entities.Indicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Period;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;


@Service
@Slf4j
public class Risk_1_6_1_Extractor extends BaseExtractor {

    /*
    Присутній електронний підпис, але документи договору відсутні
    */

    private final String INDICATOR_CODE = "RISK1-6";
    private final String PKCS7_SIGNATURE_FORMAT = "application/pkcs7-signature";

    private boolean indicatorsResolverAvailable;

    public Risk_1_6_1_Extractor() {
        indicatorsResolverAvailable = true;
    }

    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getActiveIndicator(INDICATOR_CODE);
            if (nonNull(indicator) && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                checkRisk_1_6Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error(ex.getMessage());
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
                        ? ZonedDateTime.now().minus(Period.ofYears(1)).withHour(0)
                        : indicator.getLastCheckedDateCreated();
                checkRisk_1_6Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error(ex.getMessage());
        } finally {
            indicatorsResolverAvailable = true;
        }
    }


    private void checkRisk_1_6Indicator(Indicator indicator, ZonedDateTime dateTime) {
        int size = 100;
        int page = 0;

        while (true) {

            List<String> tenders = findTenders(
                    dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind()),
                    page, size);

            log.info("{} Tenders size: {}", INDICATOR_CODE, tenders.size());

            if (tenders.isEmpty()) {
                break;
            }

            Map<String, TenderDimensions> dimensionsMap = getTenderDimensionsWithIndicatorLastIteration(new HashSet<>(tenders), INDICATOR_CODE);

            Map<String, List<TenderIndicator>> tenderIndicatorsMap = checkIndicator(tenders, indicator);

            tenderIndicatorsMap.forEach((tenderId, tenderIndicators) -> {
                tenderIndicators.forEach(tenderIndicator -> tenderIndicator.setTenderDimensions(dimensionsMap.get(tenderId)));
                uploadIndicatorIfNotExists(tenderId, INDICATOR_CODE, tenderIndicators);
            });

            ZonedDateTime maxTenderDateCreated = getMaxTenderDateCreated(dimensionsMap, dateTime);
            indicator.setLastCheckedDateCreated(maxTenderDateCreated);
            indicatorRepository.save(indicator);

            dateTime = maxTenderDateCreated;
        }

        ZonedDateTime now = ZonedDateTime.now();
        indicator.setDateChecked(now);
        indicatorRepository.save(indicator);
    }

    private Map<String, List<TenderIndicator>> checkIndicator(List<String> tenderIds, Indicator indicator) {
        try {
            Map<String, Map<Integer, List<String>>> resultMap = new HashMap<>();
            Map<String, List<TenderIndicator>> result = new HashMap<>();

            List<Object[]> tendersContractsWithDocuments = tenderContractRepository
                    .getTenderLotContractDocsByTenderId(String.join(",", tenderIds));

            for (Object[] tendersContractsWithDocument : tendersContractsWithDocuments) {
                String tenderId = tendersContractsWithDocument[0].toString();
                String lotId = tendersContractsWithDocument[1].toString();
                String contractId = isNull(tendersContractsWithDocument[2])
                        ? null
                        : tendersContractsWithDocument[2].toString();
                List<String> tenderDocsFormats = isNull(tendersContractsWithDocument[3])
                        ? null
                        : Arrays.asList(tendersContractsWithDocument[3].toString().split(COMMA_SEPARATOR));
                List<String> contractDocsFormats = isNull(tendersContractsWithDocument[4])
                        ? null
                        : Arrays.asList(tendersContractsWithDocument[4].toString().split(COMMA_SEPARATOR));
                int indicatorValue;
                if (isNull(contractId)) {
                    indicatorValue = -2;
                } else {
                    if (nonNull(tenderDocsFormats)) {
                        if (!tenderDocsFormats.stream()
                                .filter(docFormat -> !docFormat.equals("application/pkcs7-signature"))
                                .collect(Collectors.toList()).isEmpty()) {
                            indicatorValue = 0;
                        } else {
                            if (isNull(contractDocsFormats) || contractDocsFormats.stream()
                                    .filter(docFormat -> !docFormat.equals("application/pkcs7-signature"))
                                    .collect(Collectors.toList()).isEmpty()) {
                                indicatorValue = 1;
                            } else {
                                indicatorValue = 0;
                            }
                        }
                    } else {
                        if (isNull(contractDocsFormats)) {
                            indicatorValue = -2;
                        } else {
                            if (contractDocsFormats.stream()
                                    .filter(docFormat -> !docFormat.equals("application/pkcs7-signature"))
                                    .collect(Collectors.toList()).isEmpty()) {
                                indicatorValue = 1;
                            } else {
                                indicatorValue = 0;
                            }
                        }
                    }

                }
                if (!resultMap.containsKey(tenderId)) {
                    resultMap.put(tenderId, new HashMap<>());
                }
                if (!resultMap.get(tenderId).containsKey(indicatorValue)) {
                    resultMap.get(tenderId).put(indicatorValue, new ArrayList<>());
                }
                resultMap.get(tenderId).get(indicatorValue).add(lotId);
            }
            resultMap.forEach((tenderOuterId, value) -> {
                TenderDimensions tenderDimensions = new TenderDimensions(tenderOuterId);
                value.forEach((indicatorValue, lots) -> {
                    if (!result.containsKey(tenderOuterId)) result.put(tenderOuterId, new ArrayList<>());
                    result.get(tenderOuterId).add(new TenderIndicator(tenderDimensions, indicator, indicatorValue, lots));
                });
            });

            return result;
        } catch (Exception ex) {
            log.info(String.format("ERROR while processing indicator: %s ", INDICATOR_CODE));
            ex.printStackTrace();
            return new HashMap<>();
        }
    }

}
