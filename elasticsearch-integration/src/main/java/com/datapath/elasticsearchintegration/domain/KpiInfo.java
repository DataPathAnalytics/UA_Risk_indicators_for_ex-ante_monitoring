package com.datapath.elasticsearchintegration.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author vitalii
 */
@Data
@NoArgsConstructor
public class KpiInfo {
    private Long proceduresCount;
    private Long riskProceduresCount;
    private Double proceduresValue;
    private Double riskProceduresValue;
    private Long riskProcuringEntitiesCount;
    private Long allProcuringEntitiesCount;
    private Long indicatorsCount;
    private Long riskIndicatorsCount;
    private Long monitoringCount;
    private Double monitoringValue;
}
