package com.datapath.druidintegration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class ContractsFilter {

    private String contractId;
    private List<String> indicatorIds;
    private List<String> procedureTypes;

    public ContractsFilter(){}

}
