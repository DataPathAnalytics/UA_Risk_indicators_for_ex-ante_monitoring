package com.datapath.web.domain.contracting;

import lombok.Data;

import java.util.List;

@Data
public class ContractIndicator {

    private String indicatorId;
    private Byte value;
    private Double impact;
    private List<ContractIndicatorHistory> history;

}
