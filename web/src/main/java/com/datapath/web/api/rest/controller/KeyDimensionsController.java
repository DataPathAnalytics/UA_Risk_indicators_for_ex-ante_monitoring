package com.datapath.web.api.rest.controller;

import com.datapath.web.api.version.ApiVersion;
import com.datapath.web.domain.KeyDimensions;
import com.datapath.web.services.KeyDimensionsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@CrossOrigin(origins = "*")
@RestController
public class KeyDimensionsController {

    private KeyDimensionsService keyDimensionsService;

    public KeyDimensionsController(KeyDimensionsService keyDimensionsService) {
        this.keyDimensionsService = keyDimensionsService;
    }

    @ApiVersion({0.1})
    @RequestMapping(value = "/key-dimensions", method = RequestMethod.GET)
    public KeyDimensions getKeyDimensions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate) {

        if (endDate == null) {
            endDate = ZonedDateTime.now(ZoneOffset.UTC);
        }

        if (startDate == null) {
            startDate = ZonedDateTime.now(ZoneOffset.UTC).withYear(2018).withMonth(1);
        }

        startDate = startDate.withZoneSameInstant(ZoneOffset.UTC);
        endDate = endDate.withZoneSameInstant(ZoneOffset.UTC);

        return  keyDimensionsService.getKeyDimensions(startDate, endDate);
    }

    @ApiVersion({0.1})
    @RequestMapping(value = "/key-dimensions/contracts", method = RequestMethod.GET)
    public KeyDimensions getContractsKeyDimensions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate) {

        if (endDate == null) {
            endDate = ZonedDateTime.now(ZoneOffset.UTC);
        }

        if (startDate == null) {
            startDate = ZonedDateTime.now(ZoneOffset.UTC).withYear(2018).withMonth(1);
        }

        startDate = startDate.withZoneSameInstant(ZoneOffset.UTC);
        endDate = endDate.withZoneSameInstant(ZoneOffset.UTC);

        return  keyDimensionsService.getContractsKeyDimensions(startDate, endDate);
    }


}

