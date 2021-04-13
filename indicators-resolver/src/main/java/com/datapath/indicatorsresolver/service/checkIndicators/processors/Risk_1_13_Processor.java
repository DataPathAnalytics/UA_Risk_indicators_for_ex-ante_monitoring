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
import java.util.*;

import static com.datapath.indicatorsresolver.IndicatorConstants.COMPLAINT;
import static com.datapath.indicatorsresolver.IndicatorConstants.SATISFIED;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Service
public class Risk_1_13_Processor extends BaseExtractor {

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
                List<Complaint> complaints = tender.getComplaints()
                        .stream()
                        .filter(c -> SATISFIED.equals(c.getStatus()) && COMPLAINT.equals(c.getComplaintType()))
                        .collect(toList());

                complaints.addAll(
                        tender.getAwards()
                                .stream()
                                .flatMap(a -> a.getComplaints()
                                        .stream()
                                        .filter(c -> SATISFIED.equals(c.getStatus()) && COMPLAINT.equals(c.getComplaintType())))
                                .collect(toList())
                );

                if (isEmpty(complaints)) {
                    tenderIndicators.add(new TenderIndicator(tenderDimensions, indicator, CONDITIONS_NOT_MET));
                } else {
                    Optional<ZonedDateTime> minComplaintDate = complaints
                            .stream()
                            .map(Complaint::getDateDecision)
                            .filter(Objects::nonNull)
                            .sorted()
                            .findFirst();

                    if (!minComplaintDate.isPresent()) {
                        tenderIndicators.add(new TenderIndicator(tenderDimensions, indicator, IMPOSSIBLE_TO_DETECT));
                    } else {
                        int indicatorValue = getDaysBetween(minComplaintDate.get(), ZonedDateTime.now()) > 30 ? RISK : NOT_RISK;
                        tenderIndicators.add(new TenderIndicator(tenderDimensions, indicator, indicatorValue));
                    }
                }
            } catch (Exception e) {
                logService.tenderIndicatorFailed(indicator.getId(), tender.getOuterId(), e);
                tenderIndicators.add(new TenderIndicator(tenderDimensions, indicator, IMPOSSIBLE_TO_DETECT));
            }
        });

        return tenderIndicators;
    }
}
