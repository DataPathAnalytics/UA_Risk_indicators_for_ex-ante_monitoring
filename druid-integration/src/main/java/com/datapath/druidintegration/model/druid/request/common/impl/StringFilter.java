package com.datapath.druidintegration.model.druid.request.common.impl;

import lombok.Data;


@Data
public class StringFilter extends FilterImpl  {
    private String value;

    public StringFilter(String type, String dimension, String value){
        this.setType(type);
        this.setDimension(dimension);
        this.value = value;
    }
    public StringFilter(){}
}
