package com.datapath.integration.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class AgreementsPageResponse {
    @JsonProperty("next_page")
    private AgreementsPageResponse.Page nextPage;

    @JsonProperty("data")
    private List<AgreementUpdateInfo> items;

    @Data
    public class Page {
        private String path;
        private String uri;
        private String offset;
    }
}
