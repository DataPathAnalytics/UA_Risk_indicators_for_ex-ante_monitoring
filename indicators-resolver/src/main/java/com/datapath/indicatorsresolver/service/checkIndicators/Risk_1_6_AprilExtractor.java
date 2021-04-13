package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.druidintegration.model.DruidContractIndicator;
import com.datapath.indicatorsresolver.model.ContractDimensions;
import com.datapath.indicatorsresolver.model.ContractIndicator;
import com.datapath.persistence.entities.Contract;
import com.datapath.persistence.entities.ContractChange;
import com.datapath.persistence.entities.ContractDocument;
import com.datapath.persistence.entities.Indicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.datapath.indicatorsresolver.IndicatorConstants.UA_ZONE;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toSet;
import static org.springframework.util.CollectionUtils.isEmpty;


@Service
@Slf4j
public class Risk_1_6_AprilExtractor extends BaseExtractor {

    private final String INDICATOR_CODE = "RISK-1-6";
    private boolean indicatorsResolverAvailable;

    public Risk_1_6_AprilExtractor() {
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
    @Transactional
    @Scheduled(cron = "${risk-1-6.cron}")
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
                        ? ZonedDateTime.of(2020, 4, 17, 0, 0, 0, 0, UA_ZONE)
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

            List<Contract> contracts = contractRepository.findContracts(
                    dateTime,
                    asList(indicator.getProcedureStatuses()),
                    asList(indicator.getProcedureTypes()),
                    asList(indicator.getProcuringEntityKind()));

            if (contracts.isEmpty()) {
                break;
            }

            Set<String> contractIds = contracts.stream().map(Contract::getOuterId).collect(toSet());
            Map<String, ContractDimensions> dimensionsMap = getContractDimensionsWithIndicatorLastIteration(contractIds, INDICATOR_CODE);

            contracts.forEach(contract -> {

                ContractDimensions contractDimensions = dimensionsMap.get(contract.getOuterId());
                int indicatorValue;
                try {
                    indicatorValue = getContractIndicatorValue(contract);
                } catch (Exception e) {
                    logService.contractIndicatorFailed(INDICATOR_CODE, contract.getOuterId(), e);
                    indicatorValue = IMPOSSIBLE_TO_DETECT;
                }

                ContractIndicator contractIndicator = new ContractIndicator(contractDimensions, indicator, indicatorValue);
                DruidContractIndicator druidIndicators = druidIndicatorMapper.transformToDruidContractIndicator(contractIndicator);

                if (!extractContractDataService.theLastContractEquals(contract.getOuterId(), INDICATOR_CODE, singletonList(druidIndicators))) {
                    uploadDataService.uploadContractIndicator(druidIndicators);
                }
            });


            ZonedDateTime maxTenderDateCreated = getMaxContractDateCreated(dimensionsMap, dateTime);
            indicator.setLastCheckedDateCreated(maxTenderDateCreated);
            indicatorRepository.save(indicator);

            dateTime = maxTenderDateCreated;
        }

        indicator.setDateChecked(ZonedDateTime.now());
        indicatorRepository.save(indicator);

        log.info("{} indicator finished", INDICATOR_CODE);
    }

    private int getContractIndicatorValue(Contract contract) {
        if (isEmpty(contract.getChanges())) return CONDITIONS_NOT_MET;

        for (ContractChange change : contract.getChanges()) {
            if (isNull(change.getDateSigned())) return IMPOSSIBLE_TO_DETECT;
            Optional<ContractDocument> earlierDoc = change.getDocuments().stream().min(comparing(ContractDocument::getDatePublished));
            if (earlierDoc.isPresent()) {
                long daysBetween = Duration.between(
                        toUaMidnight(change.getDateSigned()),
                        toUaMidnight(ZonedDateTime.now())
                ).toDays();

                if (daysBetween > 3) return RISK;
            } else {

                long daysBetween = Duration.between(
                        toUaMidnight(change.getDateSigned()),
                        toUaMidnight(ZonedDateTime.now())
                ).toDays();
                if (daysBetween > 3) return RISK;
            }
        }

        return NOT_RISK;
    }
}
