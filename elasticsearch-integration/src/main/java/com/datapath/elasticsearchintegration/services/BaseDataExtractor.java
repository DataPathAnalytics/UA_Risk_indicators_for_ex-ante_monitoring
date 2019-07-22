package com.datapath.elasticsearchintegration.services;

import com.datapath.elasticsearchintegration.constants.Constants;
import com.datapath.elasticsearchintegration.domain.KeyValueObject;
import com.datapath.elasticsearchintegration.domain.TenderIndicatorsCommonInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.bucket.significant.SignificantTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.datapath.elasticsearchintegration.constants.Constants.ELASTICSEARCH_INDEX;
import static com.datapath.elasticsearchintegration.constants.ProcedureProperty.DATE_PUBLISHED;
import static com.datapath.elasticsearchintegration.constants.ProcedureProperty.TENDER_RISK_SCORE;
import static com.datapath.persistence.utils.DateUtils.formatToString;

/**
 * @author vitalii
 */
@Slf4j
public abstract class BaseDataExtractor {

    void applyDateRange(String startOfPeriod, String endOfPeriod, BoolQueryBuilder boolQuery) {
        boolQuery.must(QueryBuilders.rangeQuery(DATE_PUBLISHED.value()).gte(startOfPeriod).lte(endOfPeriod));
    }

    void applyRiskFilter(BoolQueryBuilder boolQuery) {
        boolQuery.must(QueryBuilders.rangeQuery(TENDER_RISK_SCORE.value()).gt(0));
    }

    SearchRequestBuilder getRequestBuilder(TransportClient transportClient) {
        return transportClient.prepareSearch(ELASTICSEARCH_INDEX);
    }

    SearchResponse getSearchResponse(SearchRequestBuilder searchRequestBuilder) {
        return searchRequestBuilder.execute().actionGet();
    }

    KeyValueObject bucketToKeyValueObject(StringTerms.Bucket bucket) {
        return new KeyValueObject(bucket.getKeyAsString(), bucket.getDocCount());
    }

    KeyValueObject bucketToKeyValueObject(SignificantTerms.Bucket bucket) {
        return new KeyValueObject(bucket.getKeyAsString(), bucket.getDocCount());
    }

    BoolQueryBuilder getDateFilteredBoolQuery(Date startOfPeriod, Date endOfPeriod) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        applyDateRange(formatToString(startOfPeriod, Constants.DATE_FORMAT), formatToString(endOfPeriod, Constants.DATE_FORMAT), boolQuery);
        return boolQuery;
    }

    BoolQueryBuilder getDateFilteredBoolQuery(String startOfPeriod, String endOfPeriod) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        applyDateRange(startOfPeriod, endOfPeriod, boolQuery);
        return boolQuery;
    }

    List<TenderIndicatorsCommonInfo> responseToEntities(SearchResponse searchResponse) {
        List<TenderIndicatorsCommonInfo> results = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        for (SearchHit hit : searchResponse.getHits()) {
            TenderIndicatorsCommonInfo tenderIndicatorsCommonInfo = null;
            try {
                tenderIndicatorsCommonInfo = objectMapper.readValue(hit.getSourceAsString(), TenderIndicatorsCommonInfo.class);
                tenderIndicatorsCommonInfo.initScoreRank();
            } catch (IOException e) {
                log.error("Could not map elastic response object to POJO", e);
            }
            results.add(tenderIndicatorsCommonInfo);
        }
        return results;
    }
}
