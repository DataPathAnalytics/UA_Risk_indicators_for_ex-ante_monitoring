package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.persistence.entities.Indicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;


@Service
@Slf4j
public class Risk_2_17_2Extractor extends BaseExtractor {

    /*
   «Учасника з найбільш економічно вигідною пропозицією замовником відхиллено, а переможцем визначено учасника-ФОП,
    з яким такий замовник попередньо укладав договори більше ніж по 3-м різним групам CPV».
    */

    private final String INDICATOR_CODE = "RISK2-17_2";

    private boolean indicatorsResolverAvailable;

    public Risk_2_17_2Extractor() {
        indicatorsResolverAvailable = true;
    }

    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getActiveIndicator(INDICATOR_CODE);
            if (nonNull(indicator) && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                checkRisk_2_17_2Indicator(indicator, dateTime);
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
                ZonedDateTime dateTime = isNull(indicator.getDateChecked())
                        ? ZonedDateTime.now(ZoneId.of("UTC")).minus(Period.ofYears(1)).withHour(0)
                        : indicator.getLastCheckedDateCreated();
                checkRisk_2_17_2Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }


    private void checkRisk_2_17_2Indicator(Indicator indicator, ZonedDateTime dateTime) {
        int size = 100;
        int page = 0;

        while (true) {

            List<String> tenders = findTenders(
                    dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind()),
                    page, size);

            if (tenders.isEmpty()) {
                break;
            }

            Set<String> tenderIds = new HashSet<>(tenders);

            Map<String, TenderDimensions> dimensionsMap = getTenderDimensionsWithIndicatorLastIteration(tenderIds, INDICATOR_CODE);


            List<Object[]> tendersWithLotUnsuccessfulAwardsSupplierAndCpvCount =
                    tenderRepository.findTendersWithLotUnsuccessfulAwardsSupplierAndCpvCount(String.join(",", tenders));

            Map<String, Map<Integer, List<String>>> resultMap = new HashMap<>();
            Map<String, List<TenderIndicator>> result = new HashMap<>();

            tendersWithLotUnsuccessfulAwardsSupplierAndCpvCount.forEach(tenderItem -> {
                String tenderId = tenderItem[0].toString();
                String lotId = tenderItem[1].toString();
                int unsuccessfulAwards = Integer.parseInt(tenderItem[2].toString());
                String supplierScheme = isNull(tenderItem[3]) ? null : tenderItem[3].toString();
                String supplierId = isNull(tenderItem[4]) ? null : tenderItem[4].toString();
                Integer cpvCount = isNull(tenderItem[5]) ? null : Integer.parseInt(tenderItem[5].toString());

                int indicatorValue;
                if (unsuccessfulAwards == 0 || isNull(supplierId)) indicatorValue = -2;
                else if (!supplierScheme.equals("UA-EDR") || supplierId.length() != 10) indicatorValue = 0;
                else indicatorValue = isNull(cpvCount) || cpvCount < 3 ? 0 : 1;

                if (!resultMap.containsKey(tenderId)) {
                    resultMap.put(tenderId, new HashMap<>());
                }
                if (!resultMap.get(tenderId).containsKey(indicatorValue)) {
                    resultMap.get(tenderId).put(indicatorValue, new ArrayList<>());
                }

                resultMap.get(tenderId).get(indicatorValue).add(lotId);
            });


            resultMap.forEach((tenderOuterId, value) -> {
                TenderDimensions tenderDimensions = dimensionsMap.get(tenderOuterId);
                value.forEach((indicatorValue, lots) -> {
                    if (!result.containsKey(tenderOuterId)) result.put(tenderOuterId, new ArrayList<>());
                    result.get(tenderOuterId).add(new TenderIndicator(tenderDimensions, indicator, indicatorValue, lots));
                });
            });

            result.forEach((tenderId, tenderIndicators) -> {
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

    }

}