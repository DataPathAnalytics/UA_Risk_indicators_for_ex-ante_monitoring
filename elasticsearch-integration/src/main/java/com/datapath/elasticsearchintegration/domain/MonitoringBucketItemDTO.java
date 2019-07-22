package com.datapath.elasticsearchintegration.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class MonitoringBucketItemDTO {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    List<TenderIndicatorsCommonInfo> tenders;
}
