package com.datapath.indicatorsresolver.service.checkIndicators.processors;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.indicatorsresolver.service.checkIndicators.BaseExtractor;
import com.datapath.persistence.entities.Award;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.entities.Lot;
import com.datapath.persistence.entities.Tender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.datapath.indicatorsresolver.IndicatorConstants.CANCELLED;
import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Service
public class Risk_2_16_Processor extends BaseExtractor {

    @Transactional
    public List<TenderIndicator> process(Indicator indicator, List<Long> ids) {
        List<Tender> tenders = tenderRepository.findByIdIn(ids);

        Map<String, TenderDimensions> dimensionsMap = getTenderDimensionsWithIndicatorLastIteration(
                tenders, indicator.getId());

        List<TenderIndicator> tenderIndicators = new LinkedList<>();

        tenders.forEach(tender -> {
            log.info("Process tender {}", tender.getOuterId());

            TenderDimensions tenderDimensions = dimensionsMap.get(tender.getOuterId());

            List<String> withRiskLotOuterIds = new LinkedList<>();
            List<String> withoutRiskLotOuterIds = new LinkedList<>();
            List<String> conditionsNotMetLotOuterIds = new LinkedList<>();

            for (Lot lot : tender.getLots()) {
                int indicatorValue;
                try {
                    indicatorValue = checkLotIndicator(lot);
                } catch (Exception e) {
                    logService.lotIndicatorFailed(indicator.getId(), tender.getOuterId(), lot.getOuterId(), e);
                    indicatorValue = IMPOSSIBLE_TO_DETECT;
                }

                if (RISK.equals(indicatorValue)) withRiskLotOuterIds.add(lot.getOuterId());
                else if (NOT_RISK.equals(indicatorValue)) withoutRiskLotOuterIds.add(lot.getOuterId());
                else conditionsNotMetLotOuterIds.add(lot.getOuterId());
            }

            if (!isEmpty(withRiskLotOuterIds))
                tenderIndicators.add(new TenderIndicator(tenderDimensions, indicator, RISK, withRiskLotOuterIds));
            if (!isEmpty(withoutRiskLotOuterIds))
                tenderIndicators.add(new TenderIndicator(tenderDimensions, indicator, NOT_RISK, withoutRiskLotOuterIds));
            if (!isEmpty(conditionsNotMetLotOuterIds))
                tenderIndicators.add(new TenderIndicator(tenderDimensions, indicator, CONDITIONS_NOT_MET, conditionsNotMetLotOuterIds));
        });

        return tenderIndicators;
    }

    private int checkLotIndicator(Lot lot) {
        for (Award award : lot.getAwards()) {
            if (!isEmpty(award.getComplaints())) {
                return CONDITIONS_NOT_MET;
            }
        }
        for (Award award : lot.getAwards()) {
            if (CANCELLED.equals(award.getStatus())) {
                return RISK;
            }
        }
        return NOT_RISK;
    }
}
