package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.persistence.entities.Indicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;


@Service
@Slf4j
public class Risk_2_1_Extractor extends BaseExtractor {

    /*
    Порушення порядку визначення предмету закупівлі товарів та послуг
     */

    private final String INDICATOR_CODE = "RISK2-1";
    private boolean indicatorsResolverAvailable;


    public Risk_2_1_Extractor() {
        indicatorsResolverAvailable = true;
    }


    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getActiveIndicator(INDICATOR_CODE);
            if (nonNull(indicator) && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                checkRisk2_1Indicator(indicator, dateTime);
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
                        ? ZonedDateTime.now(ZoneId.of("UTC")).minus(Period.ofYears(1)).withHour(0)
                        : indicator.getLastCheckedDateCreated();
                checkRisk2_1Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }

    private void checkRisk2_1Indicator(Indicator indicator, ZonedDateTime dateTime) {
        int size = 100;
        int page = 0;
        while (true) {

            List<Object[]> tenders = tenderRepository.findTenderIdPWithPendingContractAndTwiceUnsuccessful(
                    dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind()),
                    PageRequest.of(page, size));

            if (tenders.isEmpty()) {
                break;
            }
            Set<String> tenderIds = new HashSet<>();

            List<TenderIndicator> tenderIndicators = tenders.stream().map(tenderInfo -> {
                String tenderId = tenderInfo[0].toString();
                tenderIds.add(tenderId);


                String cpv = tenderInfo[1].toString();
                String procuringEntity = tenderInfo[2].toString();
                Double amount = isNull(tenderInfo[3]) ? null : Double.parseDouble(tenderInfo[3].toString());
                Double quantity = isNull(tenderInfo[4]) ? null : Double.parseDouble(tenderInfo[4].toString());
                Integer pendingContracts = isNull(tenderInfo[5]) ? null : Integer.parseInt(tenderInfo[5].toString());

                TenderDimensions tenderDimensions = new TenderDimensions(tenderId);

                if (pendingContracts == 0) {
                    return new TenderIndicator(tenderDimensions, indicator, -2, new ArrayList<>());
                }
                Object quantityByProcuringEntityAndCPV = tenderItemRepository.getQuantityByProcuringEntityAndCPV(procuringEntity, cpv);
                Object[] lastTender = (Object[]) quantityByProcuringEntityAndCPV;
                Double lastAmount = isNull(lastTender[1]) ? null : Double.parseDouble(lastTender[1].toString());
                Double lastQuantity = isNull(lastTender[2]) ? null : Double.parseDouble(lastTender[2].toString());

                if (nonNull(amount) && nonNull(quantity) && nonNull(lastAmount) && nonNull(lastQuantity)) {

                    double amountDiffShare = (Math.abs(amount - lastAmount) / amount) * 100;
                    double quantityDiffShare = (Math.abs(quantity - lastQuantity) / quantity) * 100;
                    Integer indicatorValue = (amountDiffShare < 10 && quantityDiffShare > 10) ? 1 : 0;
                    return new TenderIndicator(tenderDimensions, indicator, indicatorValue, new ArrayList<>());
                }
                return null;

            }).filter(Objects::nonNull).collect(Collectors.toList());

            Map<String, TenderDimensions> dimensionsMap = getTenderDimensionsWithIndicatorLastIteration(tenderIds, INDICATOR_CODE);

            tenderIndicators.forEach(tenderIndicator -> {
                tenderIndicator.setTenderDimensions(dimensionsMap.get(tenderIndicator.getTenderDimensions().getId()));
                uploadIndicatorIfNotExists(tenderIndicator.getTenderDimensions().getId(), INDICATOR_CODE, tenderIndicator);
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
