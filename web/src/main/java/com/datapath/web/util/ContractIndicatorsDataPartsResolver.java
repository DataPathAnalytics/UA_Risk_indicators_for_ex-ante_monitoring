package com.datapath.web.util;

import com.datapath.web.domain.DruidIndicator;
import com.datapath.web.domain.IndicatorInfo;
import com.datapath.web.domain.common.IndicatorType;
import com.datapath.web.domain.contracting.*;
import com.datapath.web.providers.IndicatorInfoProvider;
import lombok.Data;
import org.apache.commons.math3.util.Precision;
import org.springframework.lang.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static com.datapath.web.common.IndicatorValue.*;

@Data
public class ContractIndicatorsDataPartsResolver {

    private IndicatorInfoProvider indicatorInfoProvider;
    private String tenderId;
    private String tenderOuterId;
    private String contractId;
    private String contractOuterId;
    private String procedureType;
    private String tenderStatus;
    private List<DruidIndicator> rawIndicators;
    private List<DruidIndicator> contractRawIndicators;
    private List<DruidIndicator> lotRawIndicators;
    private List<DruidIndicator> lastIterationLotRawIndicators;
    private Set<String> uniqueContractIndicatorIds;
    private Set<String> uniqueLotIndicatorIds;
    private Map<String, DruidIndicator> contractIterationIdsMap;
    private Map<String, List<DruidIndicator>> lotIterationIdsMap;
    private List<ContractIndicator> contractIndicators;
    private List<LotIndicator> lotIndicators;
    private ContractIndicators indicators;
    private ContractIndicatorsSummary indicatorsSummary;
    private List<IndicatorInfo> indicatorInfos;
    private Map<String, List<LotIndicatorHistory>> lotIndicatorHistories;
    private Map<String, List<ContractIndicatorHistory>> contractIndicatorHistories;

    public ContractIndicatorsDataPartsResolver(List<DruidIndicator> druidIndicators,
                                               IndicatorInfoProvider indicatorInfoProvider) {
        this.rawIndicators = druidIndicators;
        this.indicatorInfoProvider = indicatorInfoProvider;
    }

    public ContractIndicatorsDataPartsResolver resolve() {
        tenderId = resolveTenderId();
        tenderOuterId = resolveTenderOuterId();
        contractId = resolveContractId();
        contractOuterId = resolveContractOuterId();
        procedureType = resolveProcedureType();
        tenderStatus = resolveTenderStatus();
        contractRawIndicators = resolveRawContractIndicators();
        lotRawIndicators = resolveRawLotIndicators();
        uniqueContractIndicatorIds = resolveUniqueContractIndicatorIds();
        uniqueLotIndicatorIds = resolveUniqueLotIndicatorIds();
        contractIterationIdsMap = createContractIterationMap();
        lotIterationIdsMap = createLotIterationMap();
        lotIndicatorHistories = resolveLotIndicatorsHistories();
        lotIndicators = resolveLotIndicators();
        contractIndicatorHistories = resolveContractIndicatorsHistories();
        contractIndicators = resolveContractIndicators();

        indicators = new ContractIndicators();
        indicators.setContractIndicators(contractIndicators);
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

    private String resolveTenderOuterId() {
        return rawIndicators != null && !rawIndicators.isEmpty() ?
                rawIndicators.get(0).getTenderOuterId() : null;
    }

    private String resolveContractId() {
        return rawIndicators != null && !rawIndicators.isEmpty() ?
                rawIndicators.get(0).getContractId() : null;
    }

    private String resolveContractOuterId() {
        return rawIndicators != null && !rawIndicators.isEmpty() ?
                rawIndicators.get(0).getContractOuterId() : null;
    }

    private String resolveProcedureType() {
        return rawIndicators != null && !rawIndicators.isEmpty() ?
                rawIndicators.get(0).getProcedureType() : null;
    }

    private String resolveTenderStatus() {
        return rawIndicators != null && !rawIndicators.isEmpty() ?
                rawIndicators.get(0).getTenderStatus() : null;
    }

    private List<DruidIndicator> resolveRawContractIndicators() {
        return this.rawIndicators.stream()
                .filter(indicator -> IndicatorType.CONTRACT.toString()
                        .equals(indicator.getIndicatorType())
                ).collect(Collectors.toList());
    }

    private List<DruidIndicator> resolveRawLotIndicators() {
        return this.rawIndicators.stream()
                .filter(indicator -> IndicatorType.LOT.toString()
                        .equals(indicator.getIndicatorType())
                ).collect(Collectors.toList());
    }

    private Set<String> resolveUniqueContractIndicatorIds() {
        return this.contractRawIndicators.stream()
                .map(DruidIndicator::getIndicatorId)
                .collect(Collectors.toSet());
    }

    private Set<String> resolveUniqueLotIndicatorIds() {
        return this.lotRawIndicators.stream()
                .map(DruidIndicator::getIndicatorId)
                .collect(Collectors.toSet());
    }

    private Map<String, DruidIndicator> createContractIterationMap() {
        Map<String, DruidIndicator> iterationIdsMap = new HashMap<>();
        uniqueContractIndicatorIds.forEach(indicatorId -> contractRawIndicators.stream()
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

            Set<String> lots = new HashSet<>(indicator.getLotIds());

            List<LotIndicatorHistory> history = lotIndicatorHistories.get(key).stream()
                    .filter(lotIndicatorHistory -> lots.contains(lotIndicatorHistory.getLotOuterId()))
                    .collect(Collectors.toList());

            LotIndicator lotIndicator = new LotIndicator();
            lotIndicator.setIndicatorId(key);
            lotIndicator.setValue(indicator.getIndicatorValue());
            lotIndicator.setImpact(indicator.getIndicatorImpact());
            lotIndicator.setLots(new ArrayList<>(lots));
            lotIndicator.setHistory(history);
            lotIndicators.add(lotIndicator);
        });

        return lotIndicators;
    }

    private List<ContractIndicator> resolveContractIndicators() {
        List<ContractIndicator> contractIndicators = new ArrayList<>();
        contractIterationIdsMap.forEach((key, value) -> {
            ContractIndicator contractIndicator = new ContractIndicator();
            contractIndicator.setIndicatorId(key);
            contractIndicator.setValue(value.getIndicatorValue());
            contractIndicator.setImpact(value.getIndicatorImpact());
            contractIndicator.setHistory(contractIndicatorHistories.get(key));
            contractIndicators.add(contractIndicator);
        });

        return contractIndicators;
    }

    private ContractIndicatorsSummary resolveIndicatorsSummary() {
        Set<String> allIndicatorsIds = new TreeSet<>();
        Set<String> indicatorsWithRiskIds = new TreeSet<>();
        Set<ContractIndicator> contractIndicatorWithRisks;
        Set<LotIndicator> lotIndicatorWithRisks;


        allIndicatorsIds.addAll(contractIndicators.stream().map(ContractIndicator::getIndicatorId).collect(Collectors.toSet()));
        allIndicatorsIds.addAll(lotIndicators.stream().map(LotIndicator::getIndicatorId).collect(Collectors.toSet()));


        contractIndicatorWithRisks = contractIndicators.stream()
                .filter(indicator -> indicator.getValue() == 1)
                .peek(indicator -> indicatorsWithRiskIds.add(indicator.getIndicatorId()))
                .collect(Collectors.toSet());

        lotIndicatorWithRisks = lotIndicators.stream()
                .filter(indicator -> indicator.getValue() == 1)
                .peek(indicator -> indicatorsWithRiskIds.add(indicator.getIndicatorId()))
                .collect(Collectors.toSet());

        Set<String> failedIndicators = contractIndicators.stream()
                .filter(indicator -> indicator.getValue() == -1)
                .map(ContractIndicator::getIndicatorId)
                .collect(Collectors.toSet());

        failedIndicators.addAll(lotIndicators.stream()
                .filter(indicator -> indicator.getValue() == -1)
                .map(LotIndicator::getIndicatorId)
                .collect(Collectors.toSet()));

        Set<String> failedIndicators2 = contractIndicators.stream()
                .filter(indicator -> indicator.getValue() == -2)
                .map(ContractIndicator::getIndicatorId)
                .collect(Collectors.toSet());

        failedIndicators2.addAll(lotIndicators.stream()
                .filter(indicator -> indicator.getValue() == -2)
                .map(LotIndicator::getIndicatorId)
                .collect(Collectors.toSet()));


        Double contractIndicatorsScore = Precision.round(contractIndicatorWithRisks.stream()
                .mapToDouble(ContractIndicator::getImpact).sum(), 2);

        Double lotIndicatorsScore = Precision.round(lotIndicatorWithRisks.stream()
                .mapToDouble(LotIndicator::getImpact).sum(), 2);

        Integer numberOfEligibleIndicators = allIndicatorsIds.size();

        ContractIndicatorsSummary indicatorsSummary = new ContractIndicatorsSummary();
        indicatorsSummary.setNumberOfEligibleIndicators(numberOfEligibleIndicators);
        indicatorsSummary.setNumberOfIndicatorsWithRisk(indicatorsWithRiskIds.size());
        indicatorsSummary.setNumberOfFailedIndicators(failedIndicators.size() + failedIndicators2.size());
        indicatorsSummary.setContractScore(contractIndicatorsScore + lotIndicatorsScore);

        return indicatorsSummary;
    }

    private List<IndicatorInfo> resolveIndicatorsInfos() {
        List<IndicatorInfo> indicatorInfos = uniqueContractIndicatorIds.stream()
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
                        druidIndicator.getLotIds().forEach(lotId -> {
                            LotIndicatorHistory history = new LotIndicatorHistory();
                            history.setIndicatorImpact(druidIndicator.getIndicatorImpact());
                            history.setLotOuterId(lotId);
                            history.setValue(druidIndicator.getIndicatorValue());
                            history.setDate(druidIndicator.getDate());
                            lotIndicatorHistory.add(history);
                        });
                        lotIndicatorHistory.sort((o1, o2) ->
                                o1.getDate().isBefore(o2.getDate()) ? 1 : -1);
                        return lotIndicatorHistory;
                    }).forEach(historicalIndicators::addAll);


            indicatorsHistory.put(lotIndicatorId, historicalIndicators);
        });

        return indicatorsHistory;
    }

    private Map<String, List<ContractIndicatorHistory>> resolveContractIndicatorsHistories() {
        Map<String, List<ContractIndicatorHistory>> indicatorsHistory = new HashMap<>();
        uniqueContractIndicatorIds.forEach(contractIndicatorId -> {
            List<ContractIndicatorHistory> historicalIndicators = new ArrayList<>();
            contractRawIndicators.stream()
                    .filter(rawIndicator -> rawIndicator.getIndicatorId()
                            .equals(contractIndicatorId))
                    .map(druidIndicator -> {
                        List<ContractIndicatorHistory> contractIndicatorHistory = new ArrayList<>();
                        ContractIndicatorHistory history = new ContractIndicatorHistory();
                        history.setIndicatorImpact(druidIndicator.getIndicatorImpact());
                        history.setValue(druidIndicator.getIndicatorValue());
                        history.setDate(druidIndicator.getDate());
                        contractIndicatorHistory.add(history);
                        contractIndicatorHistory.sort((o1, o2) ->
                                o1.getDate().isBefore(o2.getDate()) ? 1 : -1);
                        return contractIndicatorHistory;
                    }).forEach(historicalIndicators::addAll);


            indicatorsHistory.put(contractIndicatorId, historicalIndicators);
        });

        return indicatorsHistory;
    }
}
