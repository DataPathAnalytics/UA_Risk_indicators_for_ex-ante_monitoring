package com.datapath.integration.domain;

import lombok.Data;
import lombok.ToString;

import java.time.ZonedDateTime;

@Data
@ToString(exclude = "data")
public class ContractResponseEntity {

    private String id;
    private ZonedDateTime dateModified;
    private String data;

}
