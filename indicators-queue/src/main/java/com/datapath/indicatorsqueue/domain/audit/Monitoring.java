package com.datapath.indicatorsqueue.domain.audit;

import lombok.Data;
import lombok.ToString;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@ToString
public class Monitoring {

    private String id;
    private String status;
    private List<String> causes;
    private boolean appeal;
    private ZonedDateTime startDate;
    private ZonedDateTime endDate;
    private String monitoringId;
    private String office;

}
