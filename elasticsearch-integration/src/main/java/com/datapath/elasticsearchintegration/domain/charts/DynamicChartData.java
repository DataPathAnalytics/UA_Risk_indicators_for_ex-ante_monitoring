package com.datapath.elasticsearchintegration.domain.charts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * @author vitalii
 */
@Data
@Builder
@AllArgsConstructor
public class DynamicChartData {

    private Date date;
    private String dateAsString;
    private Double totalCount;
    private Double countWithRisk;
    private Double countWithPriority;
    private Double countWithoutPriority;
}
