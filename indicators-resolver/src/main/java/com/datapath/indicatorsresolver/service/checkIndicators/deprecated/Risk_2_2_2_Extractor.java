package com.datapath.indicatorsresolver.service.checkIndicators.deprecated;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.indicatorsresolver.service.checkIndicators.BaseExtractor;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.repositories.derivatives.BiddersForBuyersRepository;
import lombok.extern.slf4j.Slf4j;

import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Slf4j
@Deprecated
public class Risk_2_2_2_Extractor extends BaseExtractor {

    private final String INDICATOR_CODE = "RISK2-2_2";
    private boolean indicatorsResolverAvailable;

    private BiddersForBuyersRepository biddersForBuyersRepository;

    public Risk_2_2_2_Extractor(BiddersForBuyersRepository biddersForBuyersRepository) {
        this.biddersForBuyersRepository = biddersForBuyersRepository;
        indicatorsResolverAvailable = true;
    }


    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive() && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                checkRisk2_2_2Indicator(indicator, dateTime);
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
                checkRisk2_2_2Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }

    private void checkRisk2_2_2Indicator(Indicator indicator, ZonedDateTime dateTime) {
        log.info("{} indicator started", INDICATOR_CODE);
        while (true) {

            List<String> tenders = findTenders(
                    dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind()));

            if (tenders.isEmpty()) {
                break;
            }

            Map<String, TenderDimensions> dimensionsMap = getTenderDimensionsWithIndicatorLastIteration(new HashSet<>(tenders), INDICATOR_CODE);

            String tendersStr = tenders.stream().collect(Collectors.joining(","));
            tenderRepository.getTenderBiddersWithPendingContracts(tendersStr)
                    .forEach(tenderObj -> {
                        String tenderId = tenderObj[0].toString();

                        log.info("Process tender {}", tenderId);

                        try {
                            String buyerId = tenderObj[1].toString();
                            Integer pendingContractsCount = Integer.parseInt(tenderObj[2].toString());
                            List<String> biddersId = isNull(tenderObj[3]) ? null : Arrays.asList(tenderObj[3].toString().split(","));

                            Integer indicatorValue = NOT_RISK;
                            if (pendingContractsCount == 0) {
                                indicatorValue = CONDITIONS_NOT_MET;
                            } else {
                                for (String supplier : biddersId) {
                                    List<String> procuringEntities = biddersForBuyersRepository.getProcuringEntitiesBySupplier(supplier);
                                    if (procuringEntities.size() == 1 && procuringEntities.contains(buyerId)) {
                                        indicatorValue = RISK;
                                        break;
                                    }
                                }
                            }

                            TenderDimensions tenderDimensions = dimensionsMap.get(tenderId);
                            TenderIndicator tenderIndicator = new TenderIndicator(tenderDimensions, indicator, indicatorValue, new ArrayList<>());
                            uploadIndicatorIfNotExists(tenderId, INDICATOR_CODE, tenderIndicator);
                        }catch (Exception ex) {
                            log.info(String.format(TENDER_INDICATOR_ERROR_MESSAGE, INDICATOR_CODE, tenderId));
                        }

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
