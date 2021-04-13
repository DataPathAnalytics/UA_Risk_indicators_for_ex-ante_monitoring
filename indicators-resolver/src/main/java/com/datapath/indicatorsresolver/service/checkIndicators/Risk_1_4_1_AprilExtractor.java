package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.entities.nbu.ExchangeRate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static com.datapath.indicatorsresolver.IndicatorConstants.UA_ZONE;
import static com.datapath.persistence.utils.DateUtils.toZonedDateTime;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;


@Service
@Slf4j
public class Risk_1_4_1_AprilExtractor extends BaseExtractor {

    private final String INDICATOR_CODE = "RISK-1-4-1";
    private final Integer EUR_LIMIT = 5150000;
    private boolean indicatorsResolverAvailable;

    public Risk_1_4_1_AprilExtractor() {
        indicatorsResolverAvailable = true;
    }

    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive()) {
                checkIndicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }

    @Async
    @Scheduled(cron = "${risk-1-4-1.cron}")
    public void checkIndicator() {
        if (!indicatorsResolverAvailable) {
            log.info(String.format(INDICATOR_NOT_AVAILABLE_MESSAGE_FORMAT, INDICATOR_CODE));
            return;
        }
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive()) {
                ZonedDateTime dateTime = isNull(indicator.getLastCheckedDateCreated())
                        ? ZonedDateTime.now(ZoneId.of("UTC")).minus(Period.ofYears(1)).withHour(0)
                        : indicator.getLastCheckedDateCreated();
                checkIndicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }

    private void checkIndicator(Indicator indicator, ZonedDateTime dateTime) {
        log.info("{} indicator started", INDICATOR_CODE);
        while (true) {

            List<Object[]> tendersInfo = tenderRepository.getIndicator1_4_1TenderData(
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
                TenderDimensions tenderDimensions = new TenderDimensions(tenderId);

                log.info("Process tender {}", tenderId);

                Integer indicatorValue;
                try {
                    String currency = tenderData[1].toString();
                    Double amount = Double.parseDouble(tenderData[2].toString());
                    Timestamp timestampStartDate = (Timestamp) tenderData[3];
                    maxTenderDateCreated = toZonedDateTime((Timestamp) tenderData[4]);

                    tenders.add(tenderId);

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
                                    amount /= euroRate.getRate();
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
                } catch (Exception e) {
                    logService.tenderIndicatorFailed(INDICATOR_CODE, tenderId, e);
                    indicatorValue = IMPOSSIBLE_TO_DETECT;
                }

                tenderIndicators.add(new TenderIndicator(tenderDimensions, indicator, indicatorValue));
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
