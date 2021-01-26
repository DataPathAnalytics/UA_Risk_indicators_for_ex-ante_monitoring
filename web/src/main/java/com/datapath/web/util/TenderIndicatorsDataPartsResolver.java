package com.datapath.web.util;

import com.datapath.web.domain.DruidIndicator;
import com.datapath.web.domain.IndicatorInfo;
import com.datapath.web.domain.common.IndicatorType;
import com.datapath.web.domain.tendering.*;
import com.datapath.web.providers.IndicatorInfoProvider;
import lombok.Data;
import org.apache.commons.math3.util.Precision;
import org.springframework.lang.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static com.datapath.web.common.IndicatorValue.*;
import static org.springframework.util.CollectionUtils.isEmpty;

@Data
public class TenderIndicatorsDataPartsResolver {

    private IndicatorInfoProvider indicatorInfoProvider;
    private Map<String, String> tendersStatuses;
    private String tenderId;
    private String tenderOuterId;
    private String procedureType;
    private String tenderStatus;
    private List<DruidIndicator> rawIndicators;
    private List<DruidIndicator> tenderRawIndicators;
    private List<DruidIndicator> lotRawIndicators;
    private List<DruidIndicator> lastIterationLotRawIndicators;
    private Set<String> uniqueTenderIndicatorIds;
    private Set<String> uniqueLotIndicatorIds;
    private Map<String, DruidIndicator> tenderIterationIdsMap;
    private Map<String, List<DruidIndicator>> lotIterationIdsMap;
    private List<TenderIndicator> tenderIndicators;
    private List<LotIndicator> lotIndicators;
    private TenderIndicators indicators;
    private TenderIndicatorsSummary indicatorsSummary;
    private List<IndicatorInfo> indicatorInfos;
    private Map<String, List<LotIndicatorHistory>> lotIndicatorHistories;
    private Map<String, List<TenderIndicatorHistory>> tenderIndicatorHistories;

    public TenderIndicatorsDataPartsResolver(List<DruidIndicator> druidIndicators,
                                             IndicatorInfoProvider indicatorInfoProvider,
                                             Map<String, String> tendersStatuses) {
        this.rawIndicators = druidIndicators;
        this.indicatorInfoProvider = indicatorInfoProvider;
        this.tendersStatuses = tendersStatuses;
    }

    public TenderIndicatorsDataPartsResolver resolve() {
        tenderId = resolveTenderId();
        tenderOuterId = resolveTenderOuterId();
        procedureType = resolveProcedureType();
        tenderStatus = resolveTenderStatus();
        tenderRawIndicators = resolveRawTenderIndicators();
        lotRawIndicators = resolveRawLotIndicators();
        uniqueTenderIndicatorIds = resolveUniqueTenderIndicatorIds();
        uniqueLotIndicatorIds = resolveUniqueLotIndicatorIds();
        tenderIterationIdsMap = createTenderIterationMap();
        lotIterationIdsMap = createLotIterationMap();
        lotIndicatorHistories = resolveLotIndicatorsHistories();
        tenderIndicatorHistories = resolveTenderIndicatorsHistories();
        tenderIndicators = resolveTenderIndicators();
        lotIndicators = resolveLotIndicators();

        indicators = new TenderIndicators();
        indicators.setTenderIndicators(tenderIndicators);
        indicators.setLotIndicators(lotIndicators);

        indicatorsSummary = resolveIndicatorsSummary();
        indicatorInfos = resolveIndicatorsInfos();

        return this;
    }

    @Nullable
    private String resolveTenderId() {
        return rawIndicators != null && !rawIndicators.isEmpty() ?
                rawIndicators.get(0).getTenderId() : null;
    }

    @Nullable
    private String resolveTenderOuterId() {
        return rawIndicators != null && !rawIndicators.isEmpty() ?
                rawIndicators.get(0).getTenderOuterId() : null;
    }

    @Nullable
    private String resolveProcedureType() {
        return rawIndicators != null && !rawIndicators.isEmpty() ?
                rawIndicators.get(0).getProcedureType() : null;
    }

    @Nullable
    private String resolveTenderStatus() {
        return rawIndicators != null && !rawIndicators.isEmpty() ?
                tendersStatuses.get(rawIndicators.get(0).getTenderOuterId()) : null;
    }

    private List<DruidIndicator> resolveRawTenderIndicators() {
        return this.rawIndicators.stream()
                .filter(indicator -> IndicatorType.TENDER.toString()
                        .equals(indicator.getIndicatorType())
                ).collect(Collectors.toList());
    }

    private List<DruidIndicator> resolveRawLotIndicators() {
        return this.rawIndicators.stream()
                .filter(indicator -> IndicatorType.LOT.toString()
                        .equals(indicator.getIndicatorType())
                ).collect(Collectors.toList());
    }

    private Set<String> resolveUniqueTenderIndicatorIds() {
        return this.tenderRawIndicators.stream()
                .map(DruidIndicator::getIndicatorId)
                .collect(Collectors.toSet());
    }

    private Set<String> resolveUniqueLotIndicatorIds() {
        return this.lotRawIndicators.stream()
                .map(DruidIndicator::getIndicatorId)
                .collect(Collectors.toSet());
    }

    private Map<String, DruidIndicator> createTenderIterationMap() {
        Map<String, DruidIndicator> iterationIdsMap = new HashMap<>();
        uniqueTenderIndicatorIds.forEach(indicatorId -> tenderRawIndicators.stream()
                .filter(indicator -> indicator.getIndicatorId().equals(indicatorId))
                .max(Comparator.comparing(DruidIndicator::getIterationId))
                .ifPresent(druidIndicator -> iterationIdsMap.put(indicatorId, druidIndicator)));
        return iterationIdsMap;
    }

    private Map<String, List<DruidIndicator>> createLotIterationMap() {
        Map<String, List<DruidIndicator>> iterationIdsMap = new HashMap<>();
        uniqueLotIndicatorIds.forEach(indicatorId -> {
            List<DruidIndicator> lotIndicators = lotRawIndicators.stream()
                    .filter(indicator -> indicator.getIndicatorId().equals(indicatorId))
                    .collect(Collectors.toList());
            iterationIdsMap.put(indicatorId, lotIndicators);
        });
        return iterationIdsMap;
    }

    private List<LotIndicator> resolveLotIndicators() {
        List<LotIndicator> lotIndicators = new ArrayList<>();
        Arrays.asList(WORKED, NOT_WORKED, UNRESOLVED, UNRESOLVED_2)
                .forEach(val -> lotIndicators.addAll(resolveLotIndicatorsByValue(val)));
        return lotIndicators;
    }

    private List<LotIndicator> resolveLotIndicatorsByValue(Byte value) {
        Comparator<DruidIndicator> comparator = Comparator.comparing(DruidIndicator::getIterationId);
        List<LotIndicator> lotIndicators = new ArrayList<>();
        lotIterationIdsMap.forEach((key, val) -> {
            Long maxIteration = val.stream().max(comparator).get().getIterationId();
            List<DruidIndicator> filtered = val.stream()
                    .filter(druidIndicator -> druidIndicator.getIterationId().compareTo(maxIteration) == 0)
                    .filter(druidIndicator -> druidIndicator.getIndicatorValue().equals(value))
                    .collect(Collectors.toList());

            if (filtered.isEmpty()) {
                return;
            }

            DruidIndicator indicator = filtered.get(0);

            Set<String> lots = indicator.getLotIds() == null ? new HashSet<>() : new HashSet<>(indicator.getLotIds());

            List<LotIndicatorHistory> history = lotIndicatorHistories.get(key).stream()
                    .filter(lotIndicatorHistory -> lots.contains(lotIndicatorHistory.getLotOuterId()))
                    .collect(Collectors.toList());

            LotIndicator lotIndicator = new LotIndicator();
            lotIndicator.setStatus(indicator.getTenderStatus());
//            lotIndicator.setIndicatorId(key);
            lotIndicator.setIndicatorCode(indicatorInfoProvider.getIndicatorById(key).getIndicatorCode());
            lotIndicator.setValue(indicator.getIndicatorValue());
            lotIndicator.setImpact(indicator.getIndicatorImpact());
            lotIndicator.setLots(new ArrayList<>(lots));
            lotIndicator.setHistory(history);
            lotIndicators.add(lotIndicator);
        });

        return lotIndicators;
    }

    private List<TenderIndicator> resolveTenderIndicators() {
        List<TenderIndicator> tenderIndicators = new ArrayList<>();
        tenderIterationIdsMap.forEach((key, value) -> {
            TenderIndicator tenderIndicator = new TenderIndicator();
//            tenderIndicator.setIndicatorId(key);
            tenderIndicator.setIndicatorCode(indicatorInfoProvider.getIndicatorById(key).getIndicatorCode());
            tenderIndicator.setValue(value.getIndicatorValue());
            tenderIndicator.setImpact(value.getIndicatorImpact());
            tenderIndicator.setHistory(tenderIndicatorHistories.get(key));
            tenderIndicator.setStatus(value.getTenderStatus());
            tenderIndicators.add(tenderIndicator);
        });

        return tenderIndicators;
    }

    private TenderIndicatorsSummary resolveIndicatorsSummary() {
        Set<String> allIndicatorsIds = new TreeSet<>();
        Set<String> indicatorsWithRiskIds = new TreeSet<>();
        Set<TenderIndicator> tenderIndicatorWithRisks;
        Set<LotIndicator> lotIndicatorWithRisks;

        allIndicatorsIds.addAll(tenderIndicators.stream().map(TenderIndicator::getIndicatorCode).collect(Collectors.toSet()));
        allIndicatorsIds.addAll(lotIndicators.stream().map(LotIndicator::getIndicatorCode).collect(Collectors.toSet()));

        tenderIndicatorWithRisks = tenderIndicators.stream()
                .filter(indicator -> indicator.getValue() == 1)
                .peek(indicator -> indicatorsWithRiskIds.add(indicator.getIndicatorCode()))
                .collect(Collectors.toSet());

        lotIndicatorWithRisks = lotIndicators.stream()
                .filter(indicator -> indicator.getValue() == 1)
                .peek(indicator -> indicatorsWithRiskIds.add(indicator.getIndicatorCode()))
                .collect(Collectors.toSet());

        Set<String> failedIndicators = tenderIndicators.stream()
                .filter(indicator -> indicator.getValue() == -1)
                .map(TenderIndicator::getIndicatorCode)
                .collect(Collectors.toSet());

        failedIndicators.addAll(lotIndicators.stream()
                .filter(indicator -> indicator.getValue() == -1)
                .map(LotIndicator::getIndicatorCode)
                .collect(Collectors.toSet()));

        Set<String> failedIndicators2 = tenderIndicators.stream()
                .filter(indicator -> indicator.getValue() == -2)
                .map(TenderIndicator::getIndicatorCode)
                .collect(Collectors.toSet());

        failedIndicators2.addAll(lotIndicators.stream()
                .filter(indicator -> indicator.getValue() == -2)
                .map(LotIndicator::getIndicatorCode)
                .collect(Collectors.toSet()));


        Double tenderIndicatorsScore = Precision.round(tenderIndicatorWithRisks.stream()
                .mapToDouble(TenderIndicator::getImpact).sum(), 2);

        Double lotIndicatorsScore = Precision.round(lotIndicatorWithRisks.stream()
                .mapToDouble(LotIndicator::getImpact).sum(), 2);

        Integer numberOfEligibleIndicators = allIndicatorsIds.size();

        TenderIndicatorsSummary indicatorsSummary = new TenderIndicatorsSummary();
        indicatorsSummary.setNumberOfEligibleIndicators(numberOfEligibleIndicators);
        indicatorsSummary.setNumberOfIndicatorsWithRisk(indicatorsWithRiskIds.size());
        indicatorsSummary.setNumberOfFailedIndicators(failedIndicators.size() + failedIndicators2.size());
        indicatorsSummary.setTenderScore(tenderIndicatorsScore + lotIndicatorsScore);

        return indicatorsSummary;
    }

    private List<IndicatorInfo> resolveIndicatorsInfos() {
        List<IndicatorInfo> indicatorInfos = uniqueTenderIndicatorIds.stream()
                .map(indicatorInfoProvider::getIndicatorById)
                .collect(Collectors.toList());
        indicatorInfos.addAll(uniqueLotIndicatorIds.stream()
                .map(indicatorInfoProvider::getIndicatorById)
                .collect(Collectors.toList()));

        return indicatorInfos;
    }

    private Map<String, List<LotIndicatorHistory>> resolveLotIndicatorsHistories() {
        Map<String, List<LotIndicatorHistory>> indicatorsHistory = new HashMap<>();
        uniqueLotIndicatorIds.forEach(lotIndicatorId -> {
            List<LotIndicatorHistory> historicalIndicators = new ArrayList<>();
            lotRawIndicators.stream()
                    .filter(rawIndicator -> rawIndicator.getIndicatorId()
                            .equals(lotIndicatorId))
                    .map(druidIndicator -> {
                        List<LotIndicatorHistory> lotIndicatorHistory = new ArrayList<>();

                        if (!isEmpty(druidIndicator.getLotIds())) {
                            druidIndicator.getLotIds().forEach(lotId -> {
                                LotIndicatorHistory history = new LotIndicatorHistory();
                                history.setIndicatorImpact(druidIndicator.getIndicatorImpact());
                                history.setLotOuterId(lotId);
                                history.setStatus(druidIndicator.getTenderStatus());
                                history.setValue(druidIndicator.getIndicatorValue());
                                history.setDate(druidIndicator.getDate());
                                lotIndicatorHistory.add(history);
                            });
                            lotIndicatorHistory.sort((o1, o2) ->
                                    o1.getDate().isBefore(o2.getDate()) ? 1 : -1);
                        }
                        return lotIndicatorHistory;
                    }).forEach(historicalIndicators::addAll);

            Set<String> lotIds = historicalIndicators.stream()
                    .map(LotIndicatorHistory::getLotOuterId).collect(Collectors.toSet());

            Map<String, List<LotIndicatorHistory>> groupedByLotId = new HashMap<>();
            lotIds.forEach(lotId -> {
                historicalIndicators.forEach(lotIndicatorHistory -> {
                    if (lotIndicatorHistory.getLotOuterId().equals(lotId)) {
                        if (groupedByLotId.get(lotId) != null) {
                            groupedByLotId.get(lotId).add(lotIndicatorHistory);
                        } else {
                            ArrayList<LotIndicatorHistory> lotHistories = new ArrayList<>();
                            lotHistories.add(lotIndicatorHistory);
                            groupedByLotId.put(lotId, lotHistories);
                        }
                    }
                });
            });

            groupedByLotId.forEach((s, lotIndicatorHistories1) -> {
                int size = lotIndicatorHistories1.size();
                if (size > 1) {
                    int index = 0;
                    for (int i = size - 1; i > index; i--) {
                        int currentIndex = index;
                        int nextIndex = index + 1;
                        if (lotIndicatorHistories1.size() - 1 >= nextIndex) {
                            if (lotIndicatorHistories1.get(currentIndex).getValue().equals(lotIndicatorHistories1.get(nextIndex).getValue())) {
                                lotIndicatorHistories1.remove(currentIndex);
                            } else {
                                index++;
                            }
                        }
                    }
                }
            });

            List<LotIndicatorHistory> allHistoricalIndicators = new ArrayList<>();
            groupedByLotId.forEach((s, lotIndicatorHistories1) ->
                    allHistoricalIndicators.addAll(lotIndicatorHistories1));

            indicatorsHistory.put(lotIndicatorId, allHistoricalIndicators);
        });

        return indicatorsHistory;
    }

    private Map<String, List<TenderIndicatorHistory>> resolveTenderIndicatorsHistories() {
        Map<String, List<TenderIndicatorHistory>> indicatorsHistory = new HashMap<>();
        uniqueTenderIndicatorIds.forEach(tenderIndicatorId -> {
            List<TenderIndicatorHistory> historicalIndicators = new ArrayList<>();
            tenderRawIndicators.stream()
                    .filter(rawIndicator -> rawIndicator.getIndicatorId()
                            .equals(tenderIndicatorId))
                    .map(druidIndicator -> {
                        List<TenderIndicatorHistory> tenderIndicatorHistory = new ArrayList<>();
                        TenderIndicatorHistory history = new TenderIndicatorHistory();
                        history.setIndicatorImpact(druidIndicator.getIndicatorImpact());
                        history.setValue(druidIndicator.getIndicatorValue());
                        history.setDate(druidIndicator.getDate());
                        history.setStatus(druidIndicator.getTenderStatus());
                        tenderIndicatorHistory.add(history);
                        tenderIndicatorHistory.sort((o1, o2) ->
                                o1.getDate().isBefore(o2.getDate()) ? 1 : -1);
                        return tenderIndicatorHistory;
                    }).forEach(historicalIndicators::addAll);


            indicatorsHistory.put(tenderIndicatorId, historicalIndicators);
        });

        return indicatorsHistory;
    }


}
