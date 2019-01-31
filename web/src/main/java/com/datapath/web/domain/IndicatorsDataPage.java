package com.datapath.web.domain;

import lombok.Data;

import java.util.List;

@Data
public class IndicatorsDataPage<T> {

    private IndicatorsPage nextPage;
    private List<T> data;

}
