package com.datapath.web.domain.tendering;

import lombok.Data;

import java.util.List;

@Data
public class TenderIndicator {

//    private String indicatorId;
    private String indicatorCode;
    private String status;
    private Byte value;
    private Double impact;
    private List<TenderIndicatorHistory> history;

}
