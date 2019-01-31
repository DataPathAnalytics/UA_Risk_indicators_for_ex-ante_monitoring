package com.datapath.web.domain.tendering;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;

@Data
public class LotIndicator {

//    private String indicatorId;
    private String indicatorCode;
    private String status;
    private Byte value;
    private List<String> lots;
    private List<LotIndicatorHistory> history;

    @JsonIgnore
    private Double impact;
}
