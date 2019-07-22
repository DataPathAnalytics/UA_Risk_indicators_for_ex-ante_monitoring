package com.datapath.elasticsearchintegration.constants;

/**
 * @author vitalii
 */

public enum RiskedProcedure {
    WITH_RISK("withRisk"),
    WITHOUT_RISK("withoutRisk"),
    WITH_RISK_HAS_PRIORITY("withPriority"),
    WITH_RISK_NO_PRIORITY("withoutPriority");

    private final String value;

    RiskedProcedure(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }

}