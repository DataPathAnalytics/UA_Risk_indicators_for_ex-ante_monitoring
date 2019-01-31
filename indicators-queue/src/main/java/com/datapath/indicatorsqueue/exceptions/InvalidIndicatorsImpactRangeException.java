package com.datapath.indicatorsqueue.exceptions;

import com.datapath.indicatorsqueue.domain.IndicatorsImpactRange;

public class InvalidIndicatorsImpactRangeException extends RuntimeException {

    public InvalidIndicatorsImpactRangeException(IndicatorsImpactRange impactRange) {
        super(String.format("Indicators impact range %s is not valid.", impactRange.toString()));
    }

}
