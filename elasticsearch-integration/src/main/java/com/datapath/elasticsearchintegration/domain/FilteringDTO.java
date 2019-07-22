package com.datapath.elasticsearchintegration.domain;

import com.datapath.elasticsearchintegration.domain.charts.ChartsDataWraper;
import lombok.Data;

/**
 * @author vitalii
 */
@Data
public class FilteringDTO {

    private ProceduresWrapper data;
    private KpiInfo kpiInfo;
    private KpiInfo kpiInfoFiltered;
    private ChartsDataWraper chartsDataWraper;
    private AvailableFilters availableFilters;
}
