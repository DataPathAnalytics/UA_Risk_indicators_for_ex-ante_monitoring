package com.datapath.elasticsearchintegration.services;

import com.datapath.elasticsearchintegration.domain.TenderIndicatorsCommonInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author vitalii
 */
@Service
@Slf4j
public class ElasticsearchDataUploadService {

    private final TransportClient transportClient;

    public ElasticsearchDataUploadService(TransportClient transportClient) {
        this.transportClient = transportClient;
    }

    void uploadItems(List<TenderIndicatorsCommonInfo> tenderIndicatorsCommonInfos) {

        BulkRequestBuilder bulkRequestBuilder = transportClient.prepareBulk();
        tenderIndicatorsCommonInfos.forEach(item -> {
            try {
                byte[] json = new ObjectMapper().writeValueAsBytes(item);
                bulkRequestBuilder.add(new IndexRequest("tenders_indicators", "_doc", item.getTenderOuterId())
                        .source(json, XContentType.JSON));
            } catch (JsonProcessingException e) {
                log.error("Fail while preparing bulk request for upload to elastic", e);
            }
        });

        BulkResponse bulkItemResponses = bulkRequestBuilder.get();
        log.info(bulkItemResponses.toString());
        if (bulkItemResponses.hasFailures()) {
            log.info("Uploading has failures - {}", bulkItemResponses.hasFailures());
            log.error(bulkItemResponses.buildFailureMessage());
        }
    }
}
