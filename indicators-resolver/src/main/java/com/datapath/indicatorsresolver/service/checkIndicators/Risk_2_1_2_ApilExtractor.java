package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.persistence.entities.Indicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Period;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;


@Service
@Slf4j
public class Risk_2_1_2_ApilExtractor extends BaseExtractor {

    private static final String INDICATOR_CODE = "RISK-2-1-2";
    private static final String PKCS7_SIGNATURE_FORMAT = "application/pkcs7-signature";
    private boolean indicatorsResolverAvailable;


    public Risk_2_1_2_ApilExtractor() {
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
    @Scheduled(cron = "${risk-2-1-2.cron}")
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

        log.info("{} indicator finished", INDICATOR_CODE);
    }

    private Map<String, List<TenderIndicator>> checkIndicator(List<String> tenderIds, Indicator indicator) {
        Map<String, Map<Integer, List<String>>> resultMap = new HashMap<>();
        Map<String, List<TenderIndicator>> result = new HashMap<>();

        String tenderIdsStr = tenderIds.stream().collect(Collectors.joining(","));
        List<Object> tendersContractsWithDocuments = tenderContractRepository
                .getNonCancelledContractsAndDocumentTypesByTenderId(tenderIdsStr);

        for (Object tendersContractsWithDocument : tendersContractsWithDocuments) {
            Object[] o = (Object[]) tendersContractsWithDocument;
            String tenderId = o[0].toString();

            log.info("Process tender {}", tenderId);

            String lotId = o[1].toString();
            String contractId = isNull(o[2]) ? null : o[2].toString();
            List<String> documentFormats = isNull(o[3]) ? null : Arrays.asList(o[3].toString().split(COMMA_SEPARATOR));
            int indicatorValue;
            if (isNull(contractId) || isNull(documentFormats)) {
                indicatorValue = CONDITIONS_NOT_MET;
            } else {
                indicatorValue = documentFormats.contains(PKCS7_SIGNATURE_FORMAT) ? NOT_RISK : RISK;
                if (indicatorValue == RISK) {
                    List<String> contractDocumentFormat = contractDocumentRepository.getFormatByContractOuterId(contractId);
                    if (contractDocumentFormat.contains(PKCS7_SIGNATURE_FORMAT)) {
                        indicatorValue = NOT_RISK;
                    }
                }
            }
            if (!resultMap.containsKey(tenderId)) {
                resultMap.put(tenderId, new HashMap<>());
            }
            if (!resultMap.get(tenderId).containsKey(indicatorValue)) {
                resultMap.get(tenderId).put(indicatorValue, new ArrayList<>());
            }

            addPriorityLotIndicatorValue(indicatorValue, lotId, resultMap.get(tenderId));
        }

        resultMap.forEach((tenderOuterId, value) -> {
            TenderDimensions tenderDimensions = new TenderDimensions(tenderOuterId);
            value.forEach((indicatorValue, lots) -> {
                if (!result.containsKey(tenderOuterId)) result.put(tenderOuterId, new ArrayList<>());
                result.get(tenderOuterId).add(new TenderIndicator(tenderDimensions, indicator, indicatorValue, lots));
            });
        });

        return result;
    }
}
