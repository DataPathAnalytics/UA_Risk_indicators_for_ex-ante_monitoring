package com.datapath.druidintegration.service;

import com.datapath.druidintegration.model.TenderScore;
import com.datapath.druidintegration.model.druid.request.GroupByRequest;
import com.datapath.druidintegration.model.druid.request.common.Filter;
import com.datapath.druidintegration.model.druid.request.common.impl.*;
import com.datapath.druidintegration.model.druid.response.GroupByResponse;
import com.datapath.druidintegration.model.druid.response.common.Event;
import com.datapath.persistence.entities.Indicator;
import com.datapath.persistence.repositories.IndicatorRepository;
import com.datapath.persistence.repositories.TenderRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.datapath.druidintegration.DruidConstants.DEFAULT_INTERVAL;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Service
@Slf4j
public class TenderRateService {

    private static final Double RISK_IMPORTANCE_SHARE = 0.5;
    private static final Double AMOUNT_IMPORTANCE_SHARE = 0.5;
    private static final String TENDER_OUTER_ID = "tenderOuterId";
    private static final String INDICATOR_ID = "indicatorId";
    private static final String INDICATOR_VALUE = "indicatorValue";
    private static final String INDICATOR_IMPACT = "indicatorImpact";
    private static final String ITERATION_ID = "iterationId";
    private static final String STATUS = "status";

    private static final Comparator<TenderScoreData> TENDER_SCORE_COMPARATOR = Comparator
            .comparing(TenderScoreData::getRiskScore)
            .thenComparing(TenderScoreData::getAmount)
            .thenComparing(TenderScoreData::getTenderId);

    private static final Comparator<AmountScoreData> AMOUNT_SCORE_COMPARATOR = Comparator
            .comparing(AmountScoreData::getAmount)
            .thenComparing(AmountScoreData::getTenderId);

    @Value("${tenders.completed.days:30}")
    private Integer daysForQueue;
    private String druidUrl;
    private String tenderIndex;
    private RestTemplate restTemplate;
    protected TenderRepository tenderRepository;
    protected IndicatorRepository indicatorRepository;
    private boolean isAmountBasedTenderRiskScore;

    @Value("${queue.amount-based-tender-risk-score}")
    public void setAmountBasedTenderRiskScore(Boolean amountBasedTenderRiskScore) {
        isAmountBasedTenderRiskScore = amountBasedTenderRiskScore;
    }

    @Value("${druid.url}")
    public void setDruidUrl(String url) {
        this.druidUrl = url;
    }

    @Value("${druid.tenders.index}")
    public void setDruidTenderIndex(String index) {
        this.tenderIndex = index;
    }

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Autowired
    public void setTenderRepository(TenderRepository tenderRepository) {
        this.tenderRepository = tenderRepository;
    }

    @Autowired
    public void setIndicatorRepository(IndicatorRepository indicatorRepository) {
        this.indicatorRepository = indicatorRepository;
    }

    public List<TenderScore> getResult() {

        GroupByRequest tendersWithAtLeastOneRiskRequest = buildRequestWithLeastOneRiskTender();

        GroupByResponse[] tendersWithAtLeastOneRiskResponse = restTemplate.postForObject(druidUrl,
                tendersWithAtLeastOneRiskRequest, GroupByResponse[].class);

        if (isNull(tendersWithAtLeastOneRiskResponse) || tendersWithAtLeastOneRiskResponse.length == 0)
            return Collections.emptyList();

        String tendersWithAtLeastOneRisk = getCompletedTendersForDateRange(tendersWithAtLeastOneRiskResponse);

        List<Object> topTendersWithAmount = tenderRepository.findTendersWithAmountByTendersExcludingStatus(Arrays.asList("unsuccessful",
                "cancelled"), tendersWithAtLeastOneRisk);

        List<Indicator> allByIsActiveTrue = indicatorRepository.findAllByIsActiveTrue();
        List<String> indicatorIds = allByIsActiveTrue.stream().map(Indicator::getId).collect(Collectors.toList());

        Map<String, Double> tenderAmountMap = topTendersWithAmount.stream()
                .map(item -> (Object[]) item)
                .collect(Collectors.toMap(i -> i[0].toString(), i -> Double.valueOf(i[1].toString()), (oldTender, newTender) -> newTender));

        Map<String, String> tenderIdMap = topTendersWithAmount.stream()
                .map(item -> (Object[]) item)
                .collect(toMap(i -> i[0].toString(), i -> i[2].toString()));

        List<String> topTenders = new ArrayList<>(tenderAmountMap.keySet());
        Map<String, Double> tenderIdScoreMap = new HashMap<>();

        int chunkSize = 1000;
        if (!topTenders.isEmpty()) {
            for (int start = 0; start < topTenders.size(); start += chunkSize) {
                int end = Math.min(topTenders.size(), start + chunkSize);
                log.info(start + " - " + end + " of " + topTenders.size() + " are checking");

                GroupByRequest tendersIndicatorsMaxIteration = new GroupByRequest(tenderIndex);
                tendersIndicatorsMaxIteration.setDimensions(Arrays.asList(TENDER_OUTER_ID, INDICATOR_ID));
                tendersIndicatorsMaxIteration.setAggregations(Collections.singletonList(new SimpleAggregationImpl("doubleMax", "maxIteration", ITERATION_ID)));
                tendersIndicatorsMaxIteration.setFilter(new ListStringFilter("in", TENDER_OUTER_ID, topTenders.subList(start, end)));
                GroupByResponse[] tendersIndicatorsMaxIterationResponse = restTemplate.postForObject(druidUrl, tendersIndicatorsMaxIteration, GroupByResponse[].class);

                if (nonNull(tendersIndicatorsMaxIterationResponse) && tendersIndicatorsMaxIterationResponse.length > 0) {

                    List<Event> response = Arrays.stream(tendersIndicatorsMaxIterationResponse).map(GroupByResponse::getEvent).collect(Collectors.toList());

                    List<Filter> filterList = response.stream().map(event -> {
                        StringFilter indicatorIdFilter = new StringFilter("selector", INDICATOR_ID, event.getIndicatorId());
                        StringFilter tenderIdFilter = new StringFilter("selector", TENDER_OUTER_ID, event.getTenderOuterId());
                        IntFilter iterationIdFilter = new IntFilter("selector", ITERATION_ID, event.getMaxIteration().intValue());
                        IntFilter indicatorValueFilter = new IntFilter("selector", INDICATOR_VALUE, 1);
                        ListStringFilter indicatorsFilter = new ListStringFilter("in", INDICATOR_ID, indicatorIds);
                        FilterImpl filter = new FilterImpl();
                        filter.setType("and");
                        filter.setFields(Arrays.asList(tenderIdFilter, indicatorIdFilter, iterationIdFilter, indicatorValueFilter, indicatorsFilter));
                        return filter;
                    }).collect(Collectors.toList());

                    GroupByRequest filteredByTendersGroupBy = new GroupByRequest(tenderIndex);
                    filteredByTendersGroupBy.setDimensions(Arrays.asList(TENDER_OUTER_ID, INDICATOR_ID, ITERATION_ID, INDICATOR_VALUE, INDICATOR_IMPACT, STATUS));
                    filteredByTendersGroupBy.setAggregations(Collections.singletonList(new SimpleAggregationImpl("doubleMax", "maxIteration", ITERATION_ID)));
                    filteredByTendersGroupBy.setFilter(new ListStringFilter("in", TENDER_OUTER_ID, topTenders));

                    GroupByRequest resultRequest = GroupByRequest
                            .builder()
                            .queryType("groupBy")
                            .granularity("all")
                            .intervals(DEFAULT_INTERVAL)
                            .dataSource(GroupByRequest.DataSource.builder().type("query").query(filteredByTendersGroupBy).build())
                            .dimensions(Collections.singletonList(TENDER_OUTER_ID))
                            .aggregations(Collections.singletonList(SimpleAggregationImpl.builder().type("doubleSum").fieldName(INDICATOR_IMPACT).name("tenderScore").build()))
                            .filter(FilterImpl.builder().type("or").fields(filterList).build())
                            .limitSpec(GroupByRequest.LimitSpec.builder()
                                    .type("default")
                                    .limit(100000)
                                    .columns(Collections.singletonList(GroupByRequest.LimitSpec.Column.builder()
                                            .dimension("tenderScore")
                                            .direction("asc")
                                            .dimensionOrder("numeric")
                                            .build()))
                                    .build())
                            .build();

                    GroupByResponse[] postForObject = restTemplate.postForObject(druidUrl, resultRequest, GroupByResponse[].class);

                    if (postForObject != null) {
                        Arrays.stream(postForObject).forEach(item -> {
                            Event event = item.getEvent();
                            tenderIdScoreMap.put(event.getTenderOuterId(), event.getTenderScore());
                        });
                    }
                }
            }

            Map<String, Double> tenderRiskScoreMap = getTenderRiskScores(
                    tenderIdScoreMap,
                    tenderAmountMap,
                    tenderIdMap
            );

            Map<String, Double> sortedTenderRiskScoreMap = tenderRiskScoreMap.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            List<String> outerIds = new ArrayList<>(sortedTenderRiskScoreMap.keySet());

            int partitionSize = 1000;
            List<List<String>> partitions = new ArrayList<>();
            for (int i = 0; i < outerIds.size(); i += partitionSize) {
                List<String> list = outerIds.subList(i, Math.min(i + partitionSize, outerIds.size()));
                partitions.add(list);
            }

            List<TenderScore> finalTenderScores = new ArrayList<>();

            partitions.forEach(partition -> {
                String tenderIdsStr = String.join(",", partition);
                List<TenderScore> collect = tenderRepository.findAllTendersWithPERegionByOuterIdIn(tenderIdsStr)
                        .stream()
                        .map(obj -> {
                            Object[] objArr = (Object[]) obj;
                            String tenderOuterId = (String) objArr[0];
                            TenderScore tenderScore = new TenderScore();
                            tenderScore.setOuterId(tenderOuterId);
                            tenderScore.setTenderId((String) objArr[1]);
                            tenderScore.setScore(sortedTenderRiskScoreMap.get(tenderOuterId));
                            tenderScore.setExpectedValue((Double) objArr[2]);
                            tenderScore.setRegion((String) objArr[4]);
                            tenderScore.setImpact(tenderIdScoreMap.get(tenderOuterId));
                            tenderScore.setProcuringEntityId((String) objArr[3]);
                            return tenderScore;
                        })
                        .sorted(Comparator.comparing(TenderScore::getScore))
                        .collect(Collectors.toList());
                finalTenderScores.addAll(collect);
            });

            Collections.reverse(finalTenderScores);
            return finalTenderScores;
        }

        return Collections.emptyList();
    }

    private String getCompletedTendersForDateRange(GroupByResponse[] tendersWithAtLeastOneRiskResponse) {
        List<String> tendersWithAtLeastOneRiskAsList = Arrays.stream(tendersWithAtLeastOneRiskResponse)
                .map(groupByResponse -> groupByResponse.getEvent().getTenderOuterId()).collect(Collectors.toList());

        List<String> completedTendersNotOlderSpecificDaysCount = new ArrayList<>();

        boolean hasNext;
        long pageCount = 0;
        long pageSize = 20000;
        do {
            List<String> chunk = tendersWithAtLeastOneRiskAsList.stream()
                    .skip(pageCount * pageSize)
                    .limit(pageSize)
                    .collect(Collectors.toList());
            completedTendersNotOlderSpecificDaysCount.addAll(tenderRepository.findCompletedTenderNotOlderThanByOuterIdIn(ZonedDateTime.now().minusDays(daysForQueue), chunk));
            completedTendersNotOlderSpecificDaysCount.addAll(tenderRepository.findNotCompletedTenders(chunk));
            hasNext = chunk.size() == pageSize;
            pageCount++;
        } while (hasNext);

        return String.join(",", completedTendersNotOlderSpecificDaysCount);
    }

    GroupByRequest buildRequestWithLeastOneRiskTender() {
        return GroupByRequest
                .builder()
                .queryType("groupBy")
                .granularity("all")
                .intervals(DEFAULT_INTERVAL)
                .dataSource(GroupByRequest.DataSource.builder().type("table").name(tenderIndex).build())
                .dimensions(Collections.singletonList(TENDER_OUTER_ID))
                .filter(new IntFilter("selector", INDICATOR_VALUE, 1))
                .limitSpec(GroupByRequest.LimitSpec.builder()
                        .type("default")
                        .limit(1000000)
                        .build())
                .build();
    }

    private Map<String, Double> getTenderRiskScores(Map<String, Double> tenderIdScoreMap,
                                                    Map<String, Double> tenderAmountMap,
                                                    Map<String, String> tenderIdMap) {
        Map<String, Double> tenderIdScoreSortedMap = tenderIdScoreMap.entrySet().stream()
                .sorted(Entry.comparingByValue())
                .collect(toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        if (isAmountBasedTenderRiskScore) {
            return getScoreWithAmountBasedTenderScoreRankCalculation(
                    tenderIdScoreSortedMap,
                    tenderAmountMap,
                    tenderIdMap
            );
        } else {
            return getScoreWithSimpleRankCalculation(
                    tenderIdScoreSortedMap,
                    tenderAmountMap
            );
        }
    }

    private Map<String, Double> getScoreWithAmountBasedTenderScoreRankCalculation(Map<String, Double> tenderIdScoreSortedMap,
                                                                                  Map<String, Double> tenderAmountMap,
                                                                                  Map<String, String> tenderIdMap) {
        Map<String, Double> tenderRiskScoreMap = new HashMap<>();

        Map<AmountScoreData, String> amountScoreDataOuterIdMap = tenderIdScoreSortedMap.keySet().stream()
                .map(outerId -> {
                    TenderData tender = new TenderData();
                    tender.setOuterId(outerId);

                    AmountScoreData amountScoreData = new AmountScoreData();
                    amountScoreData.setAmount(tenderAmountMap.get(outerId));
                    amountScoreData.setTenderId(tenderIdMap.get(outerId));
                    tender.setAmountScoreData(amountScoreData);

                    return tender;
                }).collect(Collectors.toMap(TenderData::getAmountScoreData, TenderData::getOuterId));

        List<AmountScoreData> sortedAmountScores = amountScoreDataOuterIdMap.keySet().stream()
                .sorted(AMOUNT_SCORE_COMPARATOR)
                .collect(toList());

        Map<AmountScoreData, Integer> amountScoreRankMap = IntStream.range(0, sortedAmountScores.size())
                .boxed()
                .collect(toMap(sortedAmountScores::get, i -> i + 1));

        Map<String, Integer> amountScoreOuterIdRankMap = new HashMap<>();

        amountScoreDataOuterIdMap.forEach((amountScoreData, outerId) -> amountScoreOuterIdRankMap.put(outerId, amountScoreRankMap.get(amountScoreData)));

        Map<TenderScoreData, String> tenderScoreDataOuterIdMap = tenderIdScoreSortedMap.entrySet().stream()
                .map(e -> {
                    TenderData tender = new TenderData();
                    tender.setOuterId(e.getKey());

                    TenderScoreData amountScoreData = new TenderScoreData();
                    amountScoreData.setRiskScore(e.getValue());
                    amountScoreData.setAmount(tenderAmountMap.get(e.getKey()));
                    amountScoreData.setTenderId(tenderIdMap.get(e.getKey()));
                    tender.setTenderScoreData(amountScoreData);

                    return tender;
                }).collect(Collectors.toMap(TenderData::getTenderScoreData, TenderData::getOuterId));

        List<TenderScoreData> sortedTenderScores = tenderScoreDataOuterIdMap.keySet().stream()
                .sorted(TENDER_SCORE_COMPARATOR)
                .collect(toList());

        Map<TenderScoreData, Integer> tenderScoreRankMap = IntStream.range(0, sortedTenderScores.size())
                .boxed()
                .collect(toMap(sortedTenderScores::get, i -> i + 1));

        Map<String, Integer> tenderScoreOuterIdRankMap = new HashMap<>();

        tenderScoreDataOuterIdMap.forEach((tenderScoreData, outerId) -> tenderScoreOuterIdRankMap.put(outerId, tenderScoreRankMap.get(tenderScoreData)));

        tenderIdScoreSortedMap.forEach((tenderOuterId, value) -> {
            Double tenderRiskScore = Double.valueOf(tenderScoreOuterIdRankMap.get(tenderOuterId)) * RISK_IMPORTANCE_SHARE;
            Double amountScore = Double.valueOf(amountScoreOuterIdRankMap.get(tenderOuterId)) * AMOUNT_IMPORTANCE_SHARE;
            tenderRiskScoreMap.put(tenderOuterId, tenderRiskScore + amountScore);
        });

        return tenderRiskScoreMap;
    }

    private Map<String, Double> getScoreWithSimpleRankCalculation(Map<String, Double> tenderIdScoreSortedMap,
                                                                  Map<String, Double> tenderAmountMap) {
        Map<String, Double> tenderRiskScoreMap = new HashMap<>();

        List<String> tenderIds = new ArrayList<>(tenderIdScoreSortedMap.keySet());

        List<Double> tenderAmounts = tenderAmountMap.entrySet().stream()
                .filter(item -> tenderIds.contains(item.getKey()))
                .map(Entry::getValue)
                .distinct()
                .sorted()
                .collect(toList());

        List<Double> tenderScores = new ArrayList<>(new TreeSet<>(tenderIdScoreSortedMap.values()));
        Map<Double, Integer> tenderRiskScoreRankMap = IntStream.range(0, tenderScores.size())
                .boxed()
                .collect(toMap(tenderScores::get, i -> i + 1));

        Map<Double, Integer> tenderAmountsRankMap = IntStream.range(0, tenderAmounts.size())
                .boxed()
                .collect(toMap(tenderAmounts::get, i -> i + 1));

        tenderIdScoreSortedMap.forEach((tenderOuterId, value) -> {
            Double tenderRiskScore = Double.valueOf(tenderRiskScoreRankMap.get(value)) * RISK_IMPORTANCE_SHARE;
            Double amountScore = Double.valueOf(tenderAmountsRankMap.get(tenderAmountMap.get(tenderOuterId))) * AMOUNT_IMPORTANCE_SHARE;
            tenderRiskScoreMap.put(tenderOuterId, tenderRiskScore + amountScore);
        });

        return tenderRiskScoreMap;
    }

    @Data
    private static class TenderData {
        private String outerId;
        private TenderScoreData tenderScoreData;
        private AmountScoreData amountScoreData;
    }

    @Data
    private static class TenderScoreData {
        private Double amount;
        private Double riskScore;
        private String tenderId;
    }

    @Data
    private static class AmountScoreData {
        private Double amount;
        private String tenderId;
    }
}
