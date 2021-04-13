package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.persistence.entities.Award;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.entities.Tender;
import com.datapath.persistence.entities.TenderItem;
import com.datapath.persistence.entities.derivatives.WinsCount;
import com.datapath.persistence.repositories.derivatives.WinsCountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.datapath.indicatorsresolver.IndicatorConstants.ACTIVE;
import static java.util.Objects.isNull;
import static org.springframework.util.CollectionUtils.isEmpty;


//@Service
@Slf4j
@Deprecated
//TODO remove it after check handler
public class Risk_2_15_AprilExtractor extends BaseExtractor {

    private final String INDICATOR_CODE = "RISK-2-15";

    private WinsCountRepository repository;

    private boolean indicatorsResolverAvailable;

    public Risk_2_15_AprilExtractor(WinsCountRepository repository) {
        this.repository = repository;
        indicatorsResolverAvailable = true;
    }

    @Transactional
    public void checkIndicator(ZonedDateTime dateTime) {
        try {
            indicatorsResolverAvailable = false;
            Indicator indicator = getIndicator(INDICATOR_CODE);
            if (indicator.isActive() ) {
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
//    @Scheduled(cron = "${risk-common.cron}")
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
                    Arrays.asList("goods", "services")
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

                int indicatorValue = NOT_RISK;

                List<Award> activeAward = tender.getAwards()
                        .stream()
                        .filter(a -> ACTIVE.equalsIgnoreCase(a.getStatus()))
                        .collect(Collectors.toList());

                if (isEmpty(activeAward)) {
                    indicatorValue = CONDITIONS_NOT_MET;
                } else {
                    for (Award award : activeAward) {
                        String supplier = award.getSupplierIdentifierScheme() + award.getSupplierIdentifierId();

                        WinsCount winsCount = repository.findByProcuringEntityAndSupplier(tender.getTvProcuringEntity(), supplier);

                        if (isNull(winsCount)) {
                            continue;
                        }

                        if (winsCount.getCpvCount() >= 4) {
                            indicatorValue = RISK;
                            break;
                        }

                        if (winsCount.getCpvCount() == 3) {
                            List<String> analyticCpv = Arrays.asList(winsCount.getCpvList().split(","));

                            List<String> tenderCpv = tender.getItems().stream()
                                    .filter(i -> i.getLot().getId().equals(award.getLot().getId()))
                                    .map(TenderItem::getClassificationId)
                                    .collect(Collectors.toList());

                            for (String cpv : tenderCpv) {
                                if (!analyticCpv.contains(cpv)) {
                                    indicatorValue = RISK;
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
