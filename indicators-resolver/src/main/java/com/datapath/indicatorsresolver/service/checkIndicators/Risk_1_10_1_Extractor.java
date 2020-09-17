package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.entities.nbu.ExchangeRate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static com.datapath.persistence.utils.DateUtils.toZonedDateTime;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;


@Service
@Slf4j
public class Risk_1_10_1_Extractor extends BaseExtractor {

    /*
    Придбання товарів і послуг без проведення процедур закупівель (звітування)
    */

    private final String INDICATOR_CODE = "RISK1-10_1";

    private Map<String, Long> paKindAmountLimitMap = new HashMap<String, Long>() {{
        put("general", 200000L);
        put("authority", 200000L);
        put("central", 200000L);
        put("social", 200000L);
        put("special", 1000000L);
    }};

    private boolean indicatorsResolverAvailable;

    public Risk_1_10_1_Extractor() {
        indicatorsResolverAvailable = true;
    }

    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getActiveIndicator(INDICATOR_CODE);
            if (nonNull(indicator) && tenderRepository.findMaxDateModified().isAfter(ZonedDateTime.now().minusHours(AVAILABLE_HOURS_DIFF))) {
                checkRisk_1_10_1Indicator(indicator, dateTime);
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
                        ? ZonedDateTime.now().minus(Period.ofDays(2)).withHour(0)
                        : indicator.getLastCheckedDateCreated();
                checkRisk_1_10_1Indicator(indicator, dateTime);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            indicatorsResolverAvailable = true;
        }
    }


    private void checkRisk_1_10_1Indicator(Indicator indicator, ZonedDateTime dateTime) {
        int size = 100;
        int page = 0;
        while (true) {

            List<Object[]> tenders = tenderRepository.findGoodsServicesTenderIdPAKindAndAmountExceptFinances(
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

                Integer indicatorValue = null;
                String tenderId = tenderInfo[0].toString();
                log.info("Extract data from tender [{}[", tenderId);
                tenderIds.add(tenderId);

                Timestamp timestampStartDate = (Timestamp) tenderInfo[1];
                String kind = tenderInfo[2].toString();
                String currency = tenderInfo[3].toString();
                Double amount = Double.parseDouble(tenderInfo[4].toString());

                TenderDimensions tenderDimensions = new TenderDimensions(tenderId);

                if (!currency.equals(UAH_CURRENCY)) {
                    if (!isNull(timestampStartDate)) {
                        ZonedDateTime date = toZonedDateTime(timestampStartDate)
                                .withZoneSameInstant(ZoneId.of("Europe/Kiev"))
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
                if (nonNull(amount) && (isNull(indicatorValue) || indicatorValue != -IMPOSSIBLE_TO_DETECT)) {
                    indicatorValue = amount > paKindAmountLimitMap.get(kind) ? RISK : NOT_RISK;
                }
                return new TenderIndicator(tenderDimensions, indicator,
                        indicatorValue, new ArrayList<>());


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

    }

}
