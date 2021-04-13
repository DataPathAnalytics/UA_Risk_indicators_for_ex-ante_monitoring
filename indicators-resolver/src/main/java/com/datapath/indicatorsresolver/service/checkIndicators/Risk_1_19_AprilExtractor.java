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
import java.time.ZonedDateTime;
import java.util.*;

import static com.datapath.persistence.utils.DateUtils.toZonedDateTime;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;


@Service
@Slf4j
public class Risk_1_19_AprilExtractor extends BaseExtractor {

    private final String INDICATOR_CODE = "RISK-1-19";
    private final static int AMOUNT_LIMIT = 50000;
    private boolean indicatorsResolverAvailable;

    public Risk_1_19_AprilExtractor() {
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
    @Scheduled(cron = "${risk-1-19.cron}")
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
                        ? ZonedDateTime.now().minus(Period.ofDays(2)).withHour(0)
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

                String tenderId = tenderInfo[0].toString();
                TenderDimensions tenderDimensions = new TenderDimensions(tenderId);
                Integer indicatorValue = null;

                log.info("Process tender {}", tenderId);

                try {
                    tenderIds.add(tenderId);

                    Timestamp timestampStartDate = isNull(tenderInfo[1]) ?
                            (Timestamp) tenderInfo[5] :
                            (Timestamp) tenderInfo[1];

                    String currency = tenderInfo[3].toString();
                    Double amount = Double.parseDouble(tenderInfo[4].toString());

                    if (!currency.equals(UAH_CURRENCY)) {
                        if (!isNull(timestampStartDate)) {
                            ZonedDateTime date = toUaMidnight(toZonedDateTime(timestampStartDate));
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
                    if ((isNull(indicatorValue) || indicatorValue != -IMPOSSIBLE_TO_DETECT)) {
                        indicatorValue = amount > AMOUNT_LIMIT ? RISK : NOT_RISK;
                    }
                } catch (Exception e) {
                    logService.tenderIndicatorFailed(INDICATOR_CODE, tenderId, e);
                    indicatorValue = IMPOSSIBLE_TO_DETECT;
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
