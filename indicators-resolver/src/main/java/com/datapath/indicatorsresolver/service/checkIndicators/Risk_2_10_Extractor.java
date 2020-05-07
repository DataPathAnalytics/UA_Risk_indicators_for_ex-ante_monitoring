package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.druidintegration.model.DruidContractIndicator;
import com.datapath.indicatorsresolver.model.ContractDimensions;
import com.datapath.indicatorsresolver.model.ContractIndicator;
import com.datapath.persistence.entities.Indicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.datapath.persistence.utils.DateUtils.toZonedDateTime;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;


@Service
@Slf4j
public class Risk_2_10_Extractor extends BaseExtractor {

    /*
    Зміни до контракту внесені одразу після підписання
     */

    private final String INDICATOR_CODE = "RISK2-10";
    private boolean indicatorsResolverAvailable;

    public Risk_2_10_Extractor() {
        indicatorsResolverAvailable = true;
    }


    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getActiveIndicator(INDICATOR_CODE);
            if (nonNull(indicator) && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
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
            Indicator indicator = getActiveIndicator(INDICATOR_CODE);
            if (nonNull(indicator) && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
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
        int size = 100;
        int page = 0;
        while (true) {

            List<Object[]> tenders = tenderRepository.findTenderWithContractDateSignedAndMinChangeDateSigned(
                    dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind()),
                    PageRequest.of(page, size));

            if (tenders.isEmpty()) {
                break;
            }

            Set<String> contractIds = new HashSet<>();

            List<ContractIndicator> contractIndicators = tenders.stream().map(tenderInfo -> {
                String tenderId = tenderInfo[0].toString();
                String contractId = tenderInfo[1].toString();
                contractIds.add(contractId);
                try {
                    Timestamp contractDateSignedTimestamp = (Timestamp) tenderInfo[2];
                    Timestamp minChangeDateSignedTimestamp = (Timestamp) tenderInfo[3];
                    Integer contractChanges = Integer.parseInt(tenderInfo[4].toString());
                    ContractDimensions contractDimensions = new ContractDimensions(contractId);

                    if (contractChanges == 0) {
                        return new ContractIndicator(contractDimensions, indicator, -2, new ArrayList<>());
                    } else if (nonNull(contractDateSignedTimestamp) && nonNull(minChangeDateSignedTimestamp)) {

                        ZonedDateTime contractDateSigned = toZonedDateTime(contractDateSignedTimestamp)
                                .withZoneSameInstant(ZoneId.of("Europe/Kiev"))
                                .withHour(0)
                                .withMinute(0)
                                .withSecond(0)
                                .withNano(0);
                        ZonedDateTime changeDateSignedTimestamp = toZonedDateTime(minChangeDateSignedTimestamp)
                                .withZoneSameInstant(ZoneId.of("Europe/Kiev"))
                                .withHour(0)
                                .withMinute(0)
                                .withSecond(0)
                                .withNano(0);

                        Duration between = Duration.between(contractDateSigned, changeDateSignedTimestamp);
                        long days = between.toDays();
                        Integer indicatorValue = days < 30 ? RISK : NOT_RISK;

                        return new ContractIndicator(contractDimensions, indicator, indicatorValue, new ArrayList<>());
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
    }
}
