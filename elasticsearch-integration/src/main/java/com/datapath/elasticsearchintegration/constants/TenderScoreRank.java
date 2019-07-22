package com.datapath.elasticsearchintegration.constants;

/**
 * @author vitalii
 */

public enum TenderScoreRank {
    All("Усі типи"),
    LOW("Низький ризик"),
    MEDIUM("Середній ризик"),
    HIGH("Високий ризик");

    private final String value;

    TenderScoreRank(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }

}