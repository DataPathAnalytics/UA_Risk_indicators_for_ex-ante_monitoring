package com.datapath.indicatorsresolver.service.checkIndicators.deprecated;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.indicatorsresolver.service.checkIndicators.BaseExtractor;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.entities.nbu.ExchangeRate;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static com.datapath.indicatorsresolver.IndicatorConstants.UA_ZONE;
import static com.datapath.persistence.utils.DateUtils.toZonedDateTime;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;


@Slf4j
@Deprecated
public class Risk_1_2_1_Extractor extends BaseExtractor {

    private final String INDICATOR_CODE = "RISK1-2_1";
    private final Integer EUR_LIMIT = 133000;
    private boolean indicatorsResolverAvailable;

    public Risk_1_2_1_Extractor() {
        indicatorsResolverAvailable = true;
    }

    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive() && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                checkRisk_1_2_1Indicator(indicator, dateTime);
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
                checkRisk_1_2_1Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }

    private void checkRisk_1_2_1Indicator(Indicator indicator, ZonedDateTime dateTime) {
        log.info("{} indicator started", INDICATOR_CODE);
        while (true) {
            List<Object[]> tendersInfo = tenderRepository
                    .findGoodsServicesTenderIdCurrencyAmountByProcedureStatusAndProcedureType(
                            dateTime,
                            Arrays.asList(indicator.getProcedureStatuses()),
                            Arrays.asList(indicator.getProcedureTypes()),
                            Arrays.asList(indicator.getProcuringEntityKind()));
            if (tendersInfo.isEmpty()) {
                break;
            }
            Set<String> tenders = new HashSet<>();

            ZonedDateTime maxTenderDateCreated = ZonedDateTime.now();

            List<TenderIndicator> tenderIndicators = new ArrayList<>();

            for (Object[] tenderData : tendersInfo) {
                String tenderId = tenderData[0].toString();

                log.info("Process tender {}", tenderId);

                String currency = tenderData[1].toString();
                Double amount = Double.parseDouble(tenderData[2].toString());
                Timestamp timestampStartDate = (Timestamp) tenderData[3];
                maxTenderDateCreated = toZonedDateTime((Timestamp) tenderData[4]);

                tenders.add(tenderId);

                Integer indicatorValue;
                if (currency.equals(EUR_CURRENCY)) {
                    indicatorValue = amount > EUR_LIMIT ? RISK : NOT_RISK;
                } else {
                    if (isNull(timestampStartDate)) {
                        indicatorValue = IMPOSSIBLE_TO_DETECT;
                    } else {
                        ZonedDateTime zonedDateTime = toZonedDateTime(timestampStartDate)
                                .withZoneSameInstant(UA_ZONE)
                                .withHour(0)
                                .withMinute(0)
                                .withSecond(0)
                                .withNano(0);
                        if (currency.equals(UAH_CURRENCY)) {
                            ExchangeRate euroRate = exchangeRateService.getOneByCodeAndDate(EUR_CURRENCY, zonedDateTime);
                            if (nonNull(euroRate)) {
                                indicatorValue = amount / euroRate.getRate() > EUR_LIMIT ? RISK : NOT_RISK;
                            } else {
                                indicatorValue = IMPOSSIBLE_TO_DETECT;
                            }
                        } else {
                            ExchangeRate currencyRate = exchangeRateService.getOneByCodeAndDate(currency, zonedDateTime);
                            ExchangeRate euroRate = exchangeRateService.getOneByCodeAndDate(EUR_CURRENCY, zonedDateTime);
                            if (nonNull(currencyRate) && nonNull(euroRate)) {
                                amount = amount * currencyRate.getRate() / euroRate.getRate();
                                indicatorValue = amount > EUR_LIMIT ? RISK : NOT_RISK;
                            } else {
                                indicatorValue = IMPOSSIBLE_TO_DETECT;
                            }
                        }
                    }
                }

                TenderDimensions tenderDimensions = new TenderDimensions(tenderId);
                tenderIndicators.add(new TenderIndicator(tenderDimensions, indicator, indicatorValue, new ArrayList<>()));
            }

            Map<String, TenderDimensions> dimensionsMap = getTenderDimensionsWithIndicatorLastIteration(tenders, INDICATOR_CODE);

            tenderIndicators.forEach(tenderIndicator -> {
                tenderIndicator.setTenderDimensions(dimensionsMap.get(tenderIndicator.getTenderDimensions().getId()));
                uploadIndicator(tenderIndicator);
            });

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
