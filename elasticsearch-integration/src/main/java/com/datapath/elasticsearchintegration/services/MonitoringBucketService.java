package com.datapath.elasticsearchintegration.services;

import com.datapath.elasticsearchintegration.constants.ProcedureProperty;
import com.datapath.elasticsearchintegration.domain.KeyValueObject;
import com.datapath.elasticsearchintegration.domain.MonitoringBucketItemDTO;
import com.datapath.elasticsearchintegration.domain.TenderIndicatorsCommonInfo;
import com.datapath.elasticsearchintegration.util.Mapping;
import com.datapath.persistence.entities.monitoring.BucketItem;
import com.datapath.persistence.entities.monitoring.User;
import com.datapath.persistence.repositories.monitoring.BucketRepository;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

@Service
@Slf4j
public class MonitoringBucketService extends BaseDataExtractor {

    private final BucketRepository bucketRepository;
    private final TransportClient transportClient;

    public static final int BUCKET_MAX_COUNT = 500;

    @Autowired
    public MonitoringBucketService(BucketRepository bucketRepository, TransportClient transportClient) {
        this.bucketRepository = bucketRepository;
        this.transportClient = transportClient;
    }

    public void add(List<String> items, User user) {
        List<BucketItem> bucketItems = items.stream().map(item -> {
            BucketItem bucketItem = new BucketItem();
            bucketItem.setDate(LocalDate.now());
            bucketItem.setTenderId(item);
            bucketItem.setUser(user);
            return bucketItem;
        }).collect(Collectors.toList());

        bucketRepository.saveAll(bucketItems);
    }

    public boolean isTooMuchForBucket(long newIdsCount, User user) {
        return bucketRepository.countByUser(user) + newIdsCount > BUCKET_MAX_COUNT;
    }

    @Transactional
    public void delete(List<String> items, User user) {
        bucketRepository.deleteAllByUserAndTenderIdIn(user, items);
    }


    public List<MonitoringBucketItemDTO> get(User user) {

        List<BucketItem> allByUser = bucketRepository.findAllByUser(user);
        List<String> tenderIds = allByUser.stream().map(BucketItem::getTenderId).collect(Collectors.toList());

        SearchRequestBuilder requestBuilder = getRequestBuilder(transportClient);
        SearchRequestBuilder searchRequestBuilder = requestBuilder.setSize(10000)
                .setQuery(QueryBuilders.termsQuery(ProcedureProperty.TENDER_ID_KEYWORD.value(), tenderIds));
        SearchResponse searchResponse = getSearchResponse(searchRequestBuilder);
        List<TenderIndicatorsCommonInfo> results = responseToEntities(searchResponse);
        Map<String, TenderIndicatorsCommonInfo> mappedResults = results.stream().collect(toMap(TenderIndicatorsCommonInfo::getTenderId, tenderIndicatorsCommonInfo -> tenderIndicatorsCommonInfo));
        Map<LocalDate, List<BucketItem>> grouped = allByUser.stream().collect(groupingBy(BucketItem::getDate));

        List<MonitoringBucketItemDTO> response = new ArrayList<>();

        for (Map.Entry<LocalDate, List<BucketItem>> entry : grouped.entrySet()) {
            MonitoringBucketItemDTO monitoringBucketItemDTO = new MonitoringBucketItemDTO();
            monitoringBucketItemDTO.setDate(entry.getKey());
            monitoringBucketItemDTO.setTenders(new ArrayList<>());
            for (BucketItem bucketItem : entry.getValue()) {
                monitoringBucketItemDTO.getTenders().add(mappedResults.get(bucketItem.getTenderId()));
            }
            for (TenderIndicatorsCommonInfo procedure : monitoringBucketItemDTO.getTenders()) {
                try {
                    procedure.setTenderStatus(Mapping.TENDER_STATUS.get(procedure.getTenderStatus()).getValue().toString());
                    procedure.setProcedureType(Mapping.PROCEDURE_TYPES.get(procedure.getProcedureType()).getValue().toString());
                    procedure.setProcuringEntityKind(Mapping.PROCURING_ENTITY_KIND.get(procedure.getProcuringEntityKind()).getValue().toString());
                    procedure.setGsw(Mapping.GSW.get(procedure.getGsw()).getValue().toString());
                    procedure.setMonitoringStatus(Mapping.MONITORING_STATUS.get(procedure.getMonitoringStatus()).getValue().toString());
                    procedure.setMonitoringAppealAsString(Mapping.APPEAL.get(Boolean.valueOf(procedure.isMonitoringAppeal()).toString()).getValue().toString());
                    procedure.setIndicatorsWithRiskMapped(new ArrayList<>());
                    for (String risk : procedure.getIndicatorsWithRisk()) {
                        procedure.getIndicatorsWithRiskMapped().add(new KeyValueObject(risk, Mapping.RISK_INDICATORS.get(risk)));
                    }
                } catch (Exception ex) {
                    log.error("Error while processing bucket item", ex);
                }
            }
            response.add(monitoringBucketItemDTO);
        }

        return response;
    }

}
