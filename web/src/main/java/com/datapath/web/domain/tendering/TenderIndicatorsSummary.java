package com.datapath.web.domain.tendering;

import com.datapath.web.domain.IndicatorsSummary;
import lombok.Data;

@Data
public class TenderIndicatorsSummary extends IndicatorsSummary {

    private Double tenderScore;

}
