package com.datapath.web.dto.bucket;

import lombok.Data;

import java.util.List;

@Data
public class TenderIdsWrapper {

    private List<String> tenderIds;

    private List<String> columns;
}
