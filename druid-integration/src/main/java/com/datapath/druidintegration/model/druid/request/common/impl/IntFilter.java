package com.datapath.druidintegration.model.druid.request.common.impl;

import lombok.Data;

@Data
public class IntFilter extends FilterImpl{
    private Integer value;

    public IntFilter(String type, String dimension, Integer value){
        this.setType(type);
        this.setDimension(dimension);
        this.value = value;
    }
    public IntFilter(){}
}
