package com.datapath.elasticsearchintegration.services;

import com.datapath.elasticsearchintegration.constants.Aggregations;
import com.datapath.elasticsearchintegration.constants.ProcedureProperty;
import com.datapath.elasticsearchintegration.constants.RiskedProcedure;
import com.datapath.elasticsearchintegration.constants.TenderScoreRank;
import com.datapath.elasticsearchintegration.domain.*;
import com.datapath.elasticsearchintegration.exception.NoDataFilteredException;
import com.datapath.elasticsearchintegration.util.Mapping;
import com.datapath.persistence.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter;
import org.elasticsearch.search.aggregations.bucket.terms.DoubleTerms;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.datapath.elasticsearchintegration.constants.Constants.DATE_FORMAT;
import static com.datapath.elasticsearchintegration.constants.ProcedureProperty.*;
import static com.datapath.elasticsearchintegration.constants.RiskedProcedure.*;

/**
 * @author vitalii
 */
@Service
@Slf4j
public class ProcedureFilterService extends BaseDataExtractor {

    private final TransportClient transportClient;

    public ProcedureFilterService(TransportClient transportClient) {
        this.transportClient = transportClient;
    }

    public FilteringDTO filter(FilterQuery filterQuery) {
        SearchRequestBuilder searchRequestBuilder = getSearchRequestWithFilters(filterQuery);
        searchRequestBuilder.setFrom(filterQuery.getPage() * filterQuery.getSize());
        searchRequestBuilder.setSize(filterQuery.getSize());
        addAggregationForAvailableFilters(searchRequestBuilder);
        SearchResponse searchResponse = getSearchResponse(searchRequestBuilder);
        List<TenderIndicatorsCommonInfo> results = responseToEntities(searchResponse);
        FilteringDTO filteringDTO = new FilteringDTO();
        filteringDTO.setAvailableFilters(getAvailableFilters(searchResponse));
        ProceduresWrapper proceduresWrapper = new ProceduresWrapper();
        proceduresWrapper.setProcedures(results);
        proceduresWrapper.setTotalCount(searchResponse.getHits().getTotalHits());
        filteringDTO.setData(proceduresWrapper);
        if (CollectionUtils.isEmpty(proceduresWrapper.getProcedures())) {
            throw new NoDataFilteredException();
        }
        for (TenderIndicatorsCommonInfo procedure : proceduresWrapper.getProcedures()) {
            if (procedure.getProcedureType().contains("quick")) {
                procedure.setProcedureType(Mapping.PROCEDURE_TYPES.get("negotiation").getValue().toString());
            } else {
                procedure.setProcedureType(Mapping.PROCEDURE_TYPES.get(procedure.getProcedureType()).getValue().toString());
            }
            procedure.setTenderStatus(Mapping.TENDER_STATUS.get(procedure.getTenderStatus()).getValue().toString());
            procedure.setProcuringEntityKind(Mapping.PROCURING_ENTITY_KIND.get(procedure.getProcuringEntityKind()).getValue().toString());
            procedure.setGsw(Mapping.GSW.get(procedure.getGsw()).getValue().toString());
            procedure.setMonitoringStatus(Mapping.MONITORING_STATUS.get(procedure.getMonitoringStatus()).getValue().toString());
            procedure.setMonitoringAppealAsString(Mapping.APPEAL.get(Boolean.valueOf(procedure.isMonitoringAppeal()).toString()).getValue().toString());
            procedure.setIndicatorsWithRiskMapped(new ArrayList<>());
            for (String risk : procedure.getIndicatorsWithRisk()) {
                procedure.getIndicatorsWithRiskMapped().add(new KeyValueObject(risk, Mapping.RISK_INDICATORS.get(risk)));
            }
        }

        return filteringDTO;
    }

    public List<TenderIndicatorsCommonInfo> getForExport(List<String> tenderIds) {

        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient);
        searchRequestBuilder.setQuery(filterByTenderIds(tenderIds));
        searchRequestBuilder.setSize(10000);
        SearchResponse searchResponse = getSearchResponse(searchRequestBuilder);
        return responseToEntities(searchResponse);
    }

    List<KeyValueObject> getFilterData(FilterQuery filterQuery) {
        BoolQueryBuilder boolQuery = getBoolQueryWithFilters(filterQuery);

        BoolQueryBuilder mainQuery = QueryBuilders.boolQuery();

        String searchField = extractSearchField(filterQuery);
        mainQuery.should(QueryBuilders.regexpQuery(searchField + ".keyword", filterQuery.getSearchValue() + ".*"));
        mainQuery.should(QueryBuilders.regexpQuery(getCodeSearchField(searchField) + ".keyword", filterQuery.getSearchValue() + ".*"));
        mainQuery.should(QueryBuilders.regexpQuery(getCodeSearchField(searchField) + ".keyword", "UA-EDR-" + filterQuery.getSearchValue() + ".*"));
        mainQuery.minimumShouldMatch(1);
        boolQuery.must(mainQuery);

        int searchCount = filterQuery.getSearchCount() > 0 ? filterQuery.getSearchCount() : 15;
        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient);
        searchRequestBuilder.setQuery(boolQuery);
        searchRequestBuilder.addSort(DATE_PUBLISHED.value(), SortOrder.DESC);
        searchRequestBuilder.addAggregation(AggregationBuilders.terms(Aggregations.TEMP.value())
                .field(searchField + ".keyword").size(searchCount).subAggregation(AggregationBuilders.terms(Aggregations.CODE_SUB_AGGREGATION.value())
                        .field(getCodeSearchField(searchField) + ".keyword")));
        SearchResponse searchResponse = getSearchResponse(searchRequestBuilder);
        return parseNameAndCodeAggregationResultsToKeyValueList(searchResponse, Aggregations.TEMP.value());
    }

    List<String> checkAll(FilterQuery filterQuery) {
        BoolQueryBuilder boolQuery = getBoolQueryWithFilters(filterQuery);

        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient);
        searchRequestBuilder.setQuery(boolQuery);
        searchRequestBuilder.setFetchSource(new String[]{"tenderId"}, new String[]{});
        searchRequestBuilder.setSize(filterQuery.getSize());
        searchRequestBuilder.setFrom(filterQuery.getPage() * filterQuery.getSize());
        SearchResponse searchResponse = getSearchResponse(searchRequestBuilder);
        return Arrays.stream(searchResponse.getHits().getHits()).map(item -> item.getSourceAsMap().get("tenderId").toString()).collect(Collectors.toList());
    }

    private String extractSearchField(FilterQuery filterQuery) {
        return filterQuery.getSearchField().contains("cpvName") ? "cpvName" : filterQuery.getSearchField().contains("cpv2Name") ? "cpv2Name" : "procuringEntityName";
    }

    private String getCodeSearchField(String nameSearchField) {
        if (nameSearchField.equals("cpvName")) {
            return "cpv";
        } else if (nameSearchField.equals("cpv2Name")) {
            return "cpv2";
        } else {
            return "procuringEntityEDRPOU";
        }
    }

    private SearchRequestBuilder getSearchRequestWithFilters(FilterQuery filterQuery) {
        BoolQueryBuilder mainQuery = getBoolQueryWithFilters(filterQuery);
        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient);
        searchRequestBuilder.setQuery(mainQuery);
        searchRequestBuilder.addSort(fetchSortField(filterQuery.getSortField()), filterQuery.getSortDirection().equals("ASC") ? SortOrder.ASC : SortOrder.DESC);
        return searchRequestBuilder;
    }

    private String fetchSortField(String sortField) {
        if (Mapping.SORT_FIELDS_WITH_KEYWORD.contains(sortField)) {
            return sortField + ".keyword";
        }

        if (Mapping.SORT_FIELDS_WITHOUT_KEYWORD.contains(sortField)) {
            return sortField;
        }

        return "datePublished";
    }

    BoolQueryBuilder getBoolQueryWithFilters(FilterQuery filterQuery) {
        BoolQueryBuilder mainQuery = QueryBuilders.boolQuery();
        addAllFilters(filterQuery, mainQuery);
        applyDateRange(DateUtils.formatToString(filterQuery.getStartDate(), DATE_FORMAT), DateUtils.formatToString(filterQuery.getEndDate(), DATE_FORMAT), mainQuery);
        return mainQuery;
    }

    private void addAllFilters(FilterQuery filterQuery, BoolQueryBuilder mainQuery) {
        if (StringUtils.isNotEmpty(filterQuery.getProcedureId())) {
            filterByTenderId(filterQuery, mainQuery);
        } else {
            filterByRiskedIndicators(filterQuery, mainQuery);
            filterRiskedProcedure(filterQuery, mainQuery);
            filterRiskedProcedureByRank(filterQuery, mainQuery);
            filterByRegion(filterQuery, mainQuery);
            filterByMonitoringOffice(filterQuery, mainQuery);
            filterByCpv2Name(filterQuery, mainQuery);
            filterByProcedureType(filterQuery, mainQuery);
            filterByGsw(filterQuery, mainQuery);
            filterByCpvName(filterQuery, mainQuery);
            filterByMonitoringStatus(filterQuery, mainQuery);
            filterByProcuringEntities(filterQuery, mainQuery);
            filterProcuringEntityType(filterQuery, mainQuery);
            filterByComplaints(filterQuery, mainQuery);
            filterByTenderStatus(filterQuery, mainQuery);
            filterByExpectedValue(filterQuery, mainQuery);
            filterByCurrency(filterQuery, mainQuery);
            filterByMonitoringCause(filterQuery, mainQuery);
            filterByMonitoringAppeal(filterQuery, mainQuery);
        }
    }

    private AvailableFilters getAvailableFilters(SearchResponse searchResponse) {
        AvailableFilters availableFilters = new AvailableFilters();
        availableFilters.setRegions(parseStringTermsAggregationResultsToKeyValueList(searchResponse, Aggregations.REGION.value()));
        availableFilters.setMonitoringOffices(parseStringTermsAggregationResultsToKeyValueList(searchResponse, Aggregations.MONITORING_OFFICE.value()));
        availableFilters.setGsw(parseStringTermsAggregationResultsToKeyValueList(searchResponse, Aggregations.GSW.value()));
        availableFilters.setTenderStatuses(parseStringTermsAggregationResultsToKeyValueList(searchResponse, Aggregations.TENDER_STATUS.value()));
        availableFilters.setProcedureTypes(parseStringTermsAggregationResultsToKeyValueList(searchResponse, Aggregations.PROCEDURE_TYPE.value()));
        availableFilters.setComplaints(parseLongTermsAggregationResultsToKeyValueList(searchResponse, Aggregations.COMPLAINS.value()));
        availableFilters.setMonitoringStatus(parseStringTermsAggregationResultsToKeyValueList(searchResponse, Aggregations.MONITORING_STATUS.value()));
        availableFilters.setRiskedIndicators(parseRiskStringTermsAggregationResultsToKeyValueList(searchResponse, Aggregations.INDICATORS_WITH_RISK.value()));
        availableFilters.setProcuringEntityKind(parseStringTermsAggregationResultsToKeyValueList(searchResponse, Aggregations.PROCURING_ENTITY_KIND.value()));
        availableFilters.setProcuringEntities(parseNameAndCodeAggregationResultsToKeyValueList(searchResponse, Aggregations.PROCURING_ENTITY_NAME.value()));
        availableFilters.setCpvNames(parseNameAndCodeAggregationResultsToKeyValueList(searchResponse, Aggregations.CPV_NAME.value()));
        availableFilters.setCpv2Names(parseNameAndCodeAggregationResultsToKeyValueList(searchResponse, Aggregations.CPV2_NAME.value()));
        availableFilters.setCurrency(parseStringTermsAggregationResultsToKeyValueList(searchResponse, Aggregations.CURRENCY.value()));
        availableFilters.setTenderScoreRank(parseDoubleTermsAggregationResultsToKeyValueList(searchResponse, Aggregations.TENDER_RISK_SCORE.value()));
        availableFilters.setMonitoringAppeal(parseLongTermsAggregationResultsToKeyValueList(searchResponse, Aggregations.MONITORING_APPEAL.value()));
        availableFilters.setMonitoringCause(parseStringTermsAggregationResultsToKeyValueList(searchResponse, Aggregations.MONITORING_CAUSE.value()));
        availableFilters.setRiskedProcedures(getRiskedProceduresAvailableFilters(searchResponse));

        return availableFilters;
    }

    private List<KeyValueObject> getRiskedProceduresAvailableFilters(SearchResponse searchResponse) {
        List<KeyValueObject> results = new ArrayList<>();
        addRiskedProcedureIfAvailable(searchResponse, results, WITH_RISK);
        addRiskedProcedureIfAvailable(searchResponse, results, WITHOUT_RISK);
        addRiskedProcedureIfAvailable(searchResponse, results, WITH_RISK_HAS_PRIORITY);
        addRiskedProcedureIfAvailable(searchResponse, results, WITH_RISK_NO_PRIORITY);
        return results;
    }

    private void addRiskedProcedureIfAvailable(SearchResponse searchResponse, List<KeyValueObject> results, RiskedProcedure procedure) {
        long count = parseInternalFilterAggregationDocCount(searchResponse, procedure.value());
        if (count > 0) {
            results.add(new KeyValueObject(procedure.value(), count));
        }
    }


    private long parseInternalFilterAggregationDocCount(SearchResponse searchResponse, String aggregationName) {
        return ((InternalFilter) searchResponse
                .getAggregations()
                .get(aggregationName)).getDocCount();
    }

    private List<KeyValueObject> parseLongTermsAggregationResultsToKeyValueList(SearchResponse searchResponse, String aggregationName) {
        return ((LongTerms) searchResponse
                .getAggregations()
                .get(aggregationName)).getBuckets()
                .stream()
                .map(bucket -> new KeyValueObject(bucket.getKeyAsString(), bucket.getDocCount()))
                .collect(Collectors.toList());
    }

    private List<KeyValueObject> parseStringTermsAggregationResultsToKeyValueList(SearchResponse searchResponse, String aggregationName) {
        return ((StringTerms) searchResponse
                .getAggregations()
                .get(aggregationName)).getBuckets()
                .stream()
                .map(bucket -> new KeyValueObject(bucket.getKeyAsString(), bucket.getDocCount()))
                .collect(Collectors.toList());
    }

    private List<KeyValueObject> parseRiskStringTermsAggregationResultsToKeyValueList(SearchResponse searchResponse, String aggregationName) {
        return ((StringTerms) searchResponse
                .getAggregations()
                .get(aggregationName)).getBuckets()
                .stream()
                .map(bucket -> new KeyValueObject(bucket.getKeyAsString(), bucket.getKeyAsString() + " - " + Mapping.RISK_INDICATORS.get(bucket.getKeyAsString())))
                .collect(Collectors.toList());
    }

    private List<KeyValueObject> parseNameAndCodeAggregationResultsToKeyValueList(SearchResponse searchResponse, String aggregationName) {
        return ((StringTerms) searchResponse
                .getAggregations()
                .get(aggregationName)).getBuckets()
                .stream()
                .map(bucket -> new KeyValueObject(
                        bucket.getKeyAsString(),
                        ((StringTerms) bucket.getAggregations().get(Aggregations.CODE_SUB_AGGREGATION.value()))
                                .getBuckets().get(0).getKeyAsString() + " - " + bucket.getKeyAsString()))
                .collect(Collectors.toList());
    }

    private List<KeyValueObject> parseDoubleTermsAggregationResultsToKeyValueList(SearchResponse searchResponse, String aggregationName) {
        DoubleTerms doubleTerms = searchResponse
                .getAggregations()
                .get(aggregationName);

        List<KeyValueObject> aggResult = new ArrayList<>();
        for (DoubleTerms.InternalBucket bucket : doubleTerms.getBuckets()) {
            aggResult.add(new KeyValueObject(Math.round(Double.parseDouble(bucket.getKeyAsString()) * 100.0) / 100.0, bucket.getDocCount()));
        }
        return aggResult;
    }


    private void addAggregationForAvailableFilters(SearchRequestBuilder searchRequestBuilder) {
        searchRequestBuilder.addAggregation(AggregationBuilders.terms(Aggregations.REGION.value())
                .field(ProcedureProperty.REGION_KEYWORD.value()).size(25));

        searchRequestBuilder.addAggregation(AggregationBuilders.terms(Aggregations.MONITORING_OFFICE.value())
                .field(MONITORING_OFFICE_KEYWORD.value()).size(50));

        searchRequestBuilder.addAggregation(AggregationBuilders.terms(Aggregations.MONITORING_STATUS.value())
                .field(ProcedureProperty.MONITORING_STATUS_KEYWORD.value()).size(15));

        searchRequestBuilder.addAggregation(AggregationBuilders.terms(Aggregations.GSW.value())
                .field(ProcedureProperty.GSW_KEYWORD.value()));

        searchRequestBuilder.addAggregation(AggregationBuilders.terms(Aggregations.TENDER_STATUS.value())
                .field(ProcedureProperty.TENDER_STATUS_KEYWORD.value()));

        searchRequestBuilder.addAggregation(AggregationBuilders.terms(Aggregations.PROCEDURE_TYPE.value())
                .field(ProcedureProperty.PROCEDURE_TYPE_KEYWORD.value()));

        searchRequestBuilder.addAggregation(AggregationBuilders.terms(Aggregations.COMPLAINS.value())
                .field(ProcedureProperty.HAS_COMPLAINS.value()));

        searchRequestBuilder.addAggregation(AggregationBuilders.terms(Aggregations.MONITORING_APPEAL.value())
                .field(ProcedureProperty.MONITORING_APPEAL.value()));

        searchRequestBuilder.addAggregation(AggregationBuilders.terms(Aggregations.MONITORING_CAUSE.value())
                .field(ProcedureProperty.MONITORING_CAUSE.value()));

        searchRequestBuilder.addAggregation(AggregationBuilders.terms(Aggregations.INDICATORS_WITH_RISK.value())
                .field(ProcedureProperty.INDICATORS_WITH_RISK_KEYWORD.value()).size(100));

        searchRequestBuilder.addAggregation(AggregationBuilders.terms(Aggregations.PROCURING_ENTITY_KIND.value())
                .field(ProcedureProperty.PROCURING_ENTITY_KIND_KEYWORD.value()).size(10));

        searchRequestBuilder.addAggregation(AggregationBuilders.terms(Aggregations.PROCURING_ENTITY_NAME.value())
                .field(ProcedureProperty.PROCURING_ENTITY_NAME_KEYWORD.value()).size(10).
                        subAggregation(
                                AggregationBuilders.terms(Aggregations.CODE_SUB_AGGREGATION.value())
                                        .field(ProcedureProperty.PROCURING_ENTITY_EDRPOU_KEYWORD.value())
                        )
        );

        searchRequestBuilder.addAggregation(AggregationBuilders.terms(Aggregations.CPV_NAME.value())
                .field(ProcedureProperty.CPV_NAME_KEYWORD.value()).size(10).subAggregation(
                        AggregationBuilders.terms(Aggregations.CODE_SUB_AGGREGATION.value())
                                .field(CPV_KEYWORD.value())
                ));

        searchRequestBuilder.addAggregation(AggregationBuilders.terms(Aggregations.CPV2_NAME.value())
                .field(ProcedureProperty.CPV2_NAME_KEYWORD.value()).size(10).subAggregation(
                        AggregationBuilders.terms(Aggregations.CODE_SUB_AGGREGATION.value())
                                .field(CPV2_KEYWORD.value())
                ));

        searchRequestBuilder.addAggregation(AggregationBuilders.terms(Aggregations.CURRENCY.value())
                .field(ProcedureProperty.CURRENCY_KEYWORD.value()).size(101));

        searchRequestBuilder.addAggregation(AggregationBuilders.terms(Aggregations.TENDER_RISK_SCORE.value())
                .field(TENDER_RISK_SCORE.value()).size(100));

        searchRequestBuilder.addAggregation(AggregationBuilders.filter(WITH_RISK.value(), QueryBuilders.rangeQuery(TENDER_RISK_SCORE.value()).gt(0)));
        searchRequestBuilder.addAggregation(AggregationBuilders.filter(WITHOUT_RISK.value(), QueryBuilders.boolQuery().mustNot(QueryBuilders.rangeQuery(TENDER_RISK_SCORE.value()).gt(0))));

        searchRequestBuilder.addAggregation(AggregationBuilders.filter(WITH_RISK_HAS_PRIORITY.value(), QueryBuilders.boolQuery()
                .must(QueryBuilders.rangeQuery(TENDER_RISK_SCORE.value()).gt(0))
                .must(QueryBuilders.termQuery(HAS_PRIORITY_STATUS.value(), true))));

        searchRequestBuilder.addAggregation(AggregationBuilders.filter(WITH_RISK_NO_PRIORITY.value(), QueryBuilders.boolQuery()
                .must(QueryBuilders.rangeQuery(TENDER_RISK_SCORE.value()).gt(0))
                .mustNot(QueryBuilders.termQuery(HAS_PRIORITY_STATUS.value(), true))));

    }

    private void filterByTenderId(FilterQuery filterQuery, BoolQueryBuilder mainQuery) {
        mainQuery.must(QueryBuilders.matchPhraseQuery(ProcedureProperty.TENDER_ID_KEYWORD.value(), filterQuery.getProcedureId()));
    }

    private void filterByGsw(FilterQuery filterQuery, BoolQueryBuilder mainQuery) {
        if (StringUtils.isNotEmpty(filterQuery.getGsw())) {
            mainQuery.must(QueryBuilders.termQuery(GSW_KEYWORD.value(), filterQuery.getGsw()));
        }
    }

    private void filterProcuringEntityType(FilterQuery filterQuery, BoolQueryBuilder mainQuery) {
        if (StringUtils.isNotEmpty(filterQuery.getProcuringEntityKind())) {
            mainQuery.must(QueryBuilders.termQuery(PROCURING_ENTITY_KIND_KEYWORD.value(), filterQuery.getProcuringEntityKind()));
        }
    }

    private void filterByMonitoringStatus(FilterQuery filterQuery, BoolQueryBuilder mainQuery) {

        if (!CollectionUtils.isEmpty(filterQuery.getMonitoringStatus())) {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            for (String office : filterQuery.getMonitoringStatus()) {
                if (StringUtils.isNotEmpty(office)) {
                    boolQuery.should(QueryBuilders.termQuery(ProcedureProperty.MONITORING_STATUS_KEYWORD.value(), office));
                }
            }
            boolQuery.minimumShouldMatch(1);
            mainQuery.must(boolQuery);
        }
    }

    private void filterByComplaints(FilterQuery filterQuery, BoolQueryBuilder mainQuery) {
        if (filterQuery.getComplaints() != null) {
            mainQuery.must(QueryBuilders.termQuery(ProcedureProperty.HAS_COMPLAINS.value(), filterQuery.getComplaints()));
        }
    }

    private void filterByMonitoringAppeal(FilterQuery filterQuery, BoolQueryBuilder mainQuery) {
        if (filterQuery.getMonitoringAppeal() != null) {
            mainQuery.must(QueryBuilders.termQuery(ProcedureProperty.MONITORING_APPEAL.value(), filterQuery.getMonitoringAppeal()));
        }
    }

    private void filterByExpectedValue(FilterQuery filterQuery, BoolQueryBuilder mainQuery) {
        mainQuery.must(QueryBuilders.rangeQuery(EXPECTED_VALUE.value())
                .gte(Optional.ofNullable(filterQuery.getMinExpectedValue()).orElse(0L))
                .lte(Optional.ofNullable(filterQuery.getMaxExpectedValue()).orElse(Long.parseLong(String.valueOf(Integer.MAX_VALUE)))));
    }

    private void filterByRegion(FilterQuery filterQuery, BoolQueryBuilder mainQuery) {
        if (!CollectionUtils.isEmpty(filterQuery.getRegions())) {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            for (String region : filterQuery.getRegions()) {
                if (StringUtils.isNotEmpty(region)) {
                    boolQuery.should(QueryBuilders.termQuery(ProcedureProperty.REGION_KEYWORD.value(), region));
                }
            }
            boolQuery.minimumShouldMatch(1);
            mainQuery.must(boolQuery);
        }

    }

    private BoolQueryBuilder filterByTenderIds(List<String> tenderIds) {
        if (!CollectionUtils.isEmpty(tenderIds)) {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            for (String id : tenderIds) {
                if (StringUtils.isNotEmpty(id)) {
                    boolQuery.should(QueryBuilders.termQuery(TENDER_ID_KEYWORD.value(), id));
                }
            }
            boolQuery.minimumShouldMatch(1);
            return QueryBuilders.boolQuery().must(boolQuery);
        }
        return QueryBuilders.boolQuery();
    }

    private void filterByMonitoringOffice(FilterQuery filterQuery, BoolQueryBuilder mainQuery) {
        if (!CollectionUtils.isEmpty(filterQuery.getMonitoringOffices())) {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            for (String office : filterQuery.getMonitoringOffices()) {
                if (StringUtils.isNotEmpty(office)) {
                    boolQuery.should(QueryBuilders.termQuery(MONITORING_OFFICE_KEYWORD.value(), office));
                }
            }
            boolQuery.minimumShouldMatch(1);
            mainQuery.must(boolQuery);
        }

    }

    private void filterByMonitoringCause(FilterQuery filterQuery, BoolQueryBuilder mainQuery) {
        if (!CollectionUtils.isEmpty(filterQuery.getMonitoringCause())) {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            for (String cause : filterQuery.getMonitoringCause()) {
                if (StringUtils.isNotEmpty(cause)) {
                    boolQuery.should(QueryBuilders.termQuery(ProcedureProperty.MONITORING_CAUSE.value(), cause));
                }
            }
            boolQuery.minimumShouldMatch(1);
            mainQuery.must(boolQuery);
        }

    }

    private void filterByProcuringEntities(FilterQuery filterQuery, BoolQueryBuilder mainQuery) {
        if (!CollectionUtils.isEmpty(filterQuery.getProcuringEntities())) {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            for (String namePE : filterQuery.getProcuringEntities()) {
                if (StringUtils.isNotEmpty(namePE)) {
                    boolQuery.should(QueryBuilders.multiMatchQuery(namePE,
                            ProcedureProperty.PROCURING_ENTITY_NAME_KEYWORD.value(),
                            PROCURING_ENTITY_EDRPOU_KEYWORD.value()));
                }
            }
            boolQuery.minimumShouldMatch(1);
            mainQuery.must(boolQuery);
        }
    }

    private void filterByTenderStatus(FilterQuery filterQuery, BoolQueryBuilder mainQuery) {
        if (!CollectionUtils.isEmpty(filterQuery.getTenderStatuses())) {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            for (String status : filterQuery.getTenderStatuses()) {
                if (StringUtils.isNotEmpty(status)) {
                    boolQuery.should(QueryBuilders.termQuery(ProcedureProperty.TENDER_STATUS_KEYWORD.value(), status));
                }
            }
            boolQuery.minimumShouldMatch(1);
            mainQuery.must(boolQuery);
        }
    }

    private void filterByCurrency(FilterQuery filterQuery, BoolQueryBuilder mainQuery) {
        if (!CollectionUtils.isEmpty(filterQuery.getCurrency())) {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            for (String currency : filterQuery.getCurrency()) {
                if (StringUtils.isNotEmpty(currency)) {
                    boolQuery.should(QueryBuilders.termQuery(CURRENCY_KEYWORD.value(), currency));
                }
            }
            boolQuery.minimumShouldMatch(1);
            mainQuery.must(boolQuery);
        }
    }

    private void filterByCpv2Name(FilterQuery filterQuery, BoolQueryBuilder mainQuery) {
        if (!CollectionUtils.isEmpty(filterQuery.getCpv2Names())) {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            for (String cpv2Name : filterQuery.getCpv2Names()) {
                if (StringUtils.isNotEmpty(cpv2Name)) {
                    boolQuery.should(QueryBuilders.multiMatchQuery(cpv2Name, CPV2_NAME_KEYWORD.value(), CPV2_KEYWORD.value()));
                }
            }
            boolQuery.minimumShouldMatch(1);
            mainQuery.must(boolQuery);
        }
    }

    private void filterByCpvName(FilterQuery filterQuery, BoolQueryBuilder mainQuery) {
        if (!CollectionUtils.isEmpty(filterQuery.getCpvNames())) {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            for (String cpvName : filterQuery.getCpvNames()) {
                if (StringUtils.isNotEmpty(cpvName)) {
                    boolQuery.should(QueryBuilders.multiMatchQuery(cpvName, CPV_NAME_KEYWORD.value(), CPV_KEYWORD.value()));
                }
            }
            boolQuery.minimumShouldMatch(1);
            mainQuery.must(boolQuery);
        }
    }

    private void filterByProcedureType(FilterQuery filterQuery, BoolQueryBuilder mainQuery) {
        if (!CollectionUtils.isEmpty(filterQuery.getProcedureTypes())) {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            for (String procedureType : filterQuery.getProcedureTypes()) {
                if (StringUtils.isNotEmpty(procedureType)) {
                    boolQuery.should(QueryBuilders.matchQuery(ProcedureProperty.PROCEDURE_TYPE.value(), procedureType));
                }
            }
            boolQuery.minimumShouldMatch(1);
            mainQuery.must(boolQuery);
        }
    }

    private void filterRiskedProcedure(FilterQuery filterQuery, BoolQueryBuilder boolQuery) {
        if (filterQuery.getRiskedProcedures() == null) {
            return;
        }
        switch (filterQuery.getRiskedProcedures()) {
            case WITH_RISK:
                applyRiskFilter(boolQuery);
                break;
            case WITHOUT_RISK:
                boolQuery.mustNot(QueryBuilders.rangeQuery(TENDER_RISK_SCORE.value()).gt(0));
                break;
            case WITH_RISK_NO_PRIORITY:
                applyRiskFilter(boolQuery);
                boolQuery.mustNot(QueryBuilders.termQuery(HAS_PRIORITY_STATUS.value(), true));
                break;
            case WITH_RISK_HAS_PRIORITY:
                applyRiskFilter(boolQuery);
                boolQuery.must(QueryBuilders.termQuery(HAS_PRIORITY_STATUS.value(), true));
                break;
            default:
        }
    }

    private void filterRiskedProcedureByRank(FilterQuery filterQuery, BoolQueryBuilder boolQuery) {
        if (filterQuery.getTenderRank() == null || CollectionUtils.isEmpty(filterQuery.getTenderRank())) {
            return;
        }
        if (filterQuery.getTenderRank().contains(TenderScoreRank.All) || filterQuery.getTenderRank().size() == 3) {
            return;
        }

        BoolQueryBuilder tempQuery = QueryBuilders.boolQuery();
        if (filterQuery.getTenderRank().contains(TenderScoreRank.HIGH)) {
            tempQuery.should(QueryBuilders.rangeQuery(TENDER_RISK_SCORE.value()).gt(1.1));
        }
        if (filterQuery.getTenderRank().contains(TenderScoreRank.MEDIUM)) {
            tempQuery.should(QueryBuilders.rangeQuery(TENDER_RISK_SCORE.value()).lte(1.1).gte(0.5));
        }
        if (filterQuery.getTenderRank().contains(TenderScoreRank.LOW)) {
            tempQuery.should(QueryBuilders.rangeQuery(TENDER_RISK_SCORE.value()).lt(1.1).gt(0.0));
        }

        tempQuery.minimumShouldMatch(1);
        boolQuery.must(tempQuery);
    }

    private void filterByRiskedIndicators(FilterQuery filterQuery, BoolQueryBuilder mainQuery) {
        if (!CollectionUtils.isEmpty(filterQuery.getRiskedIndicators())) {
            List<String> riskedIndicators = filterQuery.getRiskedIndicators();
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            if (riskedIndicators != null && !riskedIndicators.isEmpty()) {
                for (String riskedIndicator : riskedIndicators) {
                    boolQuery.should(QueryBuilders.termQuery(INDICATORS_WITH_RISK_KEYWORD.value(), riskedIndicator));
                }
                boolQuery.minimumShouldMatch(1);
                mainQuery.must(boolQuery);
            }
        }
    }
}