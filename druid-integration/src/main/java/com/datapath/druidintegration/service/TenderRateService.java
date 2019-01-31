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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
public class TenderRateService {

    protected static final Logger LOG = LoggerFactory.getLogger(TenderRateService.class);

    private final static Double RISK_IMPORTANCE_SHARE = 0.5;
    private final static Double AMOUNT_IMPORTANCE_SHARE = 0.5;

    private final static String TENDER_OUTER_ID = "tenderOuterId";
    private final static String INDICATOR_ID = "indicatorId";
    private final static String INDICATOR_VALUE = "indicatorValue";
    private final static String INDICATOR_IMPACT = "indicatorImpact";
    private final static String ITERATION_ID = "iterationId";
    private final static String STATUS = "status";

    private String druidUrl;
    private String tenderIndex;
    private RestTemplate restTemplate;
    protected TenderRepository tenderRepository;
    protected IndicatorRepository indicatorRepository;

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

        GroupByRequest tendersWithAtLeastOneRiskRequest = GroupByRequest
                .builder()
                .queryType("groupBy")
                .granularity("all")
                .intervals("2017/2020")
                .dataSource(GroupByRequest.DataSource.builder().type("table").name(tenderIndex).build())
                .dimensions(Collections.singletonList(TENDER_OUTER_ID))
                .filter(new IntFilter("selector", INDICATOR_VALUE, 1))
                .limitSpec(GroupByRequest.LimitSpec.builder()
                        .type("default")
                        .limit(1000000)
                        .build())
                .build();

        GroupByResponse[] tendersWithAtLeastOneRiskResponse = restTemplate.postForObject(druidUrl,
                tendersWithAtLeastOneRiskRequest, GroupByResponse[].class);

        if (isNull(tendersWithAtLeastOneRiskResponse) || tendersWithAtLeastOneRiskResponse.length == 0)
            return Collections.emptyList();

        int chunkSize = 1000;

        String tendersWithAtLeastOneRisk = Arrays.stream(tendersWithAtLeastOneRiskResponse)
                .map(groupByResponse -> groupByResponse.getEvent().getTenderOuterId())
                .collect(Collectors.joining(","));

        List<Object> topTendersWithAmount = tenderRepository.findTendersWithAmountByTendersExcludingStatus(Arrays.asList("unsuccessful",
                "cancelled"), tendersWithAtLeastOneRisk);

        List<Indicator> allByIsActiveTrue = indicatorRepository.findAllByIsActiveTrue();
        List<String> indicatorIds = allByIsActiveTrue.stream().map(Indicator::getId).collect(Collectors.toList());

        Map<String, Double> tenderAmountMap = topTendersWithAmount.stream()
                .map(item -> (Object[]) item)
                .collect(Collectors.toMap(
                        i -> i[0].toString(), i -> Double.valueOf(i[1].toString()), (oldTender, newTender) -> newTender));

        List<String> topTenders = new ArrayList<>(tenderAmountMap.keySet());
        Map<String, Double> tenderIdScoreMap = new HashMap<>();
        Map<String, Double> tenderRiskScoreMap = new HashMap<>();
        if (!topTenders.isEmpty()) {
            for (int start = 0; start < topTenders.size(); start += chunkSize) {
                int end = Math.min(topTenders.size(), start + chunkSize);
                LOG.info(start + " - " + end + " of " + topTenders.size() + " are checking");

                GroupByRequest tendersIndicatorsMaxIteration = new GroupByRequest(tenderIndex, "2017/2020");
                tendersIndicatorsMaxIteration.setDimensions(Arrays.asList(TENDER_OUTER_ID, INDICATOR_ID));
                tendersIndicatorsMaxIteration.setAggregations(Collections.singletonList(new SimpleAggregationImpl("doubleMax", "maxIteration", ITERATION_ID)));
                tendersIndicatorsMaxIteration.setFilter(new ListStringFilter("in", TENDER_OUTER_ID, topTenders.subList(start, end)));
                GroupByResponse[] tendersIndicatorsMaxIterationResponse = restTemplate.postForObject(druidUrl, tendersIndicatorsMaxIteration, GroupByResponse[].class);

                if (nonNull(tendersIndicatorsMaxIterationResponse) && tendersIndicatorsMaxIterationResponse.length > 0) {

                    List<Event> response = Arrays.stream(tendersIndicatorsMaxIterationResponse).map(GroupByResponse::getEvent).collect(Collectors.toList());

                    List<Filter> filterList = response.stream().map((event) -> {
                        StringFilter tenderIdFilter = new StringFilter("selector", TENDER_OUTER_ID, event.getTenderOuterId());
                        StringFilter indicatorIdFilter = new StringFilter("selector", INDICATOR_ID, event.getIndicatorId());
                        IntFilter iterationIdFilter = new IntFilter("selector", ITERATION_ID, event.getMaxIteration().intValue());
                        IntFilter indicatorValueFilter = new IntFilter("selector", INDICATOR_VALUE, 1);
                        ListStringFilter indicatorsFilter = new ListStringFilter("in", INDICATOR_ID, indicatorIds);
                        FilterImpl filter = new FilterImpl();
                        filter.setType("and");
                        filter.setFields(Arrays.asList(tenderIdFilter, indicatorIdFilter, iterationIdFilter, indicatorValueFilter, indicatorsFilter));
                        return filter;
                    }).collect(Collectors.toList());

                    GroupByRequest filteredByTendersGroupBy = new GroupByRequest(tenderIndex, "2017/2020");
                    filteredByTendersGroupBy.setDimensions(Arrays.asList(TENDER_OUTER_ID, INDICATOR_ID, ITERATION_ID, INDICATOR_VALUE, INDICATOR_IMPACT, STATUS));
                    filteredByTendersGroupBy.setAggregations(Collections.singletonList(new SimpleAggregationImpl("doubleMax", "maxIteration", ITERATION_ID)));
                    filteredByTendersGroupBy.setFilter(new ListStringFilter("in", TENDER_OUTER_ID, topTenders));

                    GroupByRequest resultRequest = GroupByRequest
                            .builder()
                            .queryType("groupBy")
                            .granularity("all")
                            .intervals("2017/2020")
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


            Map<String, Double> tenderIdScoreSortedMap = tenderIdScoreMap.entrySet().stream()
                    .sorted(Entry.comparingByValue())
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));


            List<String> tenderIds = new ArrayList<>(tenderIdScoreSortedMap.keySet());
            List<Double> tenderScores = new ArrayList<>(new TreeSet<>(tenderIdScoreSortedMap.values()));
            Map<Double, Integer> tenderRiskScoreRankMap = IntStream.range(0, tenderScores.size())
                    .boxed()
                    .collect(Collectors.toMap(tenderScores::get, i -> i + 1));

            List<Double> tenderAmounts = new ArrayList<>(tenderAmountMap.entrySet().stream()
                    .filter(item -> tenderIds.contains(item.getKey()))
                    .map(Entry::getValue).sorted()
                    .collect(Collectors.toCollection(TreeSet::new)));

            Map<Double, Integer> tenderAmountsRankMap = IntStream.range(0, tenderAmounts.size())
                    .boxed()
                    .collect(Collectors.toMap(tenderAmounts::get, i -> i + 1));


            tenderIdScoreSortedMap.forEach((tenderOuterId, value) -> {
                Double tenderRiskScore = Double.valueOf(tenderRiskScoreRankMap.get(value)) * RISK_IMPORTANCE_SHARE;
                Double amountScore = Double.valueOf(tenderAmountsRankMap.get(tenderAmountMap.get(tenderOuterId))) * AMOUNT_IMPORTANCE_SHARE;
                tenderRiskScoreMap.put(tenderOuterId, tenderRiskScore + amountScore);
            });

            Map<String, Double> sortedTenderRiskScoreMap = tenderRiskScoreMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            List outerIds = sortedTenderRiskScoreMap.entrySet().stream()
                    .map(Map.Entry::getKey).collect(Collectors.toList());

            int partitionSize = 1000;
            List<List<String>> partitions = new ArrayList<>();
            for (int i = 0; i < outerIds.size(); i += partitionSize) {
                List list = outerIds.subList(i, Math.min(i + partitionSize, outerIds.size()));
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
                            String tenderId = (String) objArr[1];
                            Double amount = (Double) objArr[2];
                            String procuringEntityId = (String) objArr[3];
                            String region = (String) objArr[4];
                            Double score = sortedTenderRiskScoreMap.get(tenderOuterId);
                            Double impact = tenderIdScoreMap.get(tenderOuterId);

                            TenderScore tenderScore = new TenderScore();
                            tenderScore.setOuterId(tenderOuterId);
                            tenderScore.setTenderId(tenderId);
                            tenderScore.setScore(score);
                            tenderScore.setExpectedValue(amount);
                            tenderScore.setRegion(region);
                            tenderScore.setImpact(impact);
                            tenderScore.setProcuringEntityId(procuringEntityId);

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


}
