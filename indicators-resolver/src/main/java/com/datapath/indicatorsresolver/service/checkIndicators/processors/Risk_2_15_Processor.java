package com.datapath.indicatorsresolver.service.checkIndicators.processors;

import com.datapath.indicatorsresolver.model.TenderDimensions;
import com.datapath.indicatorsresolver.model.TenderIndicator;
import com.datapath.indicatorsresolver.service.checkIndicators.BaseExtractor;
import com.datapath.persistence.entities.Award;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.entities.Tender;
import com.datapath.persistence.entities.TenderItem;
import com.datapath.persistence.entities.derivatives.WinsCount;
import com.datapath.persistence.repositories.derivatives.WinsCountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.datapath.indicatorsresolver.IndicatorConstants.ACTIVE;
import static java.util.Objects.isNull;
import static org.springframework.util.CollectionUtils.isEmpty;

@Component
@Slf4j
public class Risk_2_15_Processor extends BaseExtractor {

    @Autowired
    private WinsCountRepository winsCountRepository;

    @Transactional
    public List<TenderIndicator> process(Indicator indicator, List<Long> ids) {

        List<Tender> tenders = tenderRepository.findByIdIn(ids);

        Map<String, TenderDimensions> dimensionsMap = getTenderDimensionsWithIndicatorLastIteration(
                tenders, indicator.getId());

        List<TenderIndicator> tenderIndicators = new LinkedList<>();

        tenders.forEach(tender -> {
            log.info("Process tender {}", tender.getOuterId());

            TenderDimensions tenderDimensions = dimensionsMap.get(tender.getOuterId());

            int indicatorValue = NOT_RISK;

            try {
                List<Award> activeAward = tender.getAwards()
                        .stream()
                        .filter(a -> ACTIVE.equalsIgnoreCase(a.getStatus()))
                        .collect(Collectors.toList());

                if (isEmpty(activeAward)) {
                    indicatorValue = CONDITIONS_NOT_MET;
                } else {
                    for (Award award : activeAward) {
                        String supplier = award.getSupplierIdentifierScheme() + award.getSupplierIdentifierId();

                        WinsCount winsCount = winsCountRepository.findByProcuringEntityAndSupplier(tender.getTvProcuringEntity(), supplier);

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
            } catch (Exception e) {
                logService.tenderIndicatorFailed(indicator.getId(), tender.getOuterId(), e);
                indicatorValue = IMPOSSIBLE_TO_DETECT;
            }

            tenderIndicators.add(new TenderIndicator(tenderDimensions, indicator, indicatorValue));
        });

        return tenderIndicators;
    }

}
