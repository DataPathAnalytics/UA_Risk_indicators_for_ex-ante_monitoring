package com.datapath.druidintegration.model.druid.request.common.impl;

import lombok.Data;

import java.util.List;

@Data
public class ListIntFilter extends FilterImpl {
    private List<Integer> values;

    public ListIntFilter(String type, String dimension, List<Integer> values){
        this.setType(type);
        this.setDimension(dimension);
        this.values = values;
    }
    public ListIntFilter(){}
}
