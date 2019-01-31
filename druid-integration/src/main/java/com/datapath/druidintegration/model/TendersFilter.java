package com.datapath.druidintegration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class TendersFilter {

    private String tenderId;
    private List<String> procedureTypes;
    private List<String> indicatorIds;

    public TendersFilter(){}

}
