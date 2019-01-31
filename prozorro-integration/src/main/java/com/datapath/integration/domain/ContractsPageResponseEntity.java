package com.datapath.integration.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ContractsPageResponseEntity {

    @JsonProperty("next_page")
    private Page nextPage;

    @JsonProperty("data")
    private List<ContractUpdateInfo> items;

    @Data
    public class Page {
        private String path;
        private String uri;
        private String offset;
    }
}
