package com.datapath.elasticsearchintegration.domain;

import com.datapath.elasticsearchintegration.constants.TenderScoreRank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * @author vitalii
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenderIndicatorsCommonInfo {
    public String gsw;
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
    public Double tenderRiskScore;
    public String tenderRiskScoreRank;

    public boolean inQueue;
    public boolean hasComplaints;
    public boolean hasPriorityStatus;

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

    public void initScoreRank() {
        if (tenderRiskScore == null) {
            return;
        }
        if (tenderRiskScore < 0.5D) {
            tenderRiskScoreRank = TenderScoreRank.LOW.value();
        } else if (tenderRiskScore > 1.1D) {
            tenderRiskScoreRank = TenderScoreRank.HIGH.value();
        } else {
            tenderRiskScoreRank = TenderScoreRank.MEDIUM.value();
        }
    }

}
