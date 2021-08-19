package com.datapath.indicatorsresolver.service.checkIndicators.deprecated;

import com.datapath.druidintegration.model.DruidContractIndicator;
import com.datapath.indicatorsresolver.model.ContractDimensions;
import com.datapath.indicatorsresolver.model.ContractIndicator;
import com.datapath.indicatorsresolver.service.checkIndicators.BaseExtractor;
import com.datapath.persistence.entities.Indicator;
import lombok.extern.slf4j.Slf4j;

import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Slf4j
@Deprecated
public class Risk_2_11_Extractor extends BaseExtractor {

    private final String INDICATOR_CODE = "RISK2-11";
    private boolean indicatorsResolverAvailable;

    public Risk_2_11_Extractor() {
        indicatorsResolverAvailable = true;
    }


    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive() && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                checkRisk2_11Indicator(indicator, dateTime);
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
                        ? ZonedDateTime.now(ZoneId.of("UTC")).minus(Period.ofYears(1)).withHour(0)
                        : indicator.getLastCheckedDateCreated();
                checkRisk2_11Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }

    private void checkRisk2_11Indicator(Indicator indicator, ZonedDateTime dateTime) {
        log.info("{} indicator started", INDICATOR_CODE);
        while (true) {

            List<String> contracts = contractRepository.getContarctIdByTenderSratusAndProcedureTypeAndProcuringEntityType(
                    dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind()));

            if (contracts.isEmpty()) {
                break;
            }

            Map<String, ContractDimensions> contractDimensionsMap = getContractDimensionsWithIndicatorLastIteration(new HashSet<>(contracts), INDICATOR_CODE);

            List<Object[]> activeChangesWithItemPriceVariationByTenderId = contractChangeRepository
                    .getActiveChangesWithItemPriceVariationByTenderIds(contracts.stream().collect(Collectors.joining(",")));

            activeChangesWithItemPriceVariationByTenderId.forEach(contractInfo -> {
                String contractId = contractInfo[0].toString();

                log.info("Process contract {}", contractId);

                try {
                    Integer changes = Integer.parseInt(contractInfo[1].toString());

                    Integer indicatorValue;

                    if (changes == 0) {
                        indicatorValue = CONDITIONS_NOT_MET;
                    } else {
                        indicatorValue = changes >= 3 ? RISK : NOT_RISK;
                    }
                    ContractDimensions contractDimensions = contractDimensionsMap.get(contractId);
                    ContractIndicator contractIndicator = new ContractIndicator(contractDimensions, indicator, indicatorValue);
                    DruidContractIndicator druidIndicator = druidIndicatorMapper.transformToDruidContractIndicator(contractIndicator);

                    if (!extractContractDataService.theLastContractEquals(contractId, INDICATOR_CODE, Collections.singletonList(druidIndicator))) {
                        log.info("UPDATES " + druidIndicator);
                        uploadDataService.uploadContractIndicator(druidIndicator);
                    } else {
                        log.info("Previous equals current " + druidIndicator);
                    }

                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                    log.info(String.format(TENDER_INDICATOR_ERROR_MESSAGE, INDICATOR_CODE, contractId));
                }
            });


            ZonedDateTime maxTenderDateCreated = getMaxContractDateCreated(contractDimensionsMap, dateTime);
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
