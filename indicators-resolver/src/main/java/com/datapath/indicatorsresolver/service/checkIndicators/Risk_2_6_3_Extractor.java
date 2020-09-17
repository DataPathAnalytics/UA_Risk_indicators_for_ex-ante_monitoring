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
public class Risk_2_6_3_Extractor extends BaseExtractor {

    private final static List<String> GENERAL_ENTITY_KINDS = Arrays.asList(
            "general",
            "authority",
            "central",
            "social"
    );

    private final static String SPECIAL_ENTITY_KIND = "special";

    private final String INDICATOR_CODE = "RISK2-6_3";
    private boolean indicatorsResolverAvailable;
    private final NearThresholdOneSupplierRepository nearThresholdOneSupplierRepository;

    public Risk_2_6_3_Extractor(NearThresholdOneSupplierRepository nearThresholdOneSupplierRepository) {
        this.nearThresholdOneSupplierRepository = nearThresholdOneSupplierRepository;
        indicatorsResolverAvailable = true;
    }

    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getActiveIndicator(INDICATOR_CODE);
            if (nonNull(indicator) && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                checkRisk2_6_3Indicator(indicator, dateTime);
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
                        || indicator.getLastCheckedDateCreated()
                        .isBefore(ZonedDateTime.now(ZoneId.of("UTC")).minus(Period.ofDays(2)).withHour(0))
                        ? ZonedDateTime.now(ZoneId.of("UTC")).minus(Period.ofDays(2)).withHour(0)
                        : indicator.getLastCheckedDateCreated();
                checkRisk2_6_3Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }

    private void checkRisk2_6_3Indicator(Indicator indicator, ZonedDateTime dateTime) {
        int size = 100;
        int page = 0;
        while (true) {

            List<Object[]> tenders = tenderRepository.findWorksProcuringEntityKindAndSupplierAmount(
                    dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind()), PageRequest.of(page, size));

            if (tenders.isEmpty()) {
                break;
            }

            Set<String> tenderIds = new HashSet<>();

            List<TenderIndicator> tenderIndicators = tenders.stream().map(tenderInfo -> {
                String tenderId = tenderInfo[0].toString();
                tenderIds.add(tenderId);
                String procuringEntity = tenderInfo[1].toString();
                String procuringEntityKind = tenderInfo[2].toString();
                List<String> suppliers = Arrays.asList(tenderInfo[3].toString().split(","));
                double amount = Double.parseDouble(tenderInfo[4].toString());


                TenderDimensions tenderDimensions = new TenderDimensions(tenderId);
                Integer indicatorValue = NOT_RISK;

                if (GENERAL_ENTITY_KINDS.contains(procuringEntityKind)) {
                    if (amount > 1350000 && amount < 1500000) {
                        indicatorValue = RISK;
                    }
                } else if (SPECIAL_ENTITY_KIND.equals(procuringEntityKind)) {
                    if (amount > 4500000 && amount < 5000000) {
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

                return new TenderIndicator(tenderDimensions, indicator, indicatorValue, new ArrayList<>());

            }).collect(Collectors.toList());

            Map<String, TenderDimensions> dimensionsMap = getTenderDimensionsWithIndicatorLastIteration(tenderIds, INDICATOR_CODE);

            tenderIndicators.forEach(tenderIndicator -> {
                tenderIndicator.setTenderDimensions(dimensionsMap.get(tenderIndicator.getTenderDimensions().getId()));
                uploadIndicator(tenderIndicator);
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
