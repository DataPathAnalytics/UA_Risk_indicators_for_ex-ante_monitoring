package com.datapath.web.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.ToString;

import java.time.ZonedDateTime;

@Data
@ToString
public class Indicator {

    private String id;
    private String name;
    private String shortName;
    private Double impact;
    private String tenderLotType;
    private String[] procedureTypes;
    private String[] procedureStatuses;
    private String[] procuringEntityKind;
    private String risk;
    private Boolean isActive;
    private Integer checkingFrequency;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private ZonedDateTime lastCheckedDateCreated;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private ZonedDateTime dateChecked;
}
