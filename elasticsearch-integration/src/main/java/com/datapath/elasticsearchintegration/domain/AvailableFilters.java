package com.datapath.elasticsearchintegration.domain;

import com.datapath.elasticsearchintegration.constants.TenderScoreRank;
import com.datapath.elasticsearchintegration.util.Mapping;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.datapath.elasticsearchintegration.constants.RiskedProcedure.*;

/**
 * @author vitalii
 */
@Data
public class AvailableFilters {

    private List<KeyValueObject> riskedIndicators = new ArrayList<>();
    private List<KeyValueObject> riskedProcedures;
    private List<KeyValueObject> tenderRank = new ArrayList<>();
    private List<KeyValueObject> regions = new ArrayList<>();
    private List<KeyValueObject> cpv2Names = new ArrayList<>();
    private List<KeyValueObject> cpvNames = new ArrayList<>();
    private List<KeyValueObject> procedureTypes = new ArrayList<>();
    private List<KeyValueObject> currency = new ArrayList<>();
    private List<KeyValueObject> procuringEntities = new ArrayList<>();
    private List<KeyValueObject> tenderStatuses = new ArrayList<>();
    private List<KeyValueObject> procuringEntityKind = new ArrayList<>();
    private List<KeyValueObject> gsw = new ArrayList<>();
    private List<KeyValueObject> monitoringStatus = new ArrayList<>();
    private List<KeyValueObject> monitoringOffices = new ArrayList<>();
    private List<KeyValueObject> complaints = new ArrayList<>();
    private List<KeyValueObject> monitoringCause = new ArrayList<>();
    private List<KeyValueObject> monitoringAppeal = new ArrayList<>();

    public void setRiskedProcedures(List<KeyValueObject> riskedProcedures) {
        this.riskedProcedures = riskedProcedures.stream()
                .peek(item -> item.setKey(Mapping.RISKED_PROCEDURES.get((String) item.getKey())))
                .peek(item -> {

                    switch (((KeyValueObject) (item.getKey())).getKey().toString()) {
                        case "withRisk":
                            ((KeyValueObject) (item.getKey())).setKey(WITH_RISK);
                            break;
                        case "withoutRisk":
                            ((KeyValueObject) (item.getKey())).setKey(WITHOUT_RISK);
                            break;
                        case "withoutPriority":
                            ((KeyValueObject) (item.getKey())).setKey(WITH_RISK_NO_PRIORITY);
                            break;
                        case "withPriority":
                            ((KeyValueObject) (item.getKey())).setKey(WITH_RISK_HAS_PRIORITY);
                            break;
                        default:
                    }
                })
                .collect(Collectors.toList());
    }

    public void setTenderScoreRank(List<KeyValueObject> scores) {

        AtomicLong lowCount = new AtomicLong();
        scores.stream().filter(item -> (double) item.getKey() < 0.5D).forEach(item -> lowCount.addAndGet((Long) item.getValue()));
        if (lowCount.intValue() > 0) {
            tenderRank.add(new KeyValueObject(new KeyValueObject(TenderScoreRank.LOW, TenderScoreRank.LOW.value()), lowCount));
        }

        AtomicLong mediumCount = new AtomicLong();
        scores.stream().filter(item -> (double) item.getKey() >= 0.5D && (double) item.getKey() <= 1.1D).forEach(item -> mediumCount.addAndGet((Long) item.getValue()));
        if (mediumCount.intValue() > 0) {
            tenderRank.add(new KeyValueObject(new KeyValueObject(TenderScoreRank.MEDIUM, TenderScoreRank.MEDIUM.value()), mediumCount));
        }

        AtomicLong highCount = new AtomicLong();
        scores.stream().filter(item -> (double) item.getKey() > 1.1D).forEach(item -> highCount.addAndGet((Long) item.getValue()));
        if (highCount.intValue() > 0) {
            tenderRank.add(new KeyValueObject(new KeyValueObject(TenderScoreRank.HIGH, TenderScoreRank.HIGH.value()), highCount));
        }
        tenderRank.add(new KeyValueObject(new KeyValueObject(TenderScoreRank.All, TenderScoreRank.All.value()), highCount.longValue() + lowCount.longValue() + mediumCount.longValue()));
    }

    public void setTenderStatuses(List<KeyValueObject> tenderStatuses) {
        this.tenderStatuses = tenderStatuses.stream()
                .peek(item -> item.setKey(Mapping.TENDER_STATUS.get((String) item.getKey())))
                .collect(Collectors.toList());
    }

    public void setGsw(List<KeyValueObject> gsw) {
        this.gsw = gsw.stream()
                .peek(item -> item.setKey(Mapping.GSW.get((String) item.getKey())))
                .collect(Collectors.toList());

    }

    public void setProcedureTypes(List<KeyValueObject> procedureTypes) {

        this.procedureTypes = procedureTypes.stream()
                .peek(item -> {
                    KeyValueObject key = Mapping.PROCEDURE_TYPES.get(item.getKey());
                    if (item.getKey().equals("negotiation.quick")) {
                        key = Mapping.PROCEDURE_TYPES.get("negotiation");
                    }
                    item.setKey(key);
                })
                .collect(Collectors.groupingBy(KeyValueObject::getKey, Collectors.summingLong(it -> ((Long) it.getValue()))))
                .entrySet()
                .stream()
                .map(item -> new KeyValueObject(item.getKey(), item.getValue()))
                .collect(Collectors.toList());
    }

    public void setMonitoringStatus(List<KeyValueObject> monitoringStatus) {
        this.monitoringStatus = monitoringStatus.stream()
                .peek(item -> item.setKey(Mapping.MONITORING_STATUS.get((String) item.getKey())))
                .collect(Collectors.toList());
    }

    public void setProcuringEntityKind(List<KeyValueObject> procuringEntityKind) {
        this.procuringEntityKind = procuringEntityKind.stream()
                .peek(item -> item.setKey(Mapping.PROCURING_ENTITY_KIND.get((String) item.getKey())))
                .collect(Collectors.toList());
    }

    public void setComplaints(List<KeyValueObject> complaints) {
        this.complaints = complaints.stream()
                .peek(item -> item.setKey(Mapping.COMPLAINTS.get((String) item.getKey())))
                .collect(Collectors.toList());
    }

    public void setMonitoringAppeal(List<KeyValueObject> appeal) {
        this.monitoringAppeal = appeal.stream()
                .peek(item -> item.setKey(Mapping.APPEAL.get((String) item.getKey())))
                .collect(Collectors.toList());
    }

    public void setMonitoringCause(List<KeyValueObject> monitoringCause) {
        this.monitoringCause = monitoringCause.stream()
                .peek(item -> item.setKey(Mapping.MONITORING_CAUSE.get((String) item.getKey())))
                .collect(Collectors.toList());
    }
}
