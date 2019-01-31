package com.datapath.web.domain.common;

public enum IndicatorRisk {

    UNFAIR_COMPETITIVITY_BETWEEN_BIDDERS("Недобросовісна конкуренція серед учасників"),
    DISCRIMINATION_OF_BIDDERS("Дискримінація учасників в закупівлі");

    private String indicatorRisk;

    IndicatorRisk(String name) {
        this.indicatorRisk = name;
    }

    @Override
    public String toString() {
        return indicatorRisk;
    }
}
