package com.datapath.web.domain.common;

public enum IndicatorStage {

    TENDERING("Tendering"),
    QUALIFICATION("Qualification"),
    AUCTION("Auction"),
    AWARD("Award"),
    CONTRACT("Contact"),
    CONTRACT_IMPLEMENTATION("ContractImplementation");

    private String indicatorStage;

    IndicatorStage(String name) {
        this.indicatorStage = name;
    }

    @Override
    public String toString() {
        return indicatorStage;
    }
}
