package com.datapath.indicatorsresolver.model;

import com.datapath.persistence.entities.Indicator;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ContractIndicator {
    ContractDimensions contractDimensions;
    Indicator indicator;
    Integer value;
}
