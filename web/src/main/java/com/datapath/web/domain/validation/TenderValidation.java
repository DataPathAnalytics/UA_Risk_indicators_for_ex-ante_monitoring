package com.datapath.web.domain.validation;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

@Data
public class TenderValidation {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private ZonedDateTime date;
    private Integer existingTendersCount;
    private Integer missingTendersCount;
    private Integer testOrExpiredTendersCount;
    private List<String> missingTenders;
    private List<TenderValidationHistoryItem> history;

}
