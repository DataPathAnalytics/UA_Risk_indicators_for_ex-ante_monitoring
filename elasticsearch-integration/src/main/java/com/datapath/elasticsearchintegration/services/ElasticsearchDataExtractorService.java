package com.datapath.elasticsearchintegration.services;

import com.datapath.elasticsearchintegration.domain.FilterQuery;
import com.datapath.elasticsearchintegration.domain.FilteringDTO;
import com.datapath.elasticsearchintegration.domain.KeyValueObject;
import com.datapath.elasticsearchintegration.domain.KpiInfo;
import com.datapath.elasticsearchintegration.domain.charts.ChartsDataWraper;
import com.datapath.elasticsearchintegration.domain.charts.DynamicChartData;
import com.datapath.elasticsearchintegration.domain.charts.KPICharts;
import com.datapath.elasticsearchintegration.domain.charts.TopRegion;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author vitalii
 */
@Slf4j
@Service
public class ElasticsearchDataExtractorService extends BaseDataExtractor {

    private final ObjectMapper objectMapper;
    private final KpiDataExtractor kpiDataExtractor;
    private final ChartsDataExtractor chartsDataExtractor;
    private final ProcedureFilterService procedureFilterService;


    @Autowired
    public ElasticsearchDataExtractorService(KpiDataExtractor kpiDataExtractor, ChartsDataExtractor chartsDataExtractor, ProcedureFilterService procedureFilterService, ObjectMapper objectMapper) {
        this.kpiDataExtractor = kpiDataExtractor;
        this.chartsDataExtractor = chartsDataExtractor;
        this.procedureFilterService = procedureFilterService;
        this.objectMapper = objectMapper;
    }

    public ChartsDataWraper getChartsDataWrapper(FilterQuery filterQuery) {
        return ChartsDataWraper.builder()
                .top10ProcuringEntities(getTop10ProcuringEntity(filterQuery))
                .top10ProcuringEntitiesAmount(getTop10ProcuringEntityAmount(filterQuery))
                .top10Cpv(getTop10Cpv(filterQuery))
                .top10CpvAmount(getTop10CpvAmount(filterQuery))
                .top10Regions(getTop10Regions(filterQuery))
                .top10RegionsAmount(getTop10RegionsAmount(filterQuery))
                .risksByProceduresTable(getProcedureByRiskTable(filterQuery))
                .risksByProceduresTableAmount(getProcedureByRiskTableAmount(filterQuery))
                .top10RiskIndicators(getTop10RiskIndicators(filterQuery))
                .top10RiskIndicatorsAmount(getTop10RiskIndicatorsAmount(filterQuery))
                .dynamicChartData(getDynamicChartData(filterQuery))
                .dynamicChartDataAmount(getDynamicChartDataAmount(filterQuery))
                .proceduresByPurchaseMethod(getGroupingByPurchaseMethod(filterQuery))
                .proceduresByPurchaseMethodAmount(getGroupingByPurchaseMethodAmount(filterQuery))
                .kpiCharts(getDataForKPICharts())
                .build();
    }

    public KpiInfo getKpiInfo() {
        return kpiDataExtractor.getData();
    }

    public KpiInfo getKpiInfoFiltered(FilterQuery filterQuery) {
        return kpiDataExtractor.getDataFiltered(filterQuery);
    }

    private List<KeyValueObject> getTop10ProcuringEntity(FilterQuery filterQuery) {
        return chartsDataExtractor.getTop10ProcuringEntity(filterQuery);
    }

    private List<KeyValueObject> getTop10ProcuringEntityAmount(FilterQuery filterQuery) {
        return chartsDataExtractor.getTop10ProcuringEntityAmount(filterQuery);
    }

    private List<KeyValueObject> getTop10Cpv(FilterQuery filterQuery) {
        return chartsDataExtractor.getTop10Cpv(filterQuery);
    }

    private List<KeyValueObject> getTop10CpvAmount(FilterQuery filterQuery) {
        return chartsDataExtractor.getTop10CpvAmount(filterQuery);
    }

    private List<TopRegion> getTop10Regions(FilterQuery filterQuery) {
        return chartsDataExtractor.getTop10RegionByRiskProcedureCount(filterQuery);
    }

    private List<TopRegion> getTop10RegionsAmount(FilterQuery filterQuery) {
        return chartsDataExtractor.getTop10RegionByRiskProcedureAmount(filterQuery);
    }

    private List<KeyValueObject> getProcedureByRiskTable(FilterQuery filterQuery) {
        return chartsDataExtractor.getProcedureByRiskTable(filterQuery);
    }

    private List<KeyValueObject> getProcedureByRiskTableAmount(FilterQuery filterQuery) {
        return chartsDataExtractor.getProcedureByRiskTableAmount(filterQuery);
    }

    private List<DynamicChartData> getDynamicChartData(FilterQuery filterQuery) {
        return chartsDataExtractor.getDynamicOfGrowingProceduresCount(filterQuery);
    }

    private List<DynamicChartData> getDynamicChartDataAmount(FilterQuery filterQuery) {
        return chartsDataExtractor.getDynamicOfGrowingProceduresAmount(filterQuery);
    }

    private List<KeyValueObject> getGroupingByPurchaseMethod(FilterQuery filterQuery) {
        return chartsDataExtractor.getProceduresGroupByPurchaseMethod(filterQuery);
    }

    private List<KeyValueObject> getGroupingByPurchaseMethodAmount(FilterQuery filterQuery) {
        return chartsDataExtractor.getProceduresGroupByPurchaseMethodAmount(filterQuery);
    }

    private KPICharts getDataForKPICharts() {
        return chartsDataExtractor.proceduresCountMonthly();
    }

    private List<KeyValueObject> getTop10RiskIndicators(FilterQuery filterQuery) {
        return chartsDataExtractor.getTop10RiskIndicators(filterQuery);
    }

    private List<KeyValueObject> getTop10RiskIndicatorsAmount(FilterQuery filterQuery) {
        return chartsDataExtractor.getTop10RiskIndicatorsAmount(filterQuery);
    }

    public FilteringDTO applyFilter(FilterQuery filterQuery) {
        return procedureFilterService.filter(filterQuery);
    }

    public Object getFilterData(FilterQuery filterQuery) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        ArrayNode arrayNode = objectMapper.createArrayNode();
        List<KeyValueObject> filterData = procedureFilterService.getFilterData(filterQuery);
        for (KeyValueObject filterDatum : filterData) {
            ObjectNode localNode = objectMapper.createObjectNode();
            localNode.put("key", filterDatum.getKey().toString());
            localNode.put("value", filterDatum.getValue().toString());
            arrayNode.add(localNode);
        }
        objectNode.set(filterQuery.getSearchField(), arrayNode);
        return objectNode;
    }

    public Object checkAll(FilterQuery filterQuery) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        ArrayNode arrayNode = objectMapper.createArrayNode();
        int returnSize;
        do {
            List<String> filterData = procedureFilterService.checkAll(filterQuery);
            for (String filterDatum : filterData) {
                arrayNode.add(filterDatum);
            }
            returnSize = filterData.size();
            filterQuery.setPage(filterQuery.getPage() + 1);
        } while (returnSize == filterQuery.getSize());
        objectNode.set("tenderIds", arrayNode);
        return objectNode;
    }
}
