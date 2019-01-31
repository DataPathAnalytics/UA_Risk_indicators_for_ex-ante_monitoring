package com.datapath.druidintegration.model.druid.request.common.impl;

import lombok.Data;

import java.util.List;

@Data
public class ListStringFilter extends FilterImpl {
    private List<String> values;

    public ListStringFilter(String type, String dimension, List<String> values){
        this.setType(type);
        this.setDimension(dimension);
        this.values = values;
    }
    public ListStringFilter(){}
}
