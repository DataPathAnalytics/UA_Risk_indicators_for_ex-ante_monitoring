package com.datapath.elasticsearchintegration.domain.charts;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author vitalii
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopRegion {
    private String name;
    private Double allWithRiskCount;
    private Double prioritizedOfThem;
}
