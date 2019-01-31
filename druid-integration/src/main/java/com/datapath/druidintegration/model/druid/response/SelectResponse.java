package com.datapath.druidintegration.model.druid.response;

import com.datapath.druidintegration.model.druid.response.common.Event;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

@Data
public class SelectResponse {
    private String timestamp;
    private Result result;

    @Data
    public static class Result {
        private JsonNode pagingIdentifiers;
        private List<Events> events;

        @Data
        public static class Events {
            private Event event;
        }
    }
}
