package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.entities.derivatives.NearThreshold;
import com.datapath.persistence.repositories.derivatives.NearThresholdRepository;
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
public class Risk_2_5_2_Extractor extends BaseExtractor {

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
            Indicator indicator = getActiveIndicator(INDICATOR_CODE);
            if (nonNull(indicator) && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                checkRisk2_5_2Indicator(indicator, dateTime);
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
            Indicator indicator = getActiveIndicator(INDICATOR_CODE);
            if (nonNull(indicator) && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                ZonedDateTime dateTime = isNull(indicator.getLastCheckedDateCreated())
                        || indicator.getLastCheckedDateCreated()
                        .isBefore(ZonedDateTime.now(ZoneId.of("UTC")).minus(Period.ofDays(2)).withHour(0))
                        ? ZonedDateTime.now(ZoneId.of("UTC")).minus(Period.ofDays(2)).withHour(0)
                        : indicator.getLastCheckedDateCreated();
                checkRisk2_5_2Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error(ex.getMessage());
        } finally {
            indicatorsResolverAvailable = true;
        }
    }

    private void checkRisk2_5_2Indicator(Indicator indicator, ZonedDateTime dateTime) {
        int size = 100;
        int page = 0;
        while (true) {

            List<Object[]> tenders = tenderRepository.findGoodsServicesProcuringEntityKindAmount(
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
                try {
                    String procuringEntity = tenderInfo[1].toString();
                    String procuringEntityKind = tenderInfo[2].toString();
                    Double amount = isNull(tenderInfo[3]) ? null : Double.parseDouble(tenderInfo[3].toString());
                    String cpv = tenderInfo[4].toString();


                    TenderDimensions tenderDimensions = new TenderDimensions(tenderId);
                    Integer indicatorValue = NOT_RISK;
                    if (isNull(amount)) {
                        indicatorValue = -1;
                    } else {
                        Optional<NearThreshold> nearThreshold = nearThresholdRepository.findFirstByProcuringEntityAndCpv(procuringEntity, cpv);
                        if (!nearThreshold.isPresent()){
                            indicatorValue = NOT_RISK;
                        } else {
                            amount += nearThreshold.get().getAmount();
                            switch (procuringEntityKind) {
                                case "general":
                                    if (amount >= 200000) {
                                        indicatorValue = RISK;
                                    }
                                    break;
                                case "special":
                                    if (amount >= 1000000 ) {
                                        indicatorValue = RISK;
                                    }
                                    break;
                            }
                        }
                    }

                    return new TenderIndicator(tenderDimensions, indicator, indicatorValue, new ArrayList<>());
                } catch (Exception ex) {
                    log.info(String.format(TENDER_INDICATOR_ERROR_MESSAGE, INDICATOR_CODE, tenderId));
                    return null;
                }
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
