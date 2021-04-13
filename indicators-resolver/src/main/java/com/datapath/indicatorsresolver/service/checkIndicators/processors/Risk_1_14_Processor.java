package com.datapath.indicatorsresolver.service.checkIndicators.processors;

import com.datapath.indicatorsresolver.model.ContractDimensions;
import com.datapath.indicatorsresolver.model.ContractIndicator;
import com.datapath.indicatorsresolver.service.checkIndicators.BaseExtractor;
import com.datapath.persistence.entities.Contract;
import com.datapath.persistence.entities.Indicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toSet;

@Slf4j
@Service
public class Risk_1_14_Processor extends BaseExtractor {

    private static final int WORKING_DAYS = 20;

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

            try {
                int indicatorValue = getContractIndicatorValue(contract);
                contractIndicators.add(new ContractIndicator(contractDimensions, indicator, indicatorValue));
            } catch (Exception e) {
                logService.contractIndicatorFailed(indicator.getId(), contract.getOuterId(), e);
                contractIndicators.add(new ContractIndicator(contractDimensions, indicator, IMPOSSIBLE_TO_DETECT));
            }
        });
        return contractIndicators;
    }

    private int getContractIndicatorValue(Contract contract) {
        if (contract.getPeriodEndDate() == null) return IMPOSSIBLE_TO_DETECT;
        if (contract.getPeriodEndDate().isAfter(ZonedDateTime.now())) return CONDITIONS_NOT_MET;

        ZonedDateTime offset = getDateOfCurrentDateMinusNWorkingDays(WORKING_DAYS);

        return contract.getPeriodEndDate().isBefore(offset) ? RISK : NOT_RISK;
    }
}