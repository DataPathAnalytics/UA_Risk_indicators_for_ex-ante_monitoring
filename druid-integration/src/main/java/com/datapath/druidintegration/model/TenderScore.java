package com.datapath.druidintegration.model;

import lombok.Data;

@Data
public class TenderScore {
    private String outerId;
    private String tenderId;
    private Double score;
    private Double expectedValue;
    private Double impact;
    private String region;
    private String procuringEntityId;
}
