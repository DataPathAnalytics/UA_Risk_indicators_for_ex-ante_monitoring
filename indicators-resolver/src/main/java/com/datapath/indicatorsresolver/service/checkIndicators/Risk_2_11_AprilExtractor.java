package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.entities.Tender;
import com.datapath.persistence.entities.TenderItem;
import com.datapath.persistence.entities.derivatives.NoNeed;
import com.datapath.persistence.repositories.derivatives.NoNeedRepository;
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

import static java.util.Objects.isNull;
import static org.springframework.util.CollectionUtils.isEmpty;


@Service
@Slf4j
public class Risk_2_11_AprilExtractor extends BaseExtractor {

    private final String INDICATOR_CODE = "RISK-2-11";

    private NoNeedRepository noNeedRepository;
    private boolean indicatorsResolverAvailable;

    public Risk_2_11_AprilExtractor(NoNeedRepository noNeedRepository) {
        this.noNeedRepository = noNeedRepository;
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
    @Scheduled(cron = "${risk-2-11.cron}")
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
            List<Tender> tenders = tenderRepository.findTenders(
                    dateTime,
                    Arrays.asList(indicator.getProcedureStatuses()),
                    Arrays.asList(indicator.getProcedureTypes()),
                    Arrays.asList(indicator.getProcuringEntityKind())
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

                try {
                    List<NoNeed> byProcuringEntity = noNeedRepository.findByProcuringEntity(tender.getTvProcuringEntity());

                    if (!isEmpty(byProcuringEntity)) {
                        Set<String> tenderCpv = tender.getItems()
                                .stream()
                                .map(TenderItem::getClassificationId)
                                .collect(Collectors.toSet());

                        boolean isPresent = byProcuringEntity
                                .stream()
                                .anyMatch(p -> tenderCpv.contains(p.getCpv()));

                        indicatorValue = isPresent ? RISK : NOT_RISK;
                    }
                } catch (Exception e) {
                    logService.tenderIndicatorFailed(INDICATOR_CODE, tender.getOuterId(), e);
                    indicatorValue = IMPOSSIBLE_TO_DETECT;
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
