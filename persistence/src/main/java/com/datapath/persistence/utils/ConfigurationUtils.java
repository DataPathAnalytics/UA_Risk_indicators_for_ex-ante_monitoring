package com.datapath.persistence.utils;

import lombok.Data;

@Data
public class ConfigurationUtils {

    public final static String TENDER_SCORE_COEFFICIENT_KEY = "tenderScore";
    public final static String EXPECTED_VALUE_COEFFICIENT_KEY = "expectedValue";

    public final static String TENDERS_COMPLETED_DAYS_KEY = "tendersCompletedDays";

    public final static String BUCKET_RISK_GROUP_MEDIUM_LEFT_KEY = "bucketRiskGroupMediumLeft";
    public final static String BUCKET_RISK_GROUP_MEDIUM_RIGHT_KEY = "bucketRiskGroupMediumRight";

    public final static String LOW_TOP_RISK_PERCENTAGE = "low_top_risk_percentage";
    public final static String MEDIUM_TOP_RISK_PERCENTAGE = "medium_top_risk_percentage";
    public final static String HIGH_TOP_RISK_PERCENTAGE = "high_top_risk_percentage";
    public final static String PROCURING_ENTITY_PERCENTAGES = "procuring_entity_percentages";

    public final static Integer CONFIGURATION_ID = 1;
}
