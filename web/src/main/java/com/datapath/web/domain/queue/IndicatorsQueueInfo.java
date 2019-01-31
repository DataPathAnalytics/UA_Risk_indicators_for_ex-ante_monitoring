package com.datapath.web.domain.queue;

import com.datapath.indicatorsqueue.domain.IndicatorsImpactRange;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class IndicatorsQueueInfo {

    private Long queueId;
    private String impactCategory;
    private IndicatorsImpactRange tenderScoreRange;
    private Integer numberOfTopRiskedTenders;
    private Double topRiskPercentage;
    private Double topRiskProcuringEntityPercentage;
    private Double expectedValueImportanceCoefficient;
    private Double tenderScoreImportanceCoefficient;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private ZonedDateTime dateCreated;
}
