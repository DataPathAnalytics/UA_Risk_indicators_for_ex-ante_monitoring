package com.datapath.web.services;

import com.datapath.web.domain.DruidIndicator;
import org.springframework.lang.Nullable;

import java.time.ZonedDateTime;
import java.util.List;

public interface IndicatorService {

    List<DruidIndicator> getIndicators(String id);

    @Nullable
    List<DruidIndicator> getIndicators(ZonedDateTime startDate,
                                       ZonedDateTime endDate,
                                       Integer limit,
                                       String order,
                                       Boolean riskOnly,
                                       List<String> procedureTypes,
                                       List<String> indicatorIds);
}
