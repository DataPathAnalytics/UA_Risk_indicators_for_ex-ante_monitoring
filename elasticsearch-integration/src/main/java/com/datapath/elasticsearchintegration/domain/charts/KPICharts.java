package com.datapath.elasticsearchintegration.domain.charts;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class KPICharts {

    private List<String> dates = new ArrayList<>();
    private List<Long> proceduresCount = new ArrayList<>();
    private List<Double> proceduresAmount = new ArrayList<>();
    private List<Long> riskedProceduresCount = new ArrayList<>();
    private List<Double> riskedProceduresAmount = new ArrayList<>();
    private List<Double> partsRiskedProcedures = new ArrayList<>();
    private List<Double> addressedProceduresAmount = new ArrayList<>();

}
