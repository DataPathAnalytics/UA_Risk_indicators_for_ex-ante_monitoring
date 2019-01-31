package com.datapath.integration.domain;

import com.datapath.integration.serializers.ModifiedDateDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class ContractUpdateInfo {

    private String id;

    @JsonDeserialize(using = ModifiedDateDeserializer.class)
    private ZonedDateTime dateModified;

}
