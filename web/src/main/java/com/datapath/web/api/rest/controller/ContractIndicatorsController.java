package com.datapath.web.api.rest.controller;

import com.datapath.web.api.version.ApiVersion;
import com.datapath.web.domain.IndicatorsDataPage;
import com.datapath.web.domain.contracting.ContractIndicatorsData;
import com.datapath.web.services.impl.ContractIndicatorsDataService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class ContractIndicatorsController {

    private ContractIndicatorsDataService contractIndicatorsDataService;

    public ContractIndicatorsController(ContractIndicatorsDataService contractIndicatorsDataService) {
        this.contractIndicatorsDataService = contractIndicatorsDataService;
    }

    @ApiVersion({0.1})
    @RequestMapping(value = "/contracts/{contractId}", method = RequestMethod.GET)
    public ContractIndicatorsData getIndicatorsByTenderId(@PathVariable String contractId) {
        return contractIndicatorsDataService.getContractIndicators(contractId);
    }

    @ApiVersion({0.1})
    @RequestMapping(value = "/contracts", method = RequestMethod.GET)
    public IndicatorsDataPage getIndicatorsByDateRange(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false, defaultValue = "asc") String order,
            @RequestParam(required = false, defaultValue = "false") Boolean riskOnly,
            @RequestParam(required = false, name = "procedureType") List<String> procedureTypes,
            @RequestParam(required = false, name = "indicator") List<String> indicators) {

        if (endDate == null) {
            endDate = ZonedDateTime.now(ZoneOffset.UTC);
        }

        if (startDate == null) {
            startDate = ZonedDateTime.now(ZoneOffset.UTC).withYear(2018).withMonth(1);
        }

        startDate = startDate.withZoneSameInstant(ZoneOffset.UTC);
        endDate = endDate.withZoneSameInstant(ZoneOffset.UTC);

        String url = request.getRequestURL().toString();
        return contractIndicatorsDataService.getContractIndicators(
                startDate,
                endDate,
                limit,
                url,
                order,
                riskOnly,
                procedureTypes,
                indicators
        );
    }

}
