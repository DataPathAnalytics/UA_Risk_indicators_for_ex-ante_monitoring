package com.datapath.web.api.rest.controller;

import com.datapath.web.api.version.ApiVersion;
import com.datapath.web.domain.Indicator;
import com.datapath.web.dto.IndicatorStatistic;
import com.datapath.web.services.impl.IndicatorService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class IndicatorController {

    private IndicatorService indicatorService;

    public IndicatorController(IndicatorService indicatorService) {
        this.indicatorService = indicatorService;
    }

    @ApiVersion({0.1})
    @RequestMapping(value = "/indicators", method = RequestMethod.GET)
    public List<Indicator> getIndicators() {
        return indicatorService.getIndicators();
    }

    @ApiVersion({0.1})
    @RequestMapping(value = "/statistic/indicators", method = RequestMethod.GET)
    public List<IndicatorStatistic> getIndicatorsStatistic() {
        return indicatorService.getIndicatorsStatistic();
    }
}
