package com.datapath.elasticsearchintegration.constants;

/**
 * @author vitalii
 */

public enum ProcedureProperty {
    PROCEDURE_TYPE_KEYWORD  ("procedureType.keyword"),
    PROCEDURE_TYPE("procedureType"),
    TENDER_ID_KEYWORD("tenderId.keyword"),
    PROCURING_ENTITY_EDRPOU_KEYWORD  ("procuringEntityEDRPOU.keyword"),
    PROCURING_ENTITY_NAME_KEYWORD  ("procuringEntityName.keyword"),
    PROCURING_ENTITY_KIND_KEYWORD("procuringEntityKind.keyword"),
    INDICATORS_WITH_RISK_KEYWORD  ("indicatorsWithRisk.keyword"),
    INDICATORS_KEYWORD("indicators.keyword"),
    MONITORING_STATUS_KEYWORD("monitoringStatus.keyword"),
    GSW_KEYWORD("gsw.keyword"),
    TENDER_STATUS_KEYWORD("tenderStatus.keyword"),
    REGION_KEYWORD  ("region.keyword"),
    MONITORING_OFFICE_KEYWORD("monitoringOffice.keyword"),
    CPV_NAME_KEYWORD("cpvName.keyword"),
    CPV_KEYWORD("cpv.keyword"),
    CPV2_NAME_KEYWORD("cpv2Name.keyword"),
    CPV2_KEYWORD("cpv2.keyword"),
    CURRENCY_KEYWORD("currency.keyword"),
    TENDER_RISK_SCORE("tenderRiskScore"),
    DATE_PUBLISHED("datePublished"),
    HAS_PRIORITY_STATUS  ("hasPriorityStatus"),
    HAS_COMPLAINS("hasComplaints"),
    MONITORING_APPEAL("monitoringAppeal"),
    MONITORING_CAUSE("monitoringCause.keyword"),
    EXPECTED_VALUE  ("expectedValue"),
    ;

    private final String value;

    ProcedureProperty(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }

}