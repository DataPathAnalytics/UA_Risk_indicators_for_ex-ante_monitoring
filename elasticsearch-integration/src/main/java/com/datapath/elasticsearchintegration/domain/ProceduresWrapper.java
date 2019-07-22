package com.datapath.elasticsearchintegration.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProceduresWrapper {
    private Long totalCount;
    private List<TenderIndicatorsCommonInfo> procedures;

    public ProceduresWrapper() {
        procedures = new ArrayList<>();
    }
}
