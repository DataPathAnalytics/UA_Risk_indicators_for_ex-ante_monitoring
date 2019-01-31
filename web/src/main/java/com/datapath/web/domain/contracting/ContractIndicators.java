package com.datapath.web.domain.contracting;

import lombok.Data;

import java.util.List;

@Data
public class ContractIndicators {

    private List<LotIndicator> lotIndicators;
    private List<ContractIndicator> contractIndicators;

}
