package com.datapath.indicatorsresolver.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class RequestsResponse {

    private List<RequestDTO> data;
    @JsonProperty("next_page")
    private Page nextPage;

    @Data
    public static class Page {
        private String uri;
    }
}
