package com.datapath.web.services.impl;

import com.datapath.druidintegration.model.TendersFilter;
import com.datapath.druidintegration.service.ExtractTenderDataService;
import com.datapath.web.api.rest.exceptions.TenderNotFoundException;
import com.datapath.web.domain.DruidIndicator;
import com.datapath.web.mappers.DruidEventMapper;
import com.datapath.web.services.IndicatorService;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DruidTenderIndicatorService implements IndicatorService {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss[.SSS]XXX";

    private ExtractTenderDataService extractDataService;

    public DruidTenderIndicatorService(ExtractTenderDataService extractDataService) {
        this.extractDataService = extractDataService;
    }

    @Override
    public List<DruidIndicator> getIndicators(String tenderId) {
        TendersFilter tenderIdFilter = TendersFilter.builder().tenderId(tenderId).build();
        List<DruidIndicator> tenderIndicators = extractDataService.getTenderDataByTenderId(tenderIdFilter)
                .stream()
                .map(DruidEventMapper::mapToIndicator)
                .collect(Collectors.toList());
        if (tenderIndicators.isEmpty()) {
            throw new TenderNotFoundException(tenderId);
        }
        return tenderIndicators;
    }

    @Override
    public List<DruidIndicator> getIndicators(ZonedDateTime startDate,
                                              ZonedDateTime endDate,
                                              Integer limit,
                                              String order,
                                              Boolean riskOnly,
                                              List<String> procedureTypes,
                                              List<String> indicatorIds) {

        TendersFilter tendersFilter = TendersFilter.builder()
                .procedureTypes(procedureTypes)
                .indicatorIds(indicatorIds)
                .build();

        String formattedStartDate = startDate.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        String formattedEndDate = endDate.format(DateTimeFormatter.ofPattern(DATE_FORMAT));

        return extractDataService.getTimePeriodTenderData(
                formattedStartDate,
                formattedEndDate,
                limit,
                order,
                riskOnly,
                tendersFilter
        ).stream()
                .map(DruidEventMapper::mapToIndicator)
                .collect(Collectors.toList());
    }
}
