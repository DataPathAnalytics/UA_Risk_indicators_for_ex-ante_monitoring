package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.entities.derivatives.NearThresholdOneSupplier;
import com.datapath.persistence.repositories.derivatives.NearThresholdOneSupplierRepository;
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
public class Risk_2_5_1_Extractor extends BaseExtractor {

    /*
    Повторна закупівля у одного Постачальника близька до порогу визначеного Законом („belowThreshold“, товари/послуги, 5% нижче 200К)
    */

    private final static List<String> GENERAL_ENTITY_KINDS = Arrays.asList(
            "general",
            "authority",
            "central",
            "social"
    );

    private final static String SPECIAL_ENTITY_KIND = "special";

    private final String INDICATOR_CODE = "RISK2-5_1";
    private boolean indicatorsResolverAvailable;
    private NearThresholdOneSupplierRepository nearThresholdOneSupplierRepository;

    public Risk_2_5_1_Extractor(NearThresholdOneSupplierRepository nearThresholdOneSupplierRepository) {
        this.nearThresholdOneSupplierRepository = nearThresholdOneSupplierRepository;
        indicatorsResolverAvailable = true;
    }

    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getActiveIndicator(INDICATOR_CODE);
            if (nonNull(indicator) && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                checkRisk2_5_1Indicator(indicator, dateTime);
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
                checkRisk2_5_1Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }

    private void checkRisk2_5_1Indicator(Indicator indicator, ZonedDateTime dateTime) {
        int size = 100;
        int page = 0;
        while (true) {

            List<String> tenders = tenderRepository.findGoodsServicesTenderIdByProcedureStatusAndProcedureType(
                    dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind()),
                    PageRequest.of(page, size));

            if (tenders.isEmpty()) {
                break;
            }

            Map<String, TenderDimensions> tenderDimensionsMap = getTenderDimensionsWithIndicatorLastIteration(new HashSet<>(tenders), INDICATOR_CODE);

            String tenderIdsStr = tenders.stream().collect(Collectors.joining(","));
            List<Object[]> tendersInfo = tenderRepository.getTenderIdPendingContractsCountProcuringEnrityIdKindSupplierAndAmount(tenderIdsStr);

            getTenderDimensionsWithIndicatorLastIteration(new HashSet<>(tenders), INDICATOR_CODE);

            tendersInfo.forEach(tenderInfo -> {
                String tenderId = tenderInfo[0].toString();
                log.info("Process tender with outer id [{}]", tenderId);
                int pendingContractsCount = Integer.parseInt(tenderInfo[1].toString());
                List<String> suppliers = isNull(tenderInfo[2])
                        ? null : Arrays.asList(tenderInfo[2].toString().split(","));
                Double amount = isNull(tenderInfo[3]) ? null : Double.parseDouble(tenderInfo[3].toString());
                String procuringEntity = tenderInfo[4].toString();
                String procuringEntityKind = tenderInfo[5].toString();

                TenderDimensions tenderDimensions = tenderDimensionsMap.get(tenderId);
                int indicatorValue = NOT_RISK;

                if (pendingContractsCount == 0) {
                    indicatorValue = -2;
                } else {

                    if (GENERAL_ENTITY_KINDS.contains(procuringEntityKind)) {
                        if (amount > 190000 && amount < 200000) {
                            indicatorValue = RISK;
                        }
                    } else if (SPECIAL_ENTITY_KIND.equals(procuringEntityKind)) {
                        if (amount > 950000 && amount < 1000000) {
                            indicatorValue = RISK;
                        }
                    }

                    if (indicatorValue == 1) {
                        Optional<NearThresholdOneSupplier> nearThreshold = nearThresholdOneSupplierRepository
                                .findFirstByProcuringEntityAndSupplierIn(procuringEntity, suppliers);
                        if (!nearThreshold.isPresent()) {
                            indicatorValue = NOT_RISK;
                        }
                    }
                }

                TenderIndicator tenderIndicator = new TenderIndicator(tenderDimensions, indicator, indicatorValue, new ArrayList<>());
                uploadIndicator(tenderIndicator);

            });

            ZonedDateTime maxTenderDateCreated = getMaxTenderDateCreated(tenderDimensionsMap, dateTime);
            indicator.setLastCheckedDateCreated(maxTenderDateCreated);
            indicatorRepository.save(indicator);

            dateTime = maxTenderDateCreated;
        }
        ZonedDateTime now = ZonedDateTime.now();
        indicator.setDateChecked(now);
        indicatorRepository.save(indicator);
    }
}
