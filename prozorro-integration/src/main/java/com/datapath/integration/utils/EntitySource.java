package com.datapath.integration.utils;

public enum EntitySource {

    TENDERING("tendering"),
    CONTRACTING("contracting");

    private String sourceName;

    EntitySource(String sourceName) {
        this.sourceName = sourceName;
    }

    @Override
    public String toString() {
        return this.sourceName;
    }

}
