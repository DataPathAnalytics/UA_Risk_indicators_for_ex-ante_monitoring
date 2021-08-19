package com.datapath.indicatorsresolver.service.checkIndicators.deprecated;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.indicatorsresolver.service.checkIndicators.BaseExtractor;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.repositories.derivatives.SuppliersSingleBuyerRepository;
import lombok.extern.slf4j.Slf4j;

import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Slf4j
@Deprecated
public class Risk_2_2_Extractor extends BaseExtractor {

    private final String INDICATOR_CODE = "RISK2-2";
    private boolean indicatorsResolverAvailable;

    private SuppliersSingleBuyerRepository suppliersSingleBuyerRepository;

    public Risk_2_2_Extractor(SuppliersSingleBuyerRepository suppliersSingleBuyerRepository) {
        this.suppliersSingleBuyerRepository = suppliersSingleBuyerRepository;
        indicatorsResolverAvailable = true;
    }


    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive() && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                checkRiskRisk_2_2Indicator(indicator, dateTime);
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
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive() && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                ZonedDateTime dateTime = isNull(indicator.getLastCheckedDateCreated())
                        ? ZonedDateTime.now(ZoneId.of("UTC")).minus(Period.ofYears(1)).withHour(0)
                        : indicator.getLastCheckedDateCreated();
                checkRiskRisk_2_2Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error(ex.getMessage());
        } finally {
            indicatorsResolverAvailable = true;
        }
    }

    private void checkRiskRisk_2_2Indicator(Indicator indicator, ZonedDateTime dateTime) {
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

            Map<String, List<TenderIndicator>> result = new HashMap<>();
            Map<String, Map<Integer, List<String>>> tenderIndicatorResult = new HashMap<>();
            tenderRepository.getTenderLotBuyerSupplier(tenders.stream().collect(Collectors.joining(",")))
                    .forEach(tenderObj -> {
                        String tenderId = tenderObj[0].toString();

                        log.info("Process tender {}", tenderId);

                        try {
                            String lotId = tenderObj[1].toString();
                            String buyerId = isNull(tenderObj[2]) ? null : tenderObj[2].toString();
                            String supplierId = isNull(tenderObj[3]) ? null : tenderObj[3].toString();
                            Integer indicatorValue;
                            if (isNull(buyerId) && isNull(supplierId)) {
                                indicatorValue = CONDITIONS_NOT_MET;
                            } else {
                                List<String> buyersSuppliers = suppliersSingleBuyerRepository.getBuyersSuppliers(buyerId, supplierId);
                                indicatorValue = buyersSuppliers.isEmpty() ? NOT_RISK : RISK;
                            }

                            if (!tenderIndicatorResult.containsKey(tenderId)) {
                                tenderIndicatorResult.put(tenderId, new HashMap<>());
                            }
                            if (!tenderIndicatorResult.get(tenderId).containsKey(indicatorValue)) {
                                tenderIndicatorResult.get(tenderId).put(indicatorValue, new ArrayList<>());
                            }

                            tenderIndicatorResult.get(tenderId).get(indicatorValue).add(lotId);
                        }catch (Exception ex) {
                            log.error(ex.getMessage() ,ex);
                            log.info(String.format(TENDER_INDICATOR_ERROR_MESSAGE, INDICATOR_CODE, tenderId));
                        }
                    });

            tenderIndicatorResult.forEach((tenderOuterId, value) -> {
                TenderDimensions tenderDimensions = new TenderDimensions(tenderOuterId);
                value.forEach((indicatorValue, lots) -> {
                    if (!result.containsKey(tenderOuterId)) {
                        result.put(tenderOuterId, new ArrayList<>());
                    }
                    result.get(tenderOuterId).add(new TenderIndicator(tenderDimensions, indicator, indicatorValue, lots));
                });
            });

            result.forEach((tenderId, tenderIndicators) -> {
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
}
