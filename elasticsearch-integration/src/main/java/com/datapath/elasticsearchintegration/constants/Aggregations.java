package com.datapath.elasticsearchintegration.constants;

/**
 * @author vitalii
 */

public enum Aggregations {
    PROCURING_ENTITY_KIND("procuringEntityKindAggs"),
    PROCURING_ENTITY_NAME("procuringEntityNameAggs"),
    CODE_SUB_AGGREGATION("codeSubAggs"),
    MONITORING_STATUS("monitoringStatusAggs"),
    GSW("gswAggs"),
    TENDER_STATUS("tenderStatusAggs"),
    PROCEDURE_TYPE("procedureTypeAggs"),
    COMPLAINS("complaintsAggs"),
    INDICATORS_WITH_RISK("indicatorsWithRiskAggs"),
    REGION("regionAggs"),
    REGION_AMOUNT("regionAggsSum"),
    MONITORING_OFFICE("officeAggs"),
    CPV_NAME("cpvNameAggs"),
    CPV2_NAME("cpv2NameAggs"),
    CURRENCY("currencyAggs"),
    TENDER_RISK_SCORE("tenderRiskScoreAggs"),
    MONITORING_APPEAL("monitoringAppealAggs"),
    MONITORING_CAUSE("monitoringCauseAggs"),
    AMOUNT_OF_RISK("amountOfRisc"),
    DAYS("days"),
    WITH_RISK_COUNT("withRisk"),
    WITH_PRIORITY_COUNT("prioritized"),
    PROCEDURES("procedures"),
    RISK_INDICATORS("riskIndicators"),
    RISK_INDICATORS_AMOUNT("riskIndicatorsAmount"),
    TEMP("temp");

    private final String value;

    Aggregations(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }


}