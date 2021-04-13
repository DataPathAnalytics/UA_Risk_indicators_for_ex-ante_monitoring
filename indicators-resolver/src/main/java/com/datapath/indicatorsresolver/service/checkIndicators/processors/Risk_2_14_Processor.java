package com.datapath.indicatorsresolver.service.checkIndicators.processors;

import com.datapath.indicatorsresolver.model.ContractDimensions;
import com.datapath.indicatorsresolver.model.ContractIndicator;
import com.datapath.indicatorsresolver.service.checkIndicators.BaseExtractor;
import com.datapath.persistence.entities.Contract;
import com.datapath.persistence.entities.ContractChange;
import com.datapath.persistence.entities.Indicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.datapath.indicatorsresolver.IndicatorConstants.ACTIVE;
import static com.datapath.indicatorsresolver.IndicatorConstants.ITEM_PRICE_VARIATION;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Service
public class Risk_2_14_Processor extends BaseExtractor {

    @Transactional
    public List<ContractIndicator> process(Indicator indicator, List<Long> ids) {
        List<Contract> contracts = contractRepository.findByIdIn(ids);

        List<ContractIndicator> contractIndicators = new LinkedList<>();

        Map<String, ContractDimensions> dimensionsMap = getContractDimensionsWithIndicatorLastIteration(
                contracts.stream()
                        .map(Contract::getOuterId)
                        .collect(toSet()),
                indicator.getId()
        );

        contracts.forEach(contract -> {
            log.info("Process contract {}", contract.getOuterId());

            ContractDimensions contractDimensions = dimensionsMap.get(contract.getOuterId());

            int indicatorValue = NOT_RISK;
            try {
                List<ZonedDateTime> signedDates = contract.getChanges()
                        .stream()
                        .filter(c -> ACTIVE.equals(c.getStatus()))
                        .filter(c -> {
                            for (String type : c.getRationaleTypes()) {
                                if (ITEM_PRICE_VARIATION.equals(type)) {
                                    return true;
                                }
                            }
                            return false;
                        })
                        .map(ContractChange::getDateSigned)
                        .sorted()
                        .collect(toList());

                if (isEmpty(signedDates)) {
                    indicatorValue = CONDITIONS_NOT_MET;
                } else {
                    ZonedDateTime first = null;
                    for (ZonedDateTime date : signedDates) {
                        if (isNull(first)) {
                            first = date;
                        } else {
                            long days = getDaysBetween(first, date);

                            if (days < 90) {
                                indicatorValue = RISK;
                                break;
                            } else {
                                first = date;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logService.contractIndicatorFailed(indicator.getId(), contract.getOuterId(), e);
                indicatorValue = IMPOSSIBLE_TO_DETECT;
            }
            contractIndicators.add(new ContractIndicator(contractDimensions, indicator, indicatorValue));
        });
        return contractIndicators;
    }
}