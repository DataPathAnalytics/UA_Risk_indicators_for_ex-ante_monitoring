package com.datapath.indicatorsresolver.service.checkIndicators.processors;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.indicatorsresolver.service.checkIndicators.BaseExtractor;
import com.datapath.persistence.entities.Complaint;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.entities.Question;
import com.datapath.persistence.entities.Tender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.datapath.indicatorsresolver.IndicatorConstants.*;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Service
public class Risk_1_10_Processor extends BaseExtractor {

    private static final int QUESTION_FILTER_DAYS = 10;
    private static final int COUNT_WORK_DAYS = 4;
    private static final List<String> IGNORED_COMPLAINT_STATUSES = Arrays.asList(DRAFT, CANCELLED);

    @Transactional
    public List<TenderIndicator> process(Indicator indicator, List<Long> ids) {
        List<Tender> tenders = tenderRepository.findByIdIn(ids);

        Map<String, TenderDimensions> dimensionsMap = getTenderDimensionsWithIndicatorLastIteration(
                tenders, indicator.getId());

        Map<String, Long> maxTendersIndicatorIteration = extractDataService
                .getMaxTenderIndicatorIteration(
                        getOuterIds(tenders),
                        indicator.getId());

        Map<String, Integer> maxTendersIterationData = extractDataService
                .getMaxTendersIterationData(maxTendersIndicatorIteration, indicator.getId());

        List<TenderIndicator> tenderIndicators = new LinkedList<>();

        tenders.forEach(tender -> {
            log.info("Process tender {}", tender.getOuterId());

            TenderDimensions tenderDimensions = dimensionsMap.get(tender.getOuterId());
            try {
                if (maxTendersIterationData.containsKey(tender.getOuterId()) &&
                        maxTendersIterationData.get(tender.getOuterId()).equals(RISK)) {
                    return;
                }

                int indicatorValue = getIndicatorValue(tender);
                tenderIndicators.add(new TenderIndicator(tenderDimensions, indicator, indicatorValue));
            } catch (Exception e) {
                logService.tenderIndicatorFailed(indicator.getId(), tender.getOuterId(), e);
                tenderIndicators.add(new TenderIndicator(tenderDimensions, indicator, IMPOSSIBLE_TO_DETECT));
            }
        });

        return tenderIndicators;
    }

    private int getIndicatorValue(Tender tender) {

        if (!isEmpty(tender.getQuestions()) || !isEmpty(tender.getComplaints())) {
            return CONDITIONS_NOT_MET;
        }

        List<Complaint> complaints = tender.getComplaints().stream()
                .filter(c -> !IGNORED_COMPLAINT_STATUSES.contains(c.getStatus()) &&
                        CLAIM.equals(c.getComplaintType()))
                .collect(toList());

        List<Question> questions = tender.getQuestions().stream()
                .filter(q -> getDaysBetween(q.getDate(), tender.getEndDate()) >= QUESTION_FILTER_DAYS)
                .collect(toList());

        if (!isEmpty(questions) || !isEmpty(complaints)) {
            return CONDITIONS_NOT_MET;
        } else {

            if (!isEmpty(questions)) {
                for (Question question : questions) {
                    if (isNull(question.getDateAnswered()) && isNull(question.getAnswer())) {
                        if (getWorkingDaysBetween(question.getDate(), ZonedDateTime.now()) > COUNT_WORK_DAYS) {
                            return RISK;
                        }
                    } else if (getWorkingDaysBetween(question.getDate(), question.getDateAnswered()) > COUNT_WORK_DAYS) {
                        return RISK;
                    }
                }
            }

            if (!isEmpty(complaints)) {
                for (Complaint complaint : complaints) {
                    if (isNull(complaint.getDateAnswered())) {
                        if (getWorkingDaysBetween(complaint.getDateSubmitted(), ZonedDateTime.now()) > COUNT_WORK_DAYS) {
                            return RISK;
                        }
                    } else if (getWorkingDaysBetween(complaint.getDateSubmitted(), complaint.getDateAnswered()) > COUNT_WORK_DAYS) {
                        return RISK;
                    }
                }
            }
        }
        return NOT_RISK;
    }
}
