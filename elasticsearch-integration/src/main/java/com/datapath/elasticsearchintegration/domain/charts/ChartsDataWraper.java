package com.datapath.elasticsearchintegration.domain.charts;

import com.datapath.elasticsearchintegration.domain.KeyValueObject;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author vitalii
 */
@Data
@Builder
public class ChartsDataWraper {
    private List<DynamicChartData> dynamicChartData;
    private List<DynamicChartData> dynamicChartDataAmount;
    private List<KeyValueObject> top10ProcuringEntities;
    private List<KeyValueObject> top10ProcuringEntitiesAmount;
    private List<KeyValueObject> top10RiskIndicators;
    private List<KeyValueObject> top10RiskIndicatorsAmount;
    private List<KeyValueObject> proceduresByPurchaseMethod;
    private List<KeyValueObject> proceduresByPurchaseMethodAmount;
    private List<KeyValueObject> risksByProceduresTable;
    private List<KeyValueObject> risksByProceduresTableAmount;
    private List<TopRegion> top10Regions;
    private List<TopRegion> top10RegionsAmount;
    private List<KeyValueObject> top10Cpv;
    private List<KeyValueObject> top10CpvAmount;
    private KPICharts kpiCharts;
}
