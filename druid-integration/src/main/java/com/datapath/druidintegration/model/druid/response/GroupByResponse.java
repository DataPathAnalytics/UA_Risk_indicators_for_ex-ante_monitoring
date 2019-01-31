package com.datapath.druidintegration.model.druid.response;

import com.datapath.druidintegration.model.druid.response.common.Event;
import lombok.Data;

@Data
public class GroupByResponse {
    private String timestamp;
    private Event event;
}



