package com.datapath.persistence.domain;

import com.datapath.persistence.utils.ConfigurationUtils;
import lombok.Setter;

import java.util.Map;

@Setter
public class ConfigurationDomain {

    private Map<String, String> configurations;

    public Double getExpectedValueCoefficient() {
        return Double.parseDouble(configurations.get(ConfigurationUtils.EXPECTED_VALUE_COEFFICIENT_KEY));
    }

    public Double getTenderScoreCoefficient() {
        return Double.parseDouble(configurations.get(ConfigurationUtils.TENDER_SCORE_COEFFICIENT_KEY));
    }

    public Long getTendersCompletedDays() {
        return Long.parseLong(configurations.get(ConfigurationUtils.TENDERS_COMPLETED_DAYS_KEY));
    }

    public Double getBucketRiskGroupMediumLeft() {
        return Double.parseDouble(configurations.get(ConfigurationUtils.BUCKET_RISK_GROUP_MEDIUM_LEFT_KEY));
    }

    public Double getBucketRiskGroupMediumRight() {
        return Double.parseDouble(configurations.get(ConfigurationUtils.BUCKET_RISK_GROUP_MEDIUM_RIGHT_KEY));
    }
}
