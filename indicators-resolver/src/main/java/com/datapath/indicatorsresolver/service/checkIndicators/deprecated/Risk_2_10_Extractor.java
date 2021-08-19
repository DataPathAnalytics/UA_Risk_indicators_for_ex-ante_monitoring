package com.datapath.indicatorsresolver.service.checkIndicators.deprecated;

import com.datapath.druidintegration.model.DruidContractIndicator;
import com.datapath.indicatorsresolver.model.ContractDimensions;
import com.datapath.indicatorsresolver.model.ContractIndicator;
import com.datapath.indicatorsresolver.service.checkIndicators.BaseExtractor;
import com.datapath.persistence.entities.Indicator;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.datapath.indicatorsresolver.IndicatorConstants.UA_ZONE;
import static com.datapath.persistence.utils.DateUtils.toZonedDateTime;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@Deprecated
public class Risk_2_10_Extractor extends BaseExtractor {

    private final String INDICATOR_CODE = "RISK2-10";
    private boolean indicatorsResolverAvailable;

    public Risk_2_10_Extractor() {
        indicatorsResolverAvailable = true;
    }


    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive() && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                checkRisk2_10Indicator(indicator, dateTime);
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
                        ? ZonedDateTime.now(ZoneId.of("UTC")).minus(Period.ofDays(36)).withHour(0)
                        : indicator.getLastCheckedDateCreated();
                checkRisk2_10Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }

    private void checkRisk2_10Indicator(Indicator indicator, ZonedDateTime dateTime) {
        log.info("{} indicator started", INDICATOR_CODE);
        while (true) {

            List<Object[]> tenders = tenderRepository.findTenderWithContractDateSignedAndMinChangeDateSigned(
                    dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind()));

            if (tenders.isEmpty()) {
                break;
            }

            Set<String> contractIds = new HashSet<>();

            List<ContractIndicator> contractIndicators = tenders.stream().map(tenderInfo -> {
                String tenderId = tenderInfo[0].toString();

                log.info("Process tender {}", tenderId);

                String contractId = tenderInfo[1].toString();
                contractIds.add(contractId);
                try {
                    Timestamp contractDateSignedTimestamp = (Timestamp) tenderInfo[2];
                    Timestamp minChangeDateSignedTimestamp = (Timestamp) tenderInfo[3];
                    Integer contractChanges = Integer.parseInt(tenderInfo[4].toString());
                    ContractDimensions contractDimensions = new ContractDimensions(contractId);

                    if (contractChanges == 0) {
                        return new ContractIndicator(contractDimensions, indicator, CONDITIONS_NOT_MET);
                    } else if (nonNull(contractDateSignedTimestamp) && nonNull(minChangeDateSignedTimestamp)) {

                        ZonedDateTime contractDateSigned = toZonedDateTime(contractDateSignedTimestamp)
                                .withZoneSameInstant(UA_ZONE)
                                .withHour(0)
                                .withMinute(0)
                                .withSecond(0)
                                .withNano(0);
                        ZonedDateTime changeDateSignedTimestamp = toZonedDateTime(minChangeDateSignedTimestamp)
                                .withZoneSameInstant(UA_ZONE)
                                .withHour(0)
                                .withMinute(0)
                                .withSecond(0)
                                .withNano(0);

                        Duration between = Duration.between(contractDateSigned, changeDateSignedTimestamp);
                        long days = between.toDays();
                        Integer indicatorValue = days < 30 ? RISK : NOT_RISK;

                        return new ContractIndicator(contractDimensions, indicator, indicatorValue);
                    }

                    return null;
                } catch (Exception ex) {
                    log.info(String.format(TENDER_INDICATOR_ERROR_MESSAGE, INDICATOR_CODE, tenderId));
                    return null;
                }

            }).filter(Objects::nonNull).collect(Collectors.toList());


            Map<String, ContractDimensions> dimensionsMap = getContractDimensionsWithIndicatorLastIteration(contractIds, INDICATOR_CODE);
            contractIndicators.forEach(contractIndicator -> {
                String contractId = contractIndicator.getContractDimensions().getContractId();
                contractIndicator.setContractDimensions(dimensionsMap.get(contractIndicator.getContractDimensions().getContractId()));
                DruidContractIndicator druidIndicators = druidIndicatorMapper.transformToDruidContractIndicator(contractIndicator);

                if (!extractContractDataService.theLastContractEquals(contractId, INDICATOR_CODE, Collections.singletonList(druidIndicators))) {
                    log.info("UPDATES " + druidIndicators);
                    uploadDataService.uploadContractIndicator(druidIndicators);
                } else {
                    log.info("Previous equals current " + druidIndicators);
                }
            });

            ZonedDateTime maxTenderDateCreated = getMaxContractDateCreated(dimensionsMap, dateTime);
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
