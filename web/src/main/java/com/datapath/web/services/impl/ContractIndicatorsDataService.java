package com.datapath.web.services.impl;

import com.datapath.web.api.rest.exceptions.UnsupportedSortOrderException;
import com.datapath.web.domain.DruidIndicator;
import com.datapath.web.domain.IndicatorsDataPage;
import com.datapath.web.domain.IndicatorsPage;
import com.datapath.web.domain.contracting.ContractIndicatorsData;
import com.datapath.web.providers.IndicatorInfoProvider;
import com.datapath.web.services.IndicatorService;
import com.datapath.web.util.ContractIndicatorsDataBuilder;
import com.datapath.web.util.ContractIndicatorsDataPartsResolver;
import com.datapath.web.util.MapUtil;
import com.datapath.web.util.PaginationUtils;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.datapath.web.common.SortOrder.ASC;
import static com.datapath.web.common.SortOrder.DESC;

@Service
public class ContractIndicatorsDataService {

    private IndicatorService druidContractIndicatorService;
    private IndicatorInfoProvider indicatorInfoProvider;

    public ContractIndicatorsDataService(IndicatorService druidContractIndicatorService,
                                         IndicatorInfoProvider indicatorInfoProvider) {
        this.druidContractIndicatorService = druidContractIndicatorService;
        this.indicatorInfoProvider = indicatorInfoProvider;
    }

    public ContractIndicatorsData getContractIndicators(String contractId) {
        List<DruidIndicator> druidIndicators = druidContractIndicatorService.getIndicators(contractId);
        ContractIndicatorsDataPartsResolver resolver = new ContractIndicatorsDataPartsResolver(
                druidIndicators, indicatorInfoProvider).resolve();
        return ContractIndicatorsDataBuilder.create()
                .setTenderId(resolver.getTenderId())
                .setContractId(resolver.getContractId())
                .setContractOuterId(resolver.getTenderOuterId())
                .setProcedureType(resolver.getProcedureType())
                .setTenderStatus(resolver.getTenderStatus())
                .setIndicators(resolver.getIndicators())
                .setIndicatorsSummary(resolver.getIndicatorsSummary())
                .setIndicatorInfo(resolver.getIndicatorInfos())
                .build();
    }

    public IndicatorsDataPage getContractIndicators(ZonedDateTime startDate,
                                                    ZonedDateTime endDate,
                                                    Integer limit,
                                                    String path,
                                                    String order,
                                                    Boolean riskOnly,
                                                    List<String> procedureTypes,
                                                    List<String> indicatorIds) {

        List<DruidIndicator> druidIndicators = druidContractIndicatorService.getIndicators(startDate, endDate, limit, order, riskOnly, procedureTypes, indicatorIds);
        List<ContractIndicatorsData> contractIndicatorsDataList = new ArrayList<>();

        Map<String, List<DruidIndicator>> grouperByContractIndicators = MapUtil.sortIndicatorsByDate(groupByOuterContractId(druidIndicators));
        grouperByContractIndicators.forEach((key, value) -> {
            ContractIndicatorsDataPartsResolver resolver = new ContractIndicatorsDataPartsResolver(
                    value, indicatorInfoProvider).resolve();

            ContractIndicatorsData tenderIndicatorsData = ContractIndicatorsDataBuilder.create()
                    .setTenderId(resolver.getTenderId())
                    .setContractId(resolver.getContractId())
                    .setContractOuterId(resolver.getContractOuterId())
                    .setProcedureType(resolver.getProcedureType())
                    .setTenderStatus(resolver.getTenderStatus())
                    .setIndicators(resolver.getIndicators())
                    .setIndicatorsSummary(resolver.getIndicatorsSummary())
                    .setIndicatorInfo(resolver.getIndicatorInfos())
                    .build();

            contractIndicatorsDataList.add(tenderIndicatorsData);
        });

        if (order.equals(DESC)) {
            Collections.reverse(contractIndicatorsDataList);
        }

        IndicatorsDataPage<ContractIndicatorsData> pageData = new IndicatorsDataPage<>();
        pageData.setData(contractIndicatorsDataList);
        pageData.setNextPage(createNextPageInfo(druidIndicators, limit, path, order,
                riskOnly, startDate, endDate, procedureTypes, indicatorIds));

        return pageData;
    }

    private Map<String, List<DruidIndicator>> groupByOuterContractId(List<DruidIndicator> indicators) {
        Set<String> uniqueContracts = indicators.stream()
                .map(DruidIndicator::getContractOuterId)
                .collect(Collectors.toSet());

        Map<String, List<DruidIndicator>> groupedIndicators = new HashMap<>();
        for (String contractId : uniqueContracts) {
            List<DruidIndicator> contractIndicators = new ArrayList<>();
            for (DruidIndicator druidIndicator : indicators) {
                if (druidIndicator.getContractOuterId().equals(contractId)) {
                    contractIndicators.add(druidIndicator);
                }
            }
            groupedIndicators.put(contractId, contractIndicators);
        }

        return groupedIndicators;
    }

    private IndicatorsPage createNextPageInfo(List<DruidIndicator> indicators,
                                              Integer limit,
                                              String path,
                                              String order,
                                              Boolean riskOnly,
                                              ZonedDateTime startDate,
                                              ZonedDateTime endDate,
                                              List<String> procedureTypes,
                                              List<String> indicatorIds) {
        if (indicators.isEmpty()) {
            return null;
        }

        DruidIndicator contractIndicatorsData = null;
        switch (order) {
            case ASC: {
                contractIndicatorsData = indicators.stream()
                        .sorted((o1, o2) -> o1.getDate().isBefore(o2.getDate()) ? 1 : -1)
                        .collect(Collectors.toList()).get(0);
                break;
            }
            case DESC: {
                Set<String> uniqueContracts = indicators.stream()
                        .map(DruidIndicator::getContractOuterId)
                        .collect(Collectors.toSet());

                List<DruidIndicator> lastIndicators = new ArrayList<>();
                uniqueContracts.forEach(contractOuterId -> {
                    Optional<DruidIndicator> optionalIndicator = indicators.stream()
                            .filter(indicator -> indicator.getContractOuterId().equals(contractOuterId))
                            .max((o1, o2) -> o1.getDate().isBefore(o2.getDate()) ? -1 : 1);
                    optionalIndicator.ifPresent(lastIndicators::add);
                });

                Optional<DruidIndicator> firstOptionalIndicator = lastIndicators.stream()
                        .sorted((o1, o2) -> o1.getDate().isBefore(o2.getDate()) ? -1 : 1)
                        .findFirst();

                if (firstOptionalIndicator.isPresent()) {
                    contractIndicatorsData = firstOptionalIndicator.get();
                }
                break;
            }
            default: {
                throw new UnsupportedSortOrderException(order);
            }
        }

        ZonedDateTime nextPageStartDate = null;
        ZonedDateTime nextPageEndDate = null;
        if (contractIndicatorsData != null) {
            nextPageStartDate = contractIndicatorsData.getDate().plusNanos(1000000);
            nextPageEndDate = contractIndicatorsData.getDate().minusNanos(1000000);
        }

        return PaginationUtils.createIndicatorsPage(startDate, endDate, nextPageStartDate, nextPageEndDate,
                order, limit, riskOnly, path, procedureTypes, indicatorIds);
    }

}
