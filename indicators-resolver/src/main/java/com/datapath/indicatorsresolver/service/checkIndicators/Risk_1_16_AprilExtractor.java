package com.datapath.indicatorsresolver.service.checkIndicators;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.persistence.entities.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Stream;

import static com.datapath.indicatorsresolver.IndicatorConstants.*;
import static java.time.ZonedDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.*;
import static org.springframework.util.CollectionUtils.isEmpty;


@Service
@Slf4j
public class Risk_1_16_AprilExtractor extends BaseExtractor {

    private static final String INDICATOR_CODE = "RISK-1-16";
    private static final Integer CALENDAR_DAYS_LIMIT = 20;
    private static final Integer WORK_DAYS_LIMIT = 3;

    private boolean indicatorsResolverAvailable;

    public Risk_1_16_AprilExtractor() {
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
    @Scheduled(cron = "${risk-1-16.cron}")
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
                        ? now().minus(Period.ofYears(1)).withHour(0)
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

            if (tenders.isEmpty()) break;

            List<String> tenderIds = tenders.stream().map(Tender::getOuterId).collect(toList());

            //fixme avoid getting last iteration while receive dimensions because we will receive it later
            Map<String, TenderDimensions> dimensionsMap = getTenderDimensionsWithIndicatorLastIteration(
                    tenders, INDICATOR_CODE);

            Map<String, Long> maxTendersIndicatorIteration = extractDataService
                    .getMaxTenderIndicatorIteration(new HashSet<>(tenderIds), INDICATOR_CODE);

            Map<String, Map<String, Integer>> maxTendersLotIterationData = extractDataService
                    .getMaxTendersLotIterationData(maxTendersIndicatorIteration, INDICATOR_CODE);

            tenders.forEach(tender -> {
                Map<String, Integer> lotIndicators = new HashMap<>();
                String tenderOuterId = tender.getOuterId();
                tender.getLots().forEach(lot -> {
                    try {
                        if (maxTendersLotIterationData.get(tenderOuterId).containsKey(lot.getOuterId())
                                && maxTendersLotIterationData.get(tenderOuterId).get(lot.getOuterId()).equals(RISK)) {
                            lotIndicators.put(lot.getOuterId(), RISK);
                        } else {
                            int indicatorValue = checkLotIndicatorValue(lot);
                            lotIndicators.put(lot.getOuterId(), indicatorValue);
                        }
                    } catch (Exception e) {
                        logService.lotIndicatorFailed(INDICATOR_CODE, tenderOuterId, lot.getOuterId(), e);
                        lotIndicators.put(lot.getOuterId(), IMPOSSIBLE_TO_DETECT);
                    }
                });

                Map<Integer, List<String>> lotGroupedByRiskValue = lotIndicators.entrySet().stream()
                        .collect(groupingBy(Map.Entry::getValue, mapping(Map.Entry::getKey, toList())));

                List<TenderIndicator> tenderIndicators = new ArrayList<>();
                lotGroupedByRiskValue.forEach((indicatorValue, lots) -> {
                    tenderIndicators.add(new TenderIndicator(dimensionsMap.get(tenderOuterId), indicator, indicatorValue, lots));
                });

                uploadIndicatorIfNotExists(tenderOuterId, INDICATOR_CODE, tenderIndicators);

            });

            ZonedDateTime maxTenderDateCreated = getMaxTenderDateCreated(dimensionsMap, dateTime);
            indicator.setLastCheckedDateCreated(maxTenderDateCreated);
            indicatorRepository.save(indicator);

            dateTime = maxTenderDateCreated;
        }

        ZonedDateTime now = now();
        indicator.setDateChecked(now);
        indicatorRepository.save(indicator);

        log.info("{} indicator finished", INDICATOR_CODE);
    }

    private int checkLotIndicatorValue(Lot lot) {

        if (CANCELLED.equalsIgnoreCase(lot.getStatus()) || UNSUCCESSFUL.equalsIgnoreCase(lot.getStatus())) {
            return CONDITIONS_NOT_MET;
        }

        List<Award> awards = !isEmpty(lot.getAwards()) ? lot.getAwards() : emptyList();

        boolean hasActiveAwards = awards.stream()
                .anyMatch(award -> award.getStatus().equalsIgnoreCase(ACTIVE));
        if (!hasActiveAwards) {
            return CONDITIONS_NOT_MET;
        }

        boolean hasComplaintWithAcceptedWithoutDecision = awards.stream()
                .filter(a -> !isEmpty(a.getComplaints()))
                .flatMap(award -> award.getComplaints().stream())
                .anyMatch(complaint ->
                        complaint.getComplaintType().equalsIgnoreCase(COMPLAINT)
                                && complaint.getDateSubmitted() != null
                                && complaint.getDateDecision() == null
                );

        if (hasComplaintWithAcceptedWithoutDecision) {
            return CONDITIONS_NOT_MET;
        }

        boolean hasNonPKCS7Docs = awards.stream()
                .filter(a -> nonNull(a.getTenderContract()))
                .flatMap(a -> Stream.of(a.getTenderContract()))
                .filter(a -> nonNull(a.getContract()))
                .flatMap(c -> Stream.of(c.getContract()))
                .flatMap(c -> c.getDocuments().stream())
                .anyMatch(d -> !d.getFormat().equalsIgnoreCase(PKCS7_SIGNATURE));

        boolean hasContractDocs = awards.stream().filter(a -> nonNull(a.getTenderContract()))
                .flatMap(a -> Stream.of(a.getTenderContract()))
                .filter(c -> ACTIVE.equals(c.getStatus()))
                .filter(c -> isEmpty(c.getDocuments()))
                .flatMap(c -> c.getDocuments().stream())
                .anyMatch(d -> d.getDocumentOf().equals(CONTRACT) && !d.getFormat().equalsIgnoreCase(PKCS7_SIGNATURE));


        boolean hasOldAwardWithoutComplains = false;

        Optional<Award> activeAwardWithMinDate = awards.stream()
                .filter(a -> ACTIVE.equals(a.getStatus()) && isEmpty(a.getComplaints()))
                .min(Comparator.comparing(Award::getDate));
        if (activeAwardWithMinDate.isPresent()) {
            ZonedDateTime awardMaxDate = getDateOfCurrentDateMinusNWorkingDays(WORK_DAYS_LIMIT).minusDays(CALENDAR_DAYS_LIMIT);

            Award award = activeAwardWithMinDate.get();
            if (award.getDate().isBefore(awardMaxDate)) {
                hasOldAwardWithoutComplains = true;
            }
        }

        boolean hasOldAwardWithComplains = false;

        List<Award> awardsWithComplaints = awards.stream().filter(a -> !isEmpty(a.getComplaints())).collect(toList());
        for (Award award : awardsWithComplaints) {

            List<Complaint> complaints = award.getComplaints().stream()
                    .filter(c ->
                            c.getComplaintType().equals(COMPLAINT)
                                    && nonNull(c.getDateAccepted())
                                    && nonNull(c.getDateSubmitted())
                                    && nonNull(c.getDateDecision()))
                    .collect(toList());

            if (!isEmpty(complaints)) {
                Set<LocalDate> excludedDates = complaints.stream()
                        .flatMap(c -> dateBetween(c.getDateSubmitted().toLocalDate(), c.getDateDecision().toLocalDate()).stream())
                        .collect(toSet());

                ZonedDateTime awardMaxDate = getDateOfCurrentDateMinusNWorkingDays(WORK_DAYS_LIMIT)
                        .minusDays(CALENDAR_DAYS_LIMIT)
                        .minusDays(excludedDates.size());

                if (award.getDate().isBefore(awardMaxDate)) {
                    hasOldAwardWithComplains = true;
                    break;
                }
            }
        }

        if (!hasNonPKCS7Docs && !hasContractDocs && (hasOldAwardWithoutComplains || hasOldAwardWithComplains)) {
            return RISK;
        }

        return NOT_RISK;
    }
}
