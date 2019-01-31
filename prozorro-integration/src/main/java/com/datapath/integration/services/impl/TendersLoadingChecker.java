package com.datapath.integration.services.impl;

import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

@Component
public class TendersLoadingChecker {

    private TenderService tenderService;

    public TendersLoadingChecker(TenderService tenderService) {
        this.tenderService = tenderService;
    }

    public boolean isAlive() {
        ZonedDateTime dateModified = tenderService.findLastModifiedEntry().getDateModified();
        ZonedDateTime dayAgo = ZonedDateTime.now().minusHours(24);
        return dateModified.isAfter(dayAgo);
    }
}
