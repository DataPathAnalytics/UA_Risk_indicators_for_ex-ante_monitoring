package com.datapath.web.domain.common;

public enum ImpactCategory {

    MIXED("mixed"),
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high");

    private String categoryName;

    ImpactCategory(String categoryName) {
        this.categoryName = categoryName;
    }

    @Override
    public String toString() {
        return categoryName;
    }

    public String categoryName() {
        return categoryName;
    }
}
