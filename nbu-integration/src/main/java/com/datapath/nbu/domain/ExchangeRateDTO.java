package com.datapath.nbu.domain;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class ExchangeRateDTO {

    private String txt;
    private String cc;
    private Double rate;
    private ZonedDateTime exchangedate;
}
