package com.datapath.web.api.rest.controller;

import com.datapath.persistence.repositories.TenderRepository;
import com.datapath.web.dto.SystemMonitoring;
import com.datapath.web.services.impl.IndicatorService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;

@RestController
@AllArgsConstructor
public class SystemMonitoringController {

    private final IndicatorService indicatorService;
    private final TenderRepository tenderRepository;

    @GetMapping("system-monitoring")
    public SystemMonitoring getSystemMonitoring() {
        SystemMonitoring monitoring = new SystemMonitoring();
        monitoring.setIndicatorStatistics(indicatorService.getIndicatorsStatistic());

        ZonedDateTime maxDateModified = tenderRepository.findMaxDateModified();
        ZonedDateTime maxDateCreated = tenderRepository.findMaxDateCreated();

        ZonedDateTime lastUploadedTenderDate = maxDateModified.isAfter(maxDateCreated) ?
                maxDateModified :
                maxDateCreated;

        monitoring.setLastUploadedTenderDate(lastUploadedTenderDate);

        return monitoring;
    }
}
