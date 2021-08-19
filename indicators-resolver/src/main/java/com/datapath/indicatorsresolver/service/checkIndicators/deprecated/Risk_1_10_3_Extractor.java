package com.datapath.indicatorsresolver.service.checkIndicators.deprecated;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.indicatorsresolver.service.checkIndicators.BaseExtractor;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.entities.nbu.ExchangeRate;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.datapath.indicatorsresolver.IndicatorConstants.UA_ZONE;
import static com.datapath.persistence.utils.DateUtils.toZonedDateTime;
import static java.util.Objects.isNull;


@Slf4j
@Deprecated
public class Risk_1_10_3_Extractor extends BaseExtractor {

    private final String INDICATOR_CODE = "RISK1-10_3";

    private Map<String, Long> paKindAmountLimitMap = new HashMap<String, Long>() {{
        put("general", 200000L);
        put("authority", 200000L);
        put("central", 200000L);
        put("social", 200000L);
        put("special", 1000000L);
    }};

    private boolean indicatorsResolverAvailable;

    public Risk_1_10_3_Extractor() {
        indicatorsResolverAvailable = true;
    }

    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive() && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                checkRisk1_10_3Indicator(indicator, dateTime);
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
                        ? ZonedDateTime.now().minus(Period.ofYears(1)).withHour(0)
                        : indicator.getLastCheckedDateCreated();
                checkRisk1_10_3Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }


    private void checkRisk1_10_3Indicator(Indicator indicator, ZonedDateTime dateTime) {
        log.info("{} indicator started", INDICATOR_CODE);
        while (true) {

            List<Object[]> tenders = tenderRepository.findGoodsServicesTenderIdPAKindAndAmountExceptFinances(
                    dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind()));

            if (tenders.isEmpty()) {
                break;
            }
            Set<String> tenderIds = new HashSet<>();

            List<TenderIndicator> tenderIndicators = tenders.stream().map(tenderInfo -> {
                Integer indicatorValue = null;
                String tenderId = tenderInfo[0].toString();

                log.info("Process tender {}", tenderId);

                tenderIds.add(tenderId);

                Timestamp timestampStartDate = (Timestamp) tenderInfo[1];
                String kind = tenderInfo[2].toString();
                String currency = tenderInfo[3].toString();
                Double amount = Double.parseDouble(tenderInfo[4].toString());

                TenderDimensions tenderDimensions = new TenderDimensions(tenderId);

                if (!currency.equals(UAH_CURRENCY)) {
                    if (!isNull(timestampStartDate)) {
                        ZonedDateTime date = toZonedDateTime(timestampStartDate)
                                .withZoneSameInstant(UA_ZONE)
                                .withHour(0)
                                .withMinute(0)
                                .withSecond(0)
                                .withNano(0);
                        ExchangeRate firstByCodeAndDate = exchangeRateService.getOneByCodeAndDate(currency, date);
                        if (!isNull(firstByCodeAndDate)) {
                            amount *= firstByCodeAndDate.getRate();
                        } else {
                            indicatorValue = IMPOSSIBLE_TO_DETECT;
                        }
                    } else {
                        indicatorValue = IMPOSSIBLE_TO_DETECT;
                    }
                }
                if (isNull(indicatorValue)) {
                    indicatorValue = amount > paKindAmountLimitMap.get(kind) ? RISK : NOT_RISK;
                }
                return new TenderIndicator(tenderDimensions, indicator,
                        indicatorValue, new ArrayList<>());

            }).collect(Collectors.toList());

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

        log.info("{} indicator finished", INDICATOR_CODE);
    }

}
