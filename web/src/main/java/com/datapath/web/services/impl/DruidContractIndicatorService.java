package com.datapath.web.services.impl;

import com.datapath.druidintegration.model.ContractsFilter;
import com.datapath.druidintegration.model.druid.response.common.Event;
import com.datapath.druidintegration.service.ExtractContractDataService;
import com.datapath.web.api.rest.exceptions.ContractNotFoundException;
import com.datapath.web.domain.DruidIndicator;
import com.datapath.web.mappers.DruidEventMapper;
import com.datapath.web.services.IndicatorService;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DruidContractIndicatorService implements IndicatorService {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss[.SSS]XXX";

    private ExtractContractDataService extractDataService;

    public DruidContractIndicatorService(ExtractContractDataService extractDataService) {
        this.extractDataService = extractDataService;
    }

    @Override
    public List<DruidIndicator> getIndicators(String contractId) {
        ContractsFilter contractsFilter = ContractsFilter.builder().contractId(contractId).build();
        List<DruidIndicator> contractIndicators = extractDataService.getContractDataByContractId(contractsFilter)
                .stream()
                .map(DruidEventMapper::mapToIndicator)
                .collect(Collectors.toList());

        if (contractIndicators.isEmpty()) {
            throw new ContractNotFoundException(contractId);
        }

        return contractIndicators;
    }

    @Override
    public List<DruidIndicator> getIndicators(ZonedDateTime startDate,
                                              ZonedDateTime endDate,
                                              Integer limit,
                                              String order,
                                              Boolean riskOnly,
                                              List<String> procedureTypes,
                                              List<String> indicatorIds) {

        ContractsFilter contractsFilter = ContractsFilter.builder()
                .indicatorIds(indicatorIds)
                .procedureTypes(procedureTypes)
                .build();

        String formattedStartDate = startDate.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        String formattedEndDate = endDate.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        List<Event> contractData = extractDataService.getTimePeriodContractData(
                formattedStartDate,
                formattedEndDate,
                limit,
                order,
                riskOnly,
                contractsFilter
        );

        return  contractData.stream()
                .map(DruidEventMapper::mapToIndicator)
                .collect(Collectors.toList());
    }
}
