package com.datapath.web.domain.contracting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;

@Data
public class LotIndicator {

    private String indicatorId;
    private Byte value;
    private List<String> lots;
    private List<LotIndicatorHistory> history;

    @JsonIgnore
    private Double impact;

}
