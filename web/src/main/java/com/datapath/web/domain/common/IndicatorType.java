package com.datapath.web.domain.common;

public enum IndicatorType {

    LOT("lot"),
    TENDER("tender"),
    CONTRACT("contract");

    private String indicatorType;

    IndicatorType(String name) {
        this.indicatorType = name;
    }

    @Override
    public String toString() {
        return indicatorType;
    }
}
