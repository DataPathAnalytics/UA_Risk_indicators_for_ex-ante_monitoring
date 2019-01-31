package com.datapath.web.domain.tendering;

import lombok.Data;

import java.util.List;

@Data
public class TenderIndicators {

    private List<TenderIndicator> tenderIndicators;
    private List<LotIndicator> lotIndicators;

}
