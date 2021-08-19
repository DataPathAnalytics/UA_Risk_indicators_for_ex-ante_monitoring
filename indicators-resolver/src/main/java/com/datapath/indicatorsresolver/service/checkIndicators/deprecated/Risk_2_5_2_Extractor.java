package com.datapath.indicatorsresolver.service.checkIndicators.deprecated;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.indicatorsresolver.service.checkIndicators.BaseExtractor;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.entities.derivatives.NearThreshold;
import com.datapath.persistence.repositories.derivatives.NearThresholdRepository;
import lombok.extern.slf4j.Slf4j;

import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

@Slf4j
@Deprecated
public class Risk_2_5_2_Extractor extends BaseExtractor {

    private final static List<String> GENERAL_ENTITY_KINDS = Arrays.asList(
            "general",
            "authority",
            "central",
            "social"
    );

    private final static String SPECIAL_ENTITY_KIND = "special";

    private final String INDICATOR_CODE = "RISK2-5_2";
    private boolean indicatorsResolverAvailable;
    private NearThresholdRepository nearThresholdRepository;

    public Risk_2_5_2_Extractor(NearThresholdRepository nearThresholdRepository) {
        this.nearThresholdRepository = nearThresholdRepository;
        indicatorsResolverAvailable = true;
    }


    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive() && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                checkRisk2_5_2Indicator(indicator, dateTime);
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
                        || indicator.getLastCheckedDateCreated()
                        .isBefore(ZonedDateTime.now(ZoneId.of("UTC")).minus(Period.ofDays(2)).withHour(0))
                        ? ZonedDateTime.now(ZoneId.of("UTC")).minus(Period.ofDays(2)).withHour(0)
                        : indicator.getLastCheckedDateCreated();
                checkRisk2_5_2Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }

    private void checkRisk2_5_2Indicator(Indicator indicator, ZonedDateTime dateTime) {
        log.info("{} indicator started", INDICATOR_CODE);
        while (true) {

            List<Object[]> tenders = tenderRepository.findGoodsServicesProcuringEntityKindAmount(
                    dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind()));

            if (tenders.isEmpty()) {
                break;
            }
            Set<String> tenderIds = new HashSet<>();

            List<TenderIndicator> tenderIndicators = tenders.stream().map(tenderInfo -> {
                String tenderId = tenderInfo[0].toString();

                log.info("Process tender {}", tenderId);

                tenderIds.add(tenderId);
                String procuringEntity = tenderInfo[1].toString();
                String procuringEntityKind = tenderInfo[2].toString();
                Double amount = isNull(tenderInfo[3]) ? null : Double.parseDouble(tenderInfo[3].toString());
                String cpv = tenderInfo[4].toString();
                log.info("Extract data from tender [{}[", tenderId);

                TenderDimensions tenderDimensions = new TenderDimensions(tenderId);
                Integer indicatorValue = NOT_RISK;
                if (isNull(amount)) {
                    indicatorValue = IMPOSSIBLE_TO_DETECT;
                } else {
                    Optional<NearThreshold> nearThreshold = nearThresholdRepository.findFirstByProcuringEntityAndCpv(procuringEntity, cpv);
                    if (nearThreshold.isPresent()) {
                        amount += nearThreshold.get().getAmount();
                        if (GENERAL_ENTITY_KINDS.contains(procuringEntityKind)) {
                            if (amount >= 200000) {
                                indicatorValue = RISK;
                            }
                        } else if (SPECIAL_ENTITY_KIND.equals(procuringEntityKind)) {
                            if (amount >= 1000000) {
                                indicatorValue = RISK;
                            }
                        }
                    }
                }
                return new TenderIndicator(tenderDimensions, indicator, indicatorValue, new ArrayList<>());
            }).collect(toList());

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

        log.info("{} indicator finished", INDICATOR_CODE);
    }
}
