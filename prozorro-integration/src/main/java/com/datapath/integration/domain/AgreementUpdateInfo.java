package com.datapath.integration.domain;

import com.datapath.integration.serializers.ModifiedDateDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class AgreementUpdateInfo {

    private String id;
    @JsonDeserialize(using = ModifiedDateDeserializer.class)
    private ZonedDateTime dateModified;
}
