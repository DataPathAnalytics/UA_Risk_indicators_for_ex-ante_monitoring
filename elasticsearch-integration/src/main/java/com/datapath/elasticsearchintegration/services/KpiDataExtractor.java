package com.datapath.elasticsearchintegration.services;

import com.datapath.elasticsearchintegration.constants.Aggregations;
import com.datapath.elasticsearchintegration.constants.ProcedureProperty;
import com.datapath.elasticsearchintegration.domain.FilterQuery;
import com.datapath.elasticsearchintegration.domain.KpiInfo;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.cardinality.InternalCardinality;
import org.elasticsearch.search.aggregations.metrics.sum.InternalSum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author vitalii
 */
@Service
public class KpiDataExtractor extends BaseDataExtractor {

    private final ProcedureFilterService filterService;

    private final TransportClient transportClient;

    @Autowired
    public KpiDataExtractor(TransportClient transportClient, ProcedureFilterService filterService) {
        this.transportClient = transportClient;
        this.filterService = filterService;
    }

    KpiInfo getData() {
        KpiInfo kpiInfo = new KpiInfo();
        setTotalKpiInfo(kpiInfo);
        setRiskedKpiInfo(kpiInfo);
        setMonitoringKpiInfo(kpiInfo);
        kpiInfo.setRiskProcuringEntitiesCount(countDistinctPEWithRisk());
        kpiInfo.setAllProcuringEntitiesCount(countDistinctPE());
        kpiInfo.setIndicatorsCount(countDistinctIndicators());
        kpiInfo.setRiskIndicatorsCount(countDistinctIndicatorsWithRisk());
        return kpiInfo;
    }

    KpiInfo getDataFiltered(FilterQuery filterQuery) {
        KpiInfo kpiInfo = new KpiInfo();
        setRiskedKpiInfo(kpiInfo, filterQuery);
        setTotalKpiInfo(kpiInfo, filterQuery);
        kpiInfo.setRiskProcuringEntitiesCount(countDistinctPEWithRisk(filterQuery));
        kpiInfo.setAllProcuringEntitiesCount(countDistinctPE(filterQuery));
        kpiInfo.setIndicatorsCount(countDistinctIndicators(filterQuery));
        kpiInfo.setRiskIndicatorsCount(countDistinctIndicatorsWithRisk(filterQuery));
        return kpiInfo;
    }

    private long countDistinctPEWithRisk(FilterQuery filterQuery) {
        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient)
                .setSize(0)
                .addAggregation(AggregationBuilders.cardinality(Aggregations.PROCURING_ENTITY_KIND.value()).field(ProcedureProperty.PROCURING_ENTITY_EDRPOU_KEYWORD.value()));
        BoolQueryBuilder boolQuery = filterService.getBoolQueryWithFilters(filterQuery);
        applyRiskFilter(boolQuery);
        searchRequestBuilder.setQuery(boolQuery);
        SearchResponse searchResponse = getSearchResponse(searchRequestBuilder);
        return ((InternalCardinality) searchResponse.getAggregations().get(Aggregations.PROCURING_ENTITY_KIND.value())).getValue();
    }

    private long countDistinctPEWithRisk() {
        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient)
                .setSize(0)
                .addAggregation(AggregationBuilders.cardinality(Aggregations.PROCURING_ENTITY_KIND.value()).field(ProcedureProperty.PROCURING_ENTITY_EDRPOU_KEYWORD.value()));
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        applyRiskFilter(boolQuery);
        searchRequestBuilder.setQuery(boolQuery);
        SearchResponse searchResponse = getSearchResponse(searchRequestBuilder);
        return ((InternalCardinality) searchResponse.getAggregations().get(Aggregations.PROCURING_ENTITY_KIND.value())).getValue();
    }

    private long countDistinctPE() {
        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient)
                .setSize(0)
                .addAggregation(AggregationBuilders.cardinality(Aggregations.PROCURING_ENTITY_KIND.value()).field(ProcedureProperty.PROCURING_ENTITY_EDRPOU_KEYWORD.value()));
        searchRequestBuilder.setQuery(QueryBuilders.matchAllQuery());
        SearchResponse searchResponse = getSearchResponse(searchRequestBuilder);
        return ((InternalCardinality) searchResponse.getAggregations().get(Aggregations.PROCURING_ENTITY_KIND.value())).getValue();
    }

    private long countDistinctPE(FilterQuery filterQuery) {
        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient)
                .setSize(0)
                .addAggregation(AggregationBuilders.cardinality(Aggregations.PROCURING_ENTITY_KIND.value()).field(ProcedureProperty.PROCURING_ENTITY_EDRPOU_KEYWORD.value()));
        searchRequestBuilder.setQuery(filterService.getBoolQueryWithFilters(filterQuery));
        SearchResponse searchResponse = getSearchResponse(searchRequestBuilder);
        return ((InternalCardinality) searchResponse.getAggregations().get(Aggregations.PROCURING_ENTITY_KIND.value())).getValue();
    }

    private long countDistinctIndicators(FilterQuery filterQuery) {
        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient)
                .setSize(0)
                .addAggregation(AggregationBuilders.cardinality(Aggregations.INDICATORS_WITH_RISK.value()).field(ProcedureProperty.INDICATORS_KEYWORD.value()));
        searchRequestBuilder.setQuery(filterService.getBoolQueryWithFilters(filterQuery));
        SearchResponse searchResponse = getSearchResponse(searchRequestBuilder);
        return ((InternalCardinality) searchResponse.getAggregations().get(Aggregations.INDICATORS_WITH_RISK.value())).getValue();
    }

    private long countDistinctIndicators() {
        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient)
                .setSize(0)
                .addAggregation(AggregationBuilders.cardinality(Aggregations.INDICATORS_WITH_RISK.value()).field(ProcedureProperty.INDICATORS_KEYWORD.value()));
        searchRequestBuilder.setQuery(QueryBuilders.matchAllQuery());
        SearchResponse searchResponse = getSearchResponse(searchRequestBuilder);
        return ((InternalCardinality) searchResponse.getAggregations().get(Aggregations.INDICATORS_WITH_RISK.value())).getValue();
    }

    private long countDistinctIndicatorsWithRisk(FilterQuery filterQuery) {
        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient)
                .setSize(0)
                .addAggregation(AggregationBuilders.cardinality(Aggregations.INDICATORS_WITH_RISK.value()).field(ProcedureProperty.INDICATORS_WITH_RISK_KEYWORD.value()));
        searchRequestBuilder.setQuery(filterService.getBoolQueryWithFilters(filterQuery));
        SearchResponse searchResponse = getSearchResponse(searchRequestBuilder);
        return ((InternalCardinality) searchResponse.getAggregations().get(Aggregations.INDICATORS_WITH_RISK.value())).getValue();
    }

    private long countDistinctIndicatorsWithRisk() {
        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient)
                .setSize(0)
                .addAggregation(AggregationBuilders.cardinality(Aggregations.INDICATORS_WITH_RISK.value()).field(ProcedureProperty.INDICATORS_WITH_RISK_KEYWORD.value()));
        searchRequestBuilder.setQuery(QueryBuilders.matchAllQuery());
        SearchResponse searchResponse = getSearchResponse(searchRequestBuilder);
        return ((InternalCardinality) searchResponse.getAggregations().get(Aggregations.INDICATORS_WITH_RISK.value())).getValue();
    }

    private SearchRequestBuilder getKPI(FilterQuery filterQuery, boolean withRisks) {
        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient)
                .setSize(0)
                .addAggregation(AggregationBuilders.sum(Aggregations.AMOUNT_OF_RISK.value()).field(ProcedureProperty.EXPECTED_VALUE.value()));
        BoolQueryBuilder boolQuery = filterService.getBoolQueryWithFilters(filterQuery);
        if (withRisks) {
            applyRiskFilter(boolQuery);
        }
        searchRequestBuilder.setQuery(boolQuery);
        return searchRequestBuilder;
    }

    private SearchRequestBuilder getKPI(boolean withRisks) {
        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient)
                .setSize(0)
                .addAggregation(AggregationBuilders.sum(Aggregations.AMOUNT_OF_RISK.value()).field(ProcedureProperty.EXPECTED_VALUE.value()));
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if (withRisks) {
            applyRiskFilter(boolQuery);
        }
        searchRequestBuilder.setQuery(boolQuery);
        return searchRequestBuilder;
    }

    private SearchRequestBuilder getMonitoringKPI() {
        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient)
                .setSize(0)
                .addAggregation(AggregationBuilders.sum(Aggregations.AMOUNT_OF_RISK.value()).field(ProcedureProperty.EXPECTED_VALUE.value()));
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.mustNot(QueryBuilders.termQuery(ProcedureProperty.MONITORING_STATUS_KEYWORD.value(), "None"));
        searchRequestBuilder.setQuery(boolQuery);
        return searchRequestBuilder;
    }

    private void setRiskedKpiInfo(KpiInfo kpiInfo) {
        SearchRequestBuilder searchRequestBuilder = getKPI(true);
        SearchResponse searchResponse = getSearchResponse(searchRequestBuilder);
        kpiInfo.setRiskProceduresCount(searchResponse.getHits().getTotalHits());
        kpiInfo.setRiskProceduresValue(((InternalSum) searchResponse.getAggregations().get(Aggregations.AMOUNT_OF_RISK.value())).getValue());
    }

    private void setMonitoringKpiInfo(KpiInfo kpiInfo) {
        SearchRequestBuilder searchRequestBuilder = getMonitoringKPI();
        SearchResponse searchResponse = getSearchResponse(searchRequestBuilder);
        kpiInfo.setMonitoringCount(searchResponse.getHits().getTotalHits());
        kpiInfo.setMonitoringValue(((InternalSum) searchResponse.getAggregations().get(Aggregations.AMOUNT_OF_RISK.value())).getValue());
    }

    private void setTotalKpiInfo(KpiInfo kpiInfo) {
        SearchRequestBuilder searchRequestBuilder = getKPI(false);
        SearchResponse searchResponse = getSearchResponse(searchRequestBuilder);
        kpiInfo.setProceduresCount(searchResponse.getHits().getTotalHits());
        kpiInfo.setProceduresValue(((InternalSum) searchResponse.getAggregations().get(Aggregations.AMOUNT_OF_RISK.value())).getValue());
    }

    private void setTotalKpiInfo(KpiInfo kpiInfo, FilterQuery filterQuery) {
        SearchRequestBuilder searchRequestBuilder = getKPI(filterQuery, false);
        SearchResponse searchResponse = getSearchResponse(searchRequestBuilder);
        kpiInfo.setProceduresCount(searchResponse.getHits().getTotalHits());
        kpiInfo.setProceduresValue(((InternalSum) searchResponse.getAggregations().get(Aggregations.AMOUNT_OF_RISK.value())).getValue());
    }

    private void setRiskedKpiInfo(KpiInfo kpiInfo, FilterQuery filterQuery) {
        SearchRequestBuilder searchRequestBuilder = getKPI(filterQuery, true);
        SearchResponse searchResponse = getSearchResponse(searchRequestBuilder);
        kpiInfo.setRiskProceduresCount(searchResponse.getHits().getTotalHits());
        kpiInfo.setRiskProceduresValue(((InternalSum) searchResponse.getAggregations().get(Aggregations.AMOUNT_OF_RISK.value())).getValue());
    }



}
