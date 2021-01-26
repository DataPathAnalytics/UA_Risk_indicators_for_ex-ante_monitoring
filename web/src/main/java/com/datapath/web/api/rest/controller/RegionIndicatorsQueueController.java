package com.datapath.web.api.rest.controller;

import com.datapath.indicatorsqueue.services.IndicatorsQueueConfigurationService;
import com.datapath.persistence.entities.queue.IndicatorsQueueConfiguration;
import com.datapath.web.api.version.ApiVersion;
import com.datapath.web.domain.queue.IndicatorsQueueDataPage;
import com.datapath.web.services.RegionIndicatorsQueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
public class RegionIndicatorsQueueController {

    private RegionIndicatorsQueueService regionIndicatorsQueueService;
    private IndicatorsQueueConfigurationService indicatorsQueueConfigurationService;

    public RegionIndicatorsQueueController(RegionIndicatorsQueueService regionIndicatorsQueueService,
                                           IndicatorsQueueConfigurationService indicatorsQueueConfigurationService) {
        this.regionIndicatorsQueueService = regionIndicatorsQueueService;
        this.indicatorsQueueConfigurationService = indicatorsQueueConfigurationService;
    }

    @ApiVersion({0.1})
    @RequestMapping(value = "/region-indicators-queue", method = RequestMethod.GET)
    public IndicatorsQueueDataPage getIndicatorsQueue(HttpServletRequest request,
                                                      @RequestParam(required = false, defaultValue = "0") Integer page,
                                                      @RequestParam(required = false, defaultValue = "10") Integer limit,
                                                      @RequestParam(name = "region") String region) {

        String requestUrl = request.getRequestURL().toString();
        return regionIndicatorsQueueService.getIndicatorsQueue(requestUrl, page, limit, region);
    }

    @ApiVersion({0.1})
    @RequestMapping(value = "/region-indicators-queue/low", method = RequestMethod.GET)
    public IndicatorsQueueDataPage getLowIndicatorsQueue(HttpServletRequest request,
                                                         @RequestParam(required = false, defaultValue = "0") Integer page,
                                                         @RequestParam(required = false, defaultValue = "10") Integer limit,
                                                         @RequestParam(name = "region") String region) {

        String requestUrl = request.getRequestURL().toString();
        return regionIndicatorsQueueService.getLowIndicatorsQueue(requestUrl, page, limit, region);
    }

    @ApiVersion({0.1})
    @RequestMapping(value = "/region-indicators-queue/medium", method = RequestMethod.GET)
    public IndicatorsQueueDataPage getMediumIndicatorsQueue(HttpServletRequest request,
                                                            @RequestParam(required = false, defaultValue = "0") Integer page,
                                                            @RequestParam(required = false, defaultValue = "10") Integer limit,
                                                            @RequestParam(name = "region") String region) {

        String requestUrl = request.getRequestURL().toString();
        return regionIndicatorsQueueService.getMediumIndicatorsQueue(requestUrl, page, limit, region);
    }

    @ApiVersion({0.1})
    @RequestMapping(value = "/region-indicators-queue/high", method = RequestMethod.GET)
    public IndicatorsQueueDataPage getHighIndicatorsQueue(HttpServletRequest request,
                                                          @RequestParam(required = false, defaultValue = "0") Integer page,
                                                          @RequestParam(required = false, defaultValue = "10") Integer limit,
                                                          @RequestParam(name = "region") String region) {

        String requestUrl = request.getRequestURL().toString();
        return regionIndicatorsQueueService.getHighIndicatorsQueue(requestUrl, page, limit, region);
    }

    @ApiVersion({0.1})
    @RequestMapping(value = "/region-indicators-queue/regions", method = RequestMethod.GET)
    public Set<String> getRegions() {
        return regionIndicatorsQueueService.getRegions();
    }

    @ApiVersion({0.1})
    @RequestMapping(value = "/region-indicators-queue/configuration", method = RequestMethod.GET)
    public IndicatorsQueueConfiguration getConfiguration() {
        return indicatorsQueueConfigurationService.getConfigurationById(1);
    }

    @ApiVersion({0.1})
    @RequestMapping(value = "/region-indicators-queue/configuration", method = RequestMethod.POST)
    public IndicatorsQueueConfiguration updateConfiguration(@RequestBody IndicatorsQueueConfiguration indicatorsQueueConfiguration) {
        return indicatorsQueueConfigurationService.save(indicatorsQueueConfiguration);
    }
}

