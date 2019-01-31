package com.datapath.web.domain.contracting;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class LotIndicatorHistory {

    @JsonProperty("lotId")
    private String lotOuterId;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private ZonedDateTime date;
    private Byte value;
    private Double indicatorImpact;

}
