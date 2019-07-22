package com.datapath.elasticsearchintegration.services;

import com.datapath.elasticsearchintegration.constants.Aggregations;
import com.datapath.elasticsearchintegration.constants.Constants;
import com.datapath.elasticsearchintegration.constants.ProcedureProperty;
import com.datapath.elasticsearchintegration.domain.FilterQuery;
import com.datapath.elasticsearchintegration.domain.KeyValueObject;
import com.datapath.elasticsearchintegration.domain.charts.DynamicChartData;
import com.datapath.elasticsearchintegration.domain.charts.KPICharts;
import com.datapath.elasticsearchintegration.domain.charts.TopRegion;
import com.datapath.elasticsearchintegration.util.Mapping;
import com.datapath.persistence.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.InternalDateHistogram;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.metrics.sum.InternalSum;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.datapath.elasticsearchintegration.constants.Constants.DATE_FORMAT;
import static com.datapath.persistence.utils.DateUtils.formatToString;
import static java.time.temporal.TemporalAdjusters.nextOrSame;
import static java.time.temporal.TemporalAdjusters.previousOrSame;

/**
 * @author vitalii
 */
@Slf4j
@Service
public class ChartsDataExtractor extends BaseDataExtractor {

    private final TransportClient transportClient;

    private final ProcedureFilterService filterService;

    @Autowired
    public ChartsDataExtractor(TransportClient transportClient, ProcedureFilterService filterService) {
        this.transportClient = transportClient;
        this.filterService = filterService;
    }

    KPICharts proceduresCountMonthly() {
        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient)
                .setSize(0)
                .addAggregation(AggregationBuilders.dateHistogram(Aggregations.DAYS.value())
                        .dateHistogramInterval(DateHistogramInterval.MONTH)
                        .field(ProcedureProperty.DATE_PUBLISHED.value())
                        .order(BucketOrder.key(true))
                        .subAggregation(AggregationBuilders.sum(Aggregations.AMOUNT_OF_RISK.value()).field(ProcedureProperty.EXPECTED_VALUE.value()))
                        .subAggregation(AggregationBuilders.filter(Aggregations.WITH_RISK_COUNT.value(), QueryBuilders.rangeQuery(ProcedureProperty.TENDER_RISK_SCORE.value()).gt(0))
                                .subAggregation(AggregationBuilders.sum(Aggregations.AMOUNT_OF_RISK.value()).field(ProcedureProperty.EXPECTED_VALUE.value()))
                                .subAggregation(AggregationBuilders.filter(Aggregations.MONITORING_STATUS.value(),
                                        QueryBuilders.termQuery(ProcedureProperty.MONITORING_STATUS_KEYWORD.value(), "addressed"))
                                        .subAggregation(AggregationBuilders.sum(Aggregations.AMOUNT_OF_RISK.value()).field(ProcedureProperty.EXPECTED_VALUE.value()))
                                )
                        )
                );

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        Date yearAgo = find12MonthAgoDate();
        Date lastMonth = findLastWholeMonthDate();
        applyDateRange(DateUtils.formatToString(yearAgo, DATE_FORMAT), DateUtils.formatToString(lastMonth, DATE_FORMAT), boolQuery);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        try {
            applyDateRange(DateUtils.formatToString(format.parse("2018-10-01"), DATE_FORMAT), DateUtils.formatToString(lastMonth, DATE_FORMAT), boolQuery);
        } catch (ParseException e) {
            log.error(e.getMessage(), e);
        }
        searchRequestBuilder.setQuery(boolQuery);
        SearchResponse searchResponse = getSearchResponse(searchRequestBuilder);
        KPICharts kpiCharts = new KPICharts();

        ((InternalDateHistogram) searchResponse.getAggregations()
                .get(Aggregations.DAYS.value())).getBuckets()
                .forEach(bucket -> {
                    kpiCharts.getDates().add(bucket.getKeyAsString());
                    kpiCharts.getProceduresCount().add(bucket.getDocCount());
                    double proceduresAmount = ((InternalSum) bucket.getAggregations().get(Aggregations.AMOUNT_OF_RISK.value())).getValue();
                    kpiCharts.getProceduresAmount().add(proceduresAmount);
                    kpiCharts.getRiskedProceduresCount().add((((InternalFilter) bucket.getAggregations().get(Aggregations.WITH_RISK_COUNT.value())).getDocCount()));
                    double riskedProceduresAmount = ((InternalSum) ((InternalFilter) bucket.getAggregations().get((Aggregations.WITH_RISK_COUNT.value())))
                            .getAggregations().get(Aggregations.AMOUNT_OF_RISK.value())).getValue();
                    kpiCharts.getRiskedProceduresAmount().add(riskedProceduresAmount);
                    kpiCharts.getAddressedProceduresAmount().add(((InternalSum) ((InternalFilter) ((InternalFilter) bucket.getAggregations().get((Aggregations.WITH_RISK_COUNT.value())))
                            .getAggregations().get(Aggregations.MONITORING_STATUS.value())).getAggregations().get(Aggregations.AMOUNT_OF_RISK.value())).getValue());
                    try {
                        kpiCharts.getPartsRiskedProcedures().add(riskedProceduresAmount / proceduresAmount * 100);
                    } catch (Exception e) {
                        log.error("Failed to calculate parts risked procedures", e);
                    }
                });
        return kpiCharts;
    }

    List<DynamicChartData> getDynamicOfGrowingProceduresAmount(FilterQuery filterQuery) {
        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient)
                .setSize(0)
                .addAggregation(AggregationBuilders.dateHistogram(Aggregations.DAYS.value())
                        .dateHistogramInterval(DateHistogramInterval.WEEK)
                        .field(ProcedureProperty.DATE_PUBLISHED.value())
                        .order(BucketOrder.key(true))
                        .subAggregation(AggregationBuilders.filter(Aggregations.WITH_RISK_COUNT.value(), QueryBuilders.rangeQuery(ProcedureProperty.TENDER_RISK_SCORE.value()).gt(0))
                                .subAggregation(AggregationBuilders.sum(Aggregations.AMOUNT_OF_RISK.value()).field(ProcedureProperty.EXPECTED_VALUE.value())))
                        .subAggregation(AggregationBuilders.filter(Aggregations.WITH_PRIORITY_COUNT.value(), QueryBuilders.termQuery(ProcedureProperty.HAS_PRIORITY_STATUS.value(), true))
                                .subAggregation(AggregationBuilders.sum(Aggregations.AMOUNT_OF_RISK.value()).field(ProcedureProperty.EXPECTED_VALUE.value())))
                        .subAggregation(AggregationBuilders.sum(Aggregations.AMOUNT_OF_RISK.value()).field(ProcedureProperty.EXPECTED_VALUE.value()))
                );

        BoolQueryBuilder boolQuery = filterService.getBoolQueryWithFilters(filterQuery);
        Date firstMonday = findFirstMonday(filterQuery.getStartDate(), filterQuery.getEndDate());
        Date lastSunday = findLastSunday(filterQuery.getStartDate(), filterQuery.getEndDate());
        applyDateRange(DateUtils.formatToString(firstMonday, DATE_FORMAT), DateUtils.formatToString(lastSunday, DATE_FORMAT), boolQuery);

        searchRequestBuilder.setQuery(boolQuery);
        SearchResponse searchResponse = getSearchResponse(searchRequestBuilder);
        return ((InternalDateHistogram) searchResponse.getAggregations()
                .get(Aggregations.DAYS.value())).getBuckets()
                .stream()
                .map(this::parseAggregationItemToDynamicChartDataAmount).collect(Collectors.toList());
    }

    private DynamicChartData parseAggregationItemToDynamicChartDataAmount(InternalDateHistogram.Bucket bucket) {
        Date date = ((DateTime) bucket.getKey()).toDate();
        Double totalProcedures = ((InternalSum) bucket.getAggregations().get(Aggregations.AMOUNT_OF_RISK.value())).getValue();
        Double countWithRisk = ((InternalSum) ((InternalFilter) bucket.getAggregations().get((Aggregations.WITH_RISK_COUNT.value()))).getAggregations().get(Aggregations.AMOUNT_OF_RISK.value())).getValue();
        Double countWithPriority = ((InternalSum) ((InternalFilter) bucket.getAggregations().get((Aggregations.WITH_PRIORITY_COUNT.value()))).getAggregations().get(Aggregations.AMOUNT_OF_RISK.value())).getValue();

        return DynamicChartData.builder()
                .date(date)
                .dateAsString(formatToString(date, Constants.DATE_FORMAT))
                .totalCount(totalProcedures)
                .countWithRisk(countWithRisk)
                .countWithPriority(countWithPriority)
                .countWithoutPriority(totalProcedures - countWithPriority).build();
    }

    List<DynamicChartData> getDynamicOfGrowingProceduresCount(FilterQuery filterQuery) {
        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient)
                .setSize(0)
                .addAggregation(AggregationBuilders.dateHistogram(Aggregations.DAYS.value())
                        .dateHistogramInterval(DateHistogramInterval.WEEK)
                        .field(ProcedureProperty.DATE_PUBLISHED.value())
                        .order(BucketOrder.key(true))
                        .subAggregation(AggregationBuilders.filter(Aggregations.WITH_RISK_COUNT.value(), QueryBuilders.rangeQuery(ProcedureProperty.TENDER_RISK_SCORE.value()).gt(0)))
                        .subAggregation(AggregationBuilders.filter(Aggregations.WITH_PRIORITY_COUNT.value(), QueryBuilders.termQuery(ProcedureProperty.HAS_PRIORITY_STATUS.value(), true)))
                );
        BoolQueryBuilder boolQuery = filterService.getBoolQueryWithFilters(filterQuery);

        Date firstMonday = findFirstMonday(filterQuery.getStartDate(), filterQuery.getEndDate());
        Date lastSunday = findLastSunday(filterQuery.getStartDate(), filterQuery.getEndDate());
        applyDateRange(DateUtils.formatToString(firstMonday, DATE_FORMAT), DateUtils.formatToString(lastSunday, DATE_FORMAT), boolQuery);

        searchRequestBuilder.setQuery(boolQuery);
        SearchResponse searchResponse = getSearchResponse(searchRequestBuilder);
        return ((InternalDateHistogram) searchResponse.getAggregations()
                .get(Aggregations.DAYS.value())).getBuckets()
                .stream()
                .map(this::parseAggregationItemToDynamicChartData).collect(Collectors.toList());
    }

    private DynamicChartData parseAggregationItemToDynamicChartData(InternalDateHistogram.Bucket bucket) {
        Date date = ((DateTime) bucket.getKey()).toDate();
        Double totalProcedures = ((double) bucket.getDocCount());
        Double countWithRisk = ((double) ((InternalFilter) bucket.getAggregations().get((Aggregations.WITH_RISK_COUNT.value()))).getDocCount());
        Double countWithPriority = ((double) ((InternalFilter) bucket.getAggregations().get((Aggregations.WITH_PRIORITY_COUNT.value()))).getDocCount());

        return DynamicChartData.builder()
                .date(date)
                .dateAsString(formatToString(date, Constants.DATE_FORMAT))
                .totalCount(totalProcedures)
                .countWithRisk(countWithRisk)
                .countWithPriority(countWithPriority)
                .countWithoutPriority(totalProcedures - countWithPriority).build();
    }

    List<KeyValueObject> getProceduresGroupByPurchaseMethod(FilterQuery filterQuery) {
        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient)
                .setSize(0)
                .addAggregation(AggregationBuilders.terms(Aggregations.PROCEDURES.value()).field(ProcedureProperty.PROCEDURE_TYPE_KEYWORD.value()).order(BucketOrder.key(false)));
        BoolQueryBuilder boolQuery = filterService.getBoolQueryWithFilters(filterQuery);
        applyRiskFilter(boolQuery);
        searchRequestBuilder.setQuery(boolQuery);
        return ((StringTerms) getSearchResponse(searchRequestBuilder).getAggregations()
                .get(Aggregations.PROCEDURES.value()))
                .getBuckets()
                .stream()
                .map(this::bucketToKeyValueObject)
                .collect(Collectors.toList());
    }

    List<KeyValueObject> getProceduresGroupByPurchaseMethodAmount(FilterQuery filterQuery) {
        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient)
                .setSize(0)
                .addAggregation(AggregationBuilders.terms(Aggregations.PROCEDURES.value()).field(ProcedureProperty.PROCEDURE_TYPE_KEYWORD.value()).order(BucketOrder.key(false))
                        .subAggregation(AggregationBuilders.sum(Aggregations.AMOUNT_OF_RISK.value()).field(ProcedureProperty.EXPECTED_VALUE.value())));
        BoolQueryBuilder boolQuery = filterService.getBoolQueryWithFilters(filterQuery);
        applyRiskFilter(boolQuery);
        searchRequestBuilder.setQuery(boolQuery);
        return ((StringTerms) getSearchResponse(searchRequestBuilder).getAggregations()
                .get(Aggregations.PROCEDURES.value()))
                .getBuckets()
                .stream()
                .map(bucket -> new KeyValueObject(bucket.getKeyAsString(), ((InternalSum) bucket.getAggregations().get(Aggregations.AMOUNT_OF_RISK.value())).getValue()))
                .collect(Collectors.toList());
    }

    List<KeyValueObject> getTop10ProcuringEntity(FilterQuery filterQuery) {
        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient)
                .setSize(0)
                .addAggregation(AggregationBuilders.terms(Aggregations.PROCURING_ENTITY_KIND.value())
                        .field(ProcedureProperty.PROCURING_ENTITY_NAME_KEYWORD.value())
                        .order(BucketOrder.count(false)).size(10));
        BoolQueryBuilder boolQuery = filterService.getBoolQueryWithFilters(filterQuery);
        applyRiskFilter(boolQuery);
        searchRequestBuilder.setQuery(boolQuery);
        return ((StringTerms) getSearchResponse(searchRequestBuilder).getAggregations()
                .get(Aggregations.PROCURING_ENTITY_KIND.value()))
                .getBuckets()
                .stream()
                .map(this::bucketToKeyValueObject)
                .collect(Collectors.toList());
    }

    List<KeyValueObject> getTop10ProcuringEntityAmount(FilterQuery filterQuery) {
        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient)
                .setSize(0)
                .addAggregation(AggregationBuilders.terms(Aggregations.PROCURING_ENTITY_KIND.value())
                        .field(ProcedureProperty.PROCURING_ENTITY_NAME_KEYWORD.value()).order(BucketOrder.aggregation(Aggregations.AMOUNT_OF_RISK.value(), false))
                        .subAggregation(AggregationBuilders.sum(Aggregations.AMOUNT_OF_RISK.value()).field(ProcedureProperty.EXPECTED_VALUE.value())));
        BoolQueryBuilder boolQuery = filterService.getBoolQueryWithFilters(filterQuery);
        applyRiskFilter(boolQuery);
        searchRequestBuilder.setQuery(boolQuery);
        return ((StringTerms) getSearchResponse(searchRequestBuilder).getAggregations()
                .get(Aggregations.PROCURING_ENTITY_KIND.value()))
                .getBuckets()
                .stream()
                .map(bucket -> new KeyValueObject(bucket.getKeyAsString(), ((InternalSum) bucket.getAggregations().get(Aggregations.AMOUNT_OF_RISK.value())).getValue()))
                .collect(Collectors.toList());
    }

    List<KeyValueObject> getTop10Cpv(FilterQuery filterQuery) {
        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient)
                .setSize(0)
                .addAggregation(AggregationBuilders.terms(Aggregations.CPV_NAME.value())
                        .field(ProcedureProperty.CPV_NAME_KEYWORD.value())
                        .order(BucketOrder.count(false)).size(10));
        BoolQueryBuilder boolQuery = filterService.getBoolQueryWithFilters(filterQuery);
        applyRiskFilter(boolQuery);
        searchRequestBuilder.setQuery(boolQuery);
        return ((StringTerms) getSearchResponse(searchRequestBuilder).getAggregations()
                .get(Aggregations.CPV_NAME.value()))
                .getBuckets()
                .stream()
                .map(this::bucketToKeyValueObject)
                .collect(Collectors.toList());
    }

    List<KeyValueObject> getTop10CpvAmount(FilterQuery filterQuery) {
        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient)
                .setSize(0)
                .addAggregation(AggregationBuilders.terms(Aggregations.CPV_NAME.value())
                        .field(ProcedureProperty.CPV_NAME_KEYWORD.value()).order(BucketOrder.aggregation(Aggregations.AMOUNT_OF_RISK.value(), false))
                        .subAggregation(AggregationBuilders.sum(Aggregations.AMOUNT_OF_RISK.value()).field(ProcedureProperty.EXPECTED_VALUE.value())));
        BoolQueryBuilder boolQuery = filterService.getBoolQueryWithFilters(filterQuery);
        applyRiskFilter(boolQuery);
        searchRequestBuilder.setQuery(boolQuery);
        return ((StringTerms) getSearchResponse(searchRequestBuilder).getAggregations()
                .get(Aggregations.CPV_NAME.value()))
                .getBuckets()
                .stream()
                .map(bucket -> new KeyValueObject(bucket.getKeyAsString(), ((InternalSum) bucket.getAggregations().get(Aggregations.AMOUNT_OF_RISK.value())).getValue()))
                .collect(Collectors.toList());
    }

    List<KeyValueObject> getTop10RiskIndicators(FilterQuery filterQuery) {
        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient)
                .setSize(0)
                .addAggregation(AggregationBuilders.terms(Aggregations.RISK_INDICATORS.value()).field(ProcedureProperty.INDICATORS_WITH_RISK_KEYWORD.value()).size(10).order(BucketOrder.count(false)));
        BoolQueryBuilder boolQuery = filterService.getBoolQueryWithFilters(filterQuery);
        applyRiskFilter(boolQuery);
        searchRequestBuilder.setQuery(boolQuery);
        return ((StringTerms) getSearchResponse(searchRequestBuilder).getAggregations()
                .get(Aggregations.RISK_INDICATORS.value()))
                .getBuckets()
                .stream()
                .map(this::bucketToKeyValueObject)
                .collect(Collectors.toList());
    }

    List<KeyValueObject> getTop10RiskIndicatorsAmount(FilterQuery filterQuery) {
        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient)
                .setSize(0)
                .addAggregation(AggregationBuilders.terms(Aggregations.RISK_INDICATORS_AMOUNT.value()).field(ProcedureProperty.INDICATORS_WITH_RISK_KEYWORD.value())
                        .size(10)
                        .order(BucketOrder.aggregation(Aggregations.AMOUNT_OF_RISK.value(), false))
                        .subAggregation(AggregationBuilders.sum(Aggregations.AMOUNT_OF_RISK.value()).field(ProcedureProperty.EXPECTED_VALUE.value())));
        BoolQueryBuilder boolQuery = filterService.getBoolQueryWithFilters(filterQuery);
        applyRiskFilter(boolQuery);
        searchRequestBuilder.setQuery(boolQuery);
        return ((StringTerms) getSearchResponse(searchRequestBuilder).getAggregations()
                .get(Aggregations.RISK_INDICATORS_AMOUNT.value()))
                .getBuckets()
                .stream()
                .map(bucket -> new KeyValueObject(bucket.getKeyAsString(), ((InternalSum) bucket.getAggregations().get(Aggregations.AMOUNT_OF_RISK.value())).getValue()))
                .collect(Collectors.toList());
    }

    List<TopRegion> getTop10RegionByRiskProcedureCount(FilterQuery filterQuery) {
        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient)
                .setSize(0)
                .addAggregation(AggregationBuilders.terms(Aggregations.PROCURING_ENTITY_KIND.value())
                        .field(ProcedureProperty.REGION_KEYWORD.value())
                        .order(BucketOrder.count(false))
                        .size(10)
                        .subAggregation(
                                AggregationBuilders
                                        .filter(Aggregations.WITH_PRIORITY_COUNT.value(),
                                                QueryBuilders.termQuery(ProcedureProperty.HAS_PRIORITY_STATUS.value(), true))
                        ));

        BoolQueryBuilder boolQuery = filterService.getBoolQueryWithFilters(filterQuery);
        applyRiskFilter(boolQuery);
        searchRequestBuilder.setQuery(boolQuery);
        SearchResponse searchResponse = getSearchResponse(searchRequestBuilder);
        return ((StringTerms) searchResponse.getAggregations()
                .get(Aggregations.PROCURING_ENTITY_KIND.value()))
                .getBuckets()
                .stream()
                .map(bucket -> {
                    long prioritized = ((InternalFilter) bucket.getAggregations().get(Aggregations.WITH_PRIORITY_COUNT.value())).getDocCount();
                    return new TopRegion(bucket.getKeyAsString(), (double) bucket.getDocCount(), (double) prioritized);
                })
                .collect(Collectors.toList());
    }

    List<TopRegion> getTop10RegionByRiskProcedureAmount(FilterQuery filterQuery) {
        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient)
                .setSize(0)
                .addAggregation(AggregationBuilders.terms(Aggregations.REGION_AMOUNT.value())
                        .field(ProcedureProperty.REGION_KEYWORD.value())
                        .size(10)
                        .order(BucketOrder.aggregation(Aggregations.AMOUNT_OF_RISK.value(), false))
                        .subAggregation(
                                AggregationBuilders
                                        .filter(Aggregations.WITH_PRIORITY_COUNT.value(),
                                                QueryBuilders.termQuery(ProcedureProperty.HAS_PRIORITY_STATUS.value(), true))
                                        .subAggregation(AggregationBuilders.sum(Aggregations.AMOUNT_OF_RISK.value()).field(ProcedureProperty.EXPECTED_VALUE.value()))
                        )
                        .subAggregation(AggregationBuilders.sum(Aggregations.AMOUNT_OF_RISK.value()).field(ProcedureProperty.EXPECTED_VALUE.value())));

        BoolQueryBuilder boolQuery = filterService.getBoolQueryWithFilters(filterQuery);
        applyRiskFilter(boolQuery);
        searchRequestBuilder.setQuery(boolQuery);
        SearchResponse searchResponse = getSearchResponse(searchRequestBuilder);
        return ((StringTerms) searchResponse.getAggregations().get(Aggregations.REGION_AMOUNT.value())).getBuckets().stream().map(bucket -> {
            bucket.getKeyAsString();
            ((InternalSum) bucket.getAggregations().get("amountOfRisc")).getValue();
            ((InternalSum) ((InternalFilter) bucket.getAggregations().get("prioritized")).getAggregations().asList().get(0)).getValue();
            return new TopRegion(bucket.getKeyAsString(), ((InternalSum) bucket.getAggregations().get("amountOfRisc")).getValue(),
                    ((InternalSum) ((InternalFilter) bucket.getAggregations().get("prioritized")).getAggregations().asList().get(0)).getValue());
        }).collect(Collectors.toList());
    }

    List<KeyValueObject> getProcedureByRiskTable(FilterQuery filterQuery) {
        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient)
                .setSize(0)
                .addAggregation(AggregationBuilders.terms(Aggregations.PROCEDURE_TYPE.value())
                        .field(ProcedureProperty.PROCEDURE_TYPE_KEYWORD.value())
                        .size(10)
                        .subAggregation(
                                AggregationBuilders.terms(Aggregations.WITH_PRIORITY_COUNT.value())
                                        .field(ProcedureProperty.INDICATORS_WITH_RISK_KEYWORD.value())
                                        .size(50)
                                        .minDocCount(0)
                                        .order(BucketOrder.key(true))
                        )
                );

        BoolQueryBuilder boolQuery = filterService.getBoolQueryWithFilters(filterQuery);
        applyRiskFilter(boolQuery);
        searchRequestBuilder.setQuery(boolQuery);
        SearchResponse searchResponse = getSearchResponse(searchRequestBuilder);
        return ((StringTerms) searchResponse.getAggregations()
                .get(Aggregations.PROCEDURE_TYPE.value()))
                .getBuckets()
                .stream()
                .map(bucket -> {
                    String procedureType = bucket.getKeyAsString();
                    List<KeyValueObject> risks = ((StringTerms) bucket.getAggregations().get(Aggregations.WITH_PRIORITY_COUNT.value())).getBuckets()
                            .stream()
                            .map(subBucket -> {
                                if (CollectionUtils.contains(Mapping.RISK_INDICATORS_PROCEDURES.get(subBucket.getKeyAsString()).iterator(), procedureType)) {
                                    return new KeyValueObject(new KeyValueObject(subBucket.getKeyAsString(), Mapping.RISK_INDICATORS.get(subBucket.getKeyAsString())), subBucket.getDocCount());
                                } else {
                                    return new KeyValueObject(new KeyValueObject(subBucket.getKeyAsString(), Mapping.RISK_INDICATORS.get(subBucket.getKeyAsString())), "X");
                                }
                            }).collect(Collectors.toList());

                    return new KeyValueObject(procedureType, risks);
                })
                .collect(Collectors.toList());
    }

    List<KeyValueObject> getProcedureByRiskTableAmount(FilterQuery filterQuery) {
        SearchRequestBuilder searchRequestBuilder = getRequestBuilder(transportClient)
                .setSize(0)
                .addAggregation(AggregationBuilders.terms(Aggregations.PROCEDURE_TYPE.value())
                        .field(ProcedureProperty.PROCEDURE_TYPE_KEYWORD.value())
                        .size(10)
                        .subAggregation(
                                AggregationBuilders.terms(Aggregations.WITH_PRIORITY_COUNT.value())
                                        .field(ProcedureProperty.INDICATORS_WITH_RISK_KEYWORD.value())
                                        .size(50)
                                        .minDocCount(0)
                                        .order(BucketOrder.key(true))
                                        .subAggregation(AggregationBuilders.sum(Aggregations.AMOUNT_OF_RISK.value()).field(ProcedureProperty.EXPECTED_VALUE.value()))
                        )
                );

        BoolQueryBuilder boolQuery = filterService.getBoolQueryWithFilters(filterQuery);
        applyRiskFilter(boolQuery);
        searchRequestBuilder.setQuery(boolQuery);
        SearchResponse searchResponse = getSearchResponse(searchRequestBuilder);
        return ((StringTerms) searchResponse.getAggregations()
                .get(Aggregations.PROCEDURE_TYPE.value()))
                .getBuckets()
                .stream()
                .map(bucket -> {
                    String procedureType = bucket.getKeyAsString();
                    List<KeyValueObject> risks = ((StringTerms) bucket.getAggregations().get(Aggregations.WITH_PRIORITY_COUNT.value())).getBuckets()
                            .stream()
                            .map(subBucket -> {
                                KeyValueObject key = new KeyValueObject(subBucket.getKeyAsString(), Mapping.RISK_INDICATORS.get(subBucket.getKeyAsString()));
                                if (CollectionUtils.contains(Mapping.RISK_INDICATORS_PROCEDURES.get(subBucket.getKeyAsString()).iterator(), procedureType)) {
                                    return new KeyValueObject(key, ((InternalSum) subBucket.getAggregations().get(Aggregations.AMOUNT_OF_RISK.value())).getValue());
                                } else {
                                    return new KeyValueObject(key, "X");
                                }
                            }).collect(Collectors.toList());
                    return new KeyValueObject(procedureType, risks);
                })
                .collect(Collectors.toList());
    }

    private static Date findLastSunday(Date dateStart, Date dateEnd) {
        ZoneId defaultZoneId = ZoneId.systemDefault();
        LocalDate resultDateTime = dateEnd.toInstant().atZone(defaultZoneId).toLocalDate().with(previousOrSame(DayOfWeek.SUNDAY));
        Date resultDate = Date.from(resultDateTime.atStartOfDay(ZoneId.systemDefault()).toInstant());
        if (resultDate.before(dateStart)) {
            return dateEnd;
        }
        return resultDate;
    }

    private static Date findFirstMonday(Date dateStart, Date dateEnd) {
        ZoneId defaultZoneId = ZoneId.systemDefault();
        LocalDate resultDateTime = dateStart.toInstant().atZone(defaultZoneId).toLocalDate().with(nextOrSame(DayOfWeek.MONDAY));
        Date resultDate = Date.from(resultDateTime.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date lastSunday = findLastSunday(dateStart, dateEnd);
        if (resultDate.before(lastSunday)) {
            return resultDate;
        }
        if (resultDate.after(lastSunday)) {
            return dateStart;
        }
        return dateStart;
    }

    private static Date find12MonthAgoDate() {
        LocalDate currentMonthDate = LocalDate.now().withDayOfMonth(1);
        return Date.from(currentMonthDate.minusMonths(12).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static Date findLastWholeMonthDate() {
        return Date.from(LocalDate.now().withDayOfMonth(1).minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
