package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.persistence.entities.Complaint;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.entities.Tender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.datapath.indicatorsresolver.IndicatorConstants.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;


@Service
@Slf4j
public class Risk_1_11_AprilExtractor extends BaseExtractor {

    private final String INDICATOR_CODE = "RISK-1-11";

    private boolean indicatorsResolverAvailable;

    public Risk_1_11_AprilExtractor() {
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
    @Scheduled(cron = "${risk-1-11.cron}")
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

                try {
                    int indicatorValue = NOT_RISK;

                    List<Complaint> complaints = tender.getAwards()
                            .stream()
                            .flatMap(a -> a.getComplaints().stream())
                            .filter(c -> c.getComplaintType().equals(CLAIM)
                                    && (!c.getStatus().equals(DRAFT) && !c.getStatus().equals(CANCELLED)))
                            .collect(toList());

                    if (isEmpty(complaints)) {
                        indicatorValue = CONDITIONS_NOT_MET;
                    } else {
                        for (Complaint complaint : complaints) {

                            if (complaint.getDateSubmitted() == null) {
                                indicatorValue = CONDITIONS_NOT_MET;
                                break;
                            }

                            ZonedDateTime dateAnswered;
                            if (nonNull(complaint.getDateAnswered())) {
                                dateAnswered = toUaMidnight(complaint.getDateAnswered());
                            } else {
                                dateAnswered = toUaMidnight(ZonedDateTime.now());
                            }

                            long days = getDaysBetween(complaint.getDateSubmitted(), dateAnswered);

                            if (days >= 6) {
                                indicatorValue = RISK;
                                break;
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
