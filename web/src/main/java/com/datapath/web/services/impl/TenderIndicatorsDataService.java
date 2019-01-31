package com.datapath.web.services.impl;

import com.datapath.web.api.rest.exceptions.UnsupportedSortOrderException;
import com.datapath.web.common.SortOrder;
import com.datapath.web.domain.DruidIndicator;
import com.datapath.web.domain.IndicatorsDataPage;
import com.datapath.web.domain.IndicatorsPage;
import com.datapath.web.domain.tendering.TenderIndicatorsData;
import com.datapath.web.providers.IndicatorInfoProvider;
import com.datapath.web.services.TenderService;
import com.datapath.web.util.MapUtil;
import com.datapath.web.util.PaginationUtils;
import com.datapath.web.util.TenderIndicatorsDataBuilder;
import com.datapath.web.util.TenderIndicatorsDataPartsResolver;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TenderIndicatorsDataService {

    private DruidTenderIndicatorService druidTenderIndicatorService;
    private IndicatorInfoProvider indicatorInfoProvider;
    private TenderService tenderService;

    public TenderIndicatorsDataService(DruidTenderIndicatorService druidTenderIndicatorService,
                                       IndicatorInfoProvider indicatorInfoProvider,
                                       TenderService tenderService) {
        this.druidTenderIndicatorService = druidTenderIndicatorService;
        this.indicatorInfoProvider = indicatorInfoProvider;
        this.tenderService = tenderService;
    }

    public TenderIndicatorsData getTenderIndicators(String tenderId) {
        List<DruidIndicator> druidIndicators = druidTenderIndicatorService.getIndicators(tenderId);
        Set<String> ids = druidIndicators.stream()
                .map(DruidIndicator::getTenderOuterId)
                .collect(Collectors.toSet());
        Map<String, String> tenderStatuses = tenderService.getTendersStatuses(new ArrayList<>(ids));

        TenderIndicatorsDataPartsResolver resolver = new TenderIndicatorsDataPartsResolver(
                druidIndicators, indicatorInfoProvider, tenderStatuses).resolve();
        return TenderIndicatorsDataBuilder.create()
                .setTenderId(resolver.getTenderId())
                .setTenderOuterId(resolver.getTenderOuterId())
                .setProcedureType(resolver.getProcedureType())
                .setStatus(resolver.getTenderStatus())
                .setIndicators(resolver.getIndicators())
                .setIndicatorsSummary(resolver.getIndicatorsSummary())
                .setIndicatorInfo(resolver.getIndicatorInfos())
                .build();
    }

    public IndicatorsDataPage getTenderIndicators(ZonedDateTime startDate,
                                                  ZonedDateTime endDate,
                                                  Integer limit,
                                                  String path,
                                                  String order,
                                                  Boolean riskOnly,
                                                  List<String> procedureTypes,
                                                  List<String> indicatorIds) {

        List<DruidIndicator> druidIndicators = druidTenderIndicatorService.getIndicators(
                startDate, endDate, limit, order, riskOnly, procedureTypes, indicatorIds);

        List<TenderIndicatorsData> tenderIndicatorsDataList = new ArrayList<>();

        Map<String, List<DruidIndicator>> grouperByTenderIndicators = MapUtil.sortIndicatorsByDate(
                groupByOuterTenderId(druidIndicators));

        Set<String> ids = druidIndicators.stream()
                .map(DruidIndicator::getTenderOuterId)
                .collect(Collectors.toSet());

        Map<String, String> tenderStatuses = tenderService.getTendersStatuses(
                new ArrayList<>(ids));

        grouperByTenderIndicators.forEach((key, value) -> {
            TenderIndicatorsDataPartsResolver resolver = new TenderIndicatorsDataPartsResolver(
                    value, indicatorInfoProvider, tenderStatuses).resolve();
            TenderIndicatorsData tenderIndicatorsData = TenderIndicatorsDataBuilder.create()
                    .setTenderId(resolver.getTenderId())
                    .setTenderOuterId(resolver.getTenderOuterId())
                    .setProcedureType(resolver.getProcedureType())
                    .setStatus(resolver.getTenderStatus())
                    .setIndicators(resolver.getIndicators())
                    .setIndicatorsSummary(resolver.getIndicatorsSummary())
                    .setIndicatorInfo(resolver.getIndicatorInfos())
                    .build();

            tenderIndicatorsDataList.add(tenderIndicatorsData);
        });

        if (order.equals(SortOrder.DESC)) {
            Collections.reverse(tenderIndicatorsDataList);
        }

        IndicatorsDataPage<TenderIndicatorsData> pageData = new IndicatorsDataPage<>();
        pageData.setData(tenderIndicatorsDataList);
        pageData.setNextPage(createNextPageInfo(
                druidIndicators, limit, path, order,
                riskOnly, startDate, endDate,
                procedureTypes, indicatorIds
        ));

        return pageData;
    }

    private Map<String, List<DruidIndicator>> groupByOuterTenderId(List<DruidIndicator> indicators) {
        Set<String> uniqueTenders = indicators.stream()
                .map(DruidIndicator::getTenderOuterId)
                .collect(Collectors.toSet());

        Map<String, List<DruidIndicator>> groupedIndicators = new HashMap<>();
        for (String tenderId : uniqueTenders) {
            List<DruidIndicator> tenderIndicators = new ArrayList<>();
            for (DruidIndicator druidIndicator : indicators) {
                if (druidIndicator.getTenderOuterId().equals(tenderId)) {
                    tenderIndicators.add(druidIndicator);
                }
            }
            groupedIndicators.put(tenderId, tenderIndicators);
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

        DruidIndicator tenderIndicatorsData = null;
        switch (order) {
            case SortOrder.ASC: {
                tenderIndicatorsData = indicators.stream()
                        .sorted((o1, o2) -> o1.getDate().isBefore(o2.getDate()) ? 1 : -1)
                        .collect(Collectors.toList()).get(0);
                break;
            }
            case SortOrder.DESC: {
                Set<String> uniqueTenders = indicators.stream()
                        .map(DruidIndicator::getTenderOuterId)
                        .collect(Collectors.toSet());

                List<DruidIndicator> lastIndicators = new ArrayList<>();
                uniqueTenders.forEach(tenderOuterId -> {
                    Optional<DruidIndicator> optionalIndicator = indicators.stream()
                            .filter(indicator -> indicator.getTenderOuterId().equals(tenderOuterId))
                            .max((o1, o2) -> o1.getDate().isBefore(o2.getDate()) ? -1 : 1);
                    optionalIndicator.ifPresent(lastIndicators::add);
                });

                Optional<DruidIndicator> firstOptionalIndicator = lastIndicators.stream()
                        .sorted((o1, o2) -> o1.getDate().isBefore(o2.getDate()) ? -1 : 1)
                        .findFirst();

                if (firstOptionalIndicator.isPresent()) {
                    tenderIndicatorsData = firstOptionalIndicator.get();
                }
                break;
            }
            default: {
                throw new UnsupportedSortOrderException(order);
            }
        }

        ZonedDateTime nextPageStartDate = null;
        ZonedDateTime nextPageEndDate = null;
        if (tenderIndicatorsData != null) {
            nextPageStartDate = tenderIndicatorsData.getDate().plusNanos(1000000);
            nextPageEndDate = tenderIndicatorsData.getDate().minusNanos(1000000);
        }

        return PaginationUtils.createIndicatorsPage(startDate, endDate, nextPageStartDate,
                nextPageEndDate, order, limit, riskOnly, path, procedureTypes, indicatorIds);
    }
}
