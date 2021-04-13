package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.persistence.entities.Award;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.entities.Lot;
import com.datapath.persistence.entities.Tender;
import com.datapath.persistence.entities.derivatives.Contracts3Years;
import com.datapath.persistence.entities.nbu.ExchangeRate;
import com.datapath.persistence.repositories.derivatives.Contracts3YearsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.datapath.indicatorsresolver.IndicatorConstants.ACTIVE;
import static com.datapath.indicatorsresolver.IndicatorConstants.ADDITIONAL_PURCHASE;
import static java.util.Objects.isNull;
import static org.springframework.util.CollectionUtils.isEmpty;


@Service
@Slf4j
public class Risk_1_2_AprilExtractor extends BaseExtractor {

    private final String INDICATOR_CODE = "RISK-1-2";

    private Contracts3YearsRepository repository;
    private boolean indicatorsResolverAvailable;

    public Risk_1_2_AprilExtractor(Contracts3YearsRepository repository) {
        this.repository = repository;
        indicatorsResolverAvailable = true;
    }

    @Transactional
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
    @Transactional
    @Scheduled(cron = "${risk-1-2.cron}")
    public void checkIndicator() {
        if (!indicatorsResolverAvailable) {
            log.info(String.format(INDICATOR_NOT_AVAILABLE_MESSAGE_FORMAT, INDICATOR_CODE));
            return;
        }
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive()) {
                ZonedDateTime dateTime = isNull(indicator.getDateChecked())
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
            List<Tender> tenders = tenderRepository.findTendersByMainProcurementCategory(
                    dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind()),
                    Collections.singletonList("goods")
            );

            if (isEmpty(tenders)) {
                break;
            }

            Map<String, TenderDimensions> dimensionsMap = getTenderDimensionsWithIndicatorLastIteration(
                    tenders, INDICATOR_CODE);

            List<TenderIndicator> tenderIndicators = new LinkedList<>();

            tenders.forEach(tender -> {
                log.info("Process tender {}", tender.getOuterId());

                TenderDimensions tenderDimensions = dimensionsMap.get(tender.getOuterId());

                try {
                    int indicatorValue = NOT_RISK;

                    if (!tender.getCause().equals(ADDITIONAL_PURCHASE)) {
                        indicatorValue = CONDITIONS_NOT_MET;
                    } else {

                        List<Award> activeAward = tender.getAwards()
                                .stream()
                                .filter(a -> ACTIVE.equalsIgnoreCase(a.getStatus()))
                                .collect(Collectors.toList());

                        if (isEmpty(activeAward)) {
                            indicatorValue = CONDITIONS_NOT_MET;
                        } else {
                            for (Award award : activeAward) {
                                String supplier = award.getSupplierIdentifierScheme() + award.getSupplierIdentifierId();

                                Map<String, List<Lot>> cpvLots = new HashMap<>();

                                tender.getItems().forEach(item -> {
                                    if (item.getLot().getId().equals(award.getLot().getId())) {
                                        if (cpvLots.containsKey(item.getClassificationId())) {
                                            cpvLots.get(item.getClassificationId()).add(item.getLot());
                                        } else {
                                            List<Lot> lots = new LinkedList<>();
                                            lots.add(item.getLot());
                                            cpvLots.put(item.getClassificationId(), lots);
                                        }
                                    }
                                });

                                for (Map.Entry<String, List<Lot>> entry : cpvLots.entrySet()) {
                                    Optional<Contracts3Years> contracts3Years = repository.findByProcuringEntityAndSupplierAndCpv(
                                            tender.getTvProcuringEntity(),
                                            supplier,
                                            entry.getKey()
                                    );

                                    if (!contracts3Years.isPresent()) {
                                        indicatorValue = RISK;
                                        break;
                                    }

                                    for (Lot lot : entry.getValue()) {
                                        Double amount = lot.getAmount();

                                        if (!lot.getCurrency().equals(UAH_CURRENCY)) {
                                            ZonedDateTime rateDate = isNull(tender.getStartDate()) ?
                                                    tender.getDate() :
                                                    tender.getStartDate();

                                            ExchangeRate rate = exchangeRateService.getOneByCodeAndDate(lot.getCurrency(), rateDate);
                                            amount = amount * rate.getRate();
                                        }

                                        if (amount / contracts3Years.get().getAmount() > 0.5) {
                                            indicatorValue = RISK;
                                            break;
                                        }
                                    }

                                    if (indicatorValue == RISK) {
                                        break;
                                    }
                                }

                                if (indicatorValue == RISK) {
                                    break;
                                }
                            }
                        }
                    }

                    tenderIndicators.add(new TenderIndicator(tenderDimensions, indicator, indicatorValue));
                } catch (Exception e) {
                    logService.tenderIndicatorFailed(INDICATOR_CODE, tender.getOuterId(), e);
                    tenderIndicators.add(new TenderIndicator(tenderDimensions, indicator, IMPOSSIBLE_TO_DETECT));
                }
            });

            tenderIndicators.forEach(this::uploadIndicator);

            ZonedDateTime maxTenderDateCreated = getMaxTenderDateCreated(dimensionsMap, dateTime);
            indicator.setLastCheckedDateCreated(maxTenderDateCreated);
            indicator.setDateChecked(ZonedDateTime.now());
            indicatorRepository.save(indicator);

            dateTime = maxTenderDateCreated;
        }

        log.info("{} indicator finished", INDICATOR_CODE);
    }

}
