package com.datapath.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

@Data
public class SystemMonitoring {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private ZonedDateTime lastUploadedTenderDate;
    private List<IndicatorStatistic> indicatorStatistics;
}
