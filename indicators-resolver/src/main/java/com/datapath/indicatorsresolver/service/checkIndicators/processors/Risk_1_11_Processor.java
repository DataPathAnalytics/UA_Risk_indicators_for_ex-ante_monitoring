package com.datapath.indicatorsresolver.service.checkIndicators.processors;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.indicatorsresolver.service.checkIndicators.BaseExtractor;
import com.datapath.persistence.entities.Complaint;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.entities.Tender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.datapath.indicatorsresolver.IndicatorConstants.*;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Service
public class Risk_1_11_Processor extends BaseExtractor {

    private static final int DAYS_COUNT = 6;

    @Transactional
    public List<TenderIndicator> process(Indicator indicator, List<Long> ids) {
        List<Tender> tenders = tenderRepository.findByIdIn(ids);

        Map<String, TenderDimensions> dimensionsMap = getTenderDimensionsWithIndicatorLastIteration(
                tenders, indicator.getId());

        List<TenderIndicator> tenderIndicators = new LinkedList<>();

        tenders.forEach(tender -> {
            log.info("Process tender {}", tender.getOuterId());

            TenderDimensions tenderDimensions = dimensionsMap.get(tender.getOuterId());

            try {
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
        List<Complaint> complaints = tender.getAwards()
                .stream()
                .flatMap(a -> a.getComplaints().stream())
                .filter(c -> c.getComplaintType().equals(CLAIM)
                        && (!c.getStatus().equals(DRAFT) && !c.getStatus().equals(CANCELLED)))
                .collect(toList());

        if (isEmpty(complaints)) return CONDITIONS_NOT_MET;

        for (Complaint complaint : complaints) {

            if (complaint.getDateSubmitted() == null) {
                return CONDITIONS_NOT_MET;
            }

            ZonedDateTime dateAnswered;
            if (nonNull(complaint.getDateAnswered())) {
                dateAnswered = toUaMidnight(complaint.getDateAnswered());
            } else {
                dateAnswered = toUaMidnight(ZonedDateTime.now());
            }

            long days = getDaysBetween(complaint.getDateSubmitted(), dateAnswered);

            if (days >= DAYS_COUNT) {
                return RISK;
            }
        }

        return NOT_RISK;
    }
}
