package com.datapath.web.dto;

import com.datapath.web.util.TenderScoreRank;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
@Builder
public class TenderInfo {

    public String cpv;
    public String cpv2;
    public String region;
    public String cpvName;
    public String cpv2Name;
    public String tenderId;
    public String tenderName;
    public String currency;
    public String tenderStatus;
    public String tenderOuterId;
    public String procedureType;
    public String monitoringStatus;
    public String procuringEntityName;
    public String procuringEntityKind;
    public String procuringEntityEDRPOU;

    public Double expectedValue;
    public Double materialityScore;
    public Double tenderRiskScore;
    public TenderScoreRank tenderRiskScoreRank;

    public boolean inQueue;
    public boolean hasComplaints;
    public boolean hasPriorityStatus;

    public String tenderDateModified;
    public String monitoringDateModified;
    public String queueDateModified;

    public String datePublished;
    public List<String> monitoringCause;
    public boolean monitoringAppeal;
    public String monitoringAppealAsString;
    public String monitoringOffice;
    public String monitoringId;

    public Set<String> indicators;
    public Set<String> indicatorsWithRisk;
    public Set<String> indicatorsWithOutRisk;
    public List<KeyValueObject> indicatorsWithRiskMapped;

    private String gsw;
    private String procurementMethodType;
    private String procurementMethodRationale;

    private boolean hasQueueHistory;

}
