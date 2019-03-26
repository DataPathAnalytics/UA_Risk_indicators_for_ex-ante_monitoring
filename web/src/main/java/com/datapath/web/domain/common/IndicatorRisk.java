package com.datapath.web.domain.common;

public enum IndicatorRisk {

    UNFAIR_COMPETITIVITY_BETWEEN_BIDDERS("Недобросовісна конкуренція серед учасників"),
    LOW_ECONOMY_AND_EFFICIENCY("Низька ефективність та економія в закупівлях"),
    UNTRANSPARENT_PROCUREMENT("Непрозорість  закупівлі"),
    DISCRIMINATION_OF_BIDDERS("Дискримінація учасників в закупівлі"),
    UNOBJECTIVE_EVALUATION_OF_BIDDERS_PROPOSALS("Необ'єктивна та упереджена оцінка пропозицій учасників"),
    CORRUPTION_AND_ABUSE_IN_PROCUREMENT("Корупція і зловживання в закупівлях"),
    INCOSISTENCY_IN_LAW("Невідповідність закону");

    private String indicatorRisk;

    IndicatorRisk(String name) {
        this.indicatorRisk = name;
    }

    @Override
    public String toString() {
        return indicatorRisk;
    }
}
