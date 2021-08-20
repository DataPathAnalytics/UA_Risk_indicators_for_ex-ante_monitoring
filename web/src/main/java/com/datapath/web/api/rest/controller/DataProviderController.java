package com.datapath.web.api.rest.controller;

import com.datapath.web.dto.TendersInfoResponse;
import com.datapath.web.services.DataProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.time.ZonedDateTime;

@RestController
@CrossOrigin(origins = "*")
public class DataProviderController {

    @Autowired
    private DataProviderService service;

    @GetMapping("providers/tenders")
    public TendersInfoResponse getTendersInfo(
            @Validated @NotNull @RequestParam(value = "since") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime since,
            @RequestParam(name = "size", defaultValue = "100") int size) {
        return service.getTendersInfo(since, size);
    }

    @GetMapping("providers/tenders/monitoring")
    public TendersInfoResponse getTendersMonitoringInfo(
            @Validated @NotNull @RequestParam(value = "since") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime since,
            @RequestParam(name = "size", defaultValue = "100") int size) {
        return service.getTendersMonitoringInfo(since, size);
    }

    @GetMapping("providers/tenders/queue")
    public TendersInfoResponse getTendersQueueInfo(
            @Validated @NotNull @RequestParam(value = "since") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime since,
            @RequestParam(name = "size", defaultValue = "100") int size) {
        return service.getTendersQueueInfo(since, size);
    }

}
