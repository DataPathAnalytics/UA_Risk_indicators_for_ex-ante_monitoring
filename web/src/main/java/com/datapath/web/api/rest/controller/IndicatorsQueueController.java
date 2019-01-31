package com.datapath.web.api.rest.controller;

import com.datapath.indicatorsqueue.services.IndicatorsQueueConfigurationService;
import com.datapath.persistence.entities.queue.IndicatorsQueueConfiguration;
import com.datapath.web.api.version.ApiVersion;
import com.datapath.web.domain.queue.IndicatorsQueueDataPage;
import com.datapath.web.services.IndicatorsQueueService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class IndicatorsQueueController {

    private IndicatorsQueueService indicatorsQueueService;
    private IndicatorsQueueConfigurationService indicatorsQueueConfigurationService;

    public IndicatorsQueueController(IndicatorsQueueService indicatorsQueueService,
                                     IndicatorsQueueConfigurationService indicatorsQueueConfigurationService) {
        this.indicatorsQueueService = indicatorsQueueService;
        this.indicatorsQueueConfigurationService = indicatorsQueueConfigurationService;
    }

    @ApiVersion({0.1})
    @RequestMapping(value = "/indicators-queue", method = RequestMethod.GET)
    public IndicatorsQueueDataPage getIndicatorsQueue(HttpServletRequest request,
                                                      @RequestParam(required = false, defaultValue = "0") Integer page,
                                                      @RequestParam(required = false, defaultValue = "10") Integer limit,
                                                      @RequestParam(required = false, name = "region") List<String> regions) {

        String requestUrl = request.getRequestURL().toString();
        return indicatorsQueueService.getIndicatorsQueue(requestUrl, page, limit, regions);
    }

    @ApiVersion({0.1})
    @RequestMapping(value = "/indicators-queue/low", method = RequestMethod.GET)
    public IndicatorsQueueDataPage getLowIndicatorsQueue(HttpServletRequest request,
                                                         @RequestParam(required = false, defaultValue = "0") Integer page,
                                                         @RequestParam(required = false, defaultValue = "10") Integer limit,
                                                         @RequestParam(required = false, name = "region") List<String> regions) {

        String requestUrl = request.getRequestURL().toString();
        return indicatorsQueueService.getLowIndicatorsQueue(requestUrl, page, limit, regions);
    }

    @ApiVersion({0.1})
    @RequestMapping(value = "/indicators-queue/medium", method = RequestMethod.GET)
    public IndicatorsQueueDataPage getMediumIndicatorsQueue(HttpServletRequest request,
                                                            @RequestParam(required = false, defaultValue = "0") Integer page,
                                                            @RequestParam(required = false, defaultValue = "10") Integer limit,
                                                            @RequestParam(required = false, name = "region") List<String> regions) {

        String requestUrl = request.getRequestURL().toString();
        return indicatorsQueueService.getMediumIndicatorsQueue(requestUrl, page, limit, regions);
    }

    @ApiVersion({0.1})
    @RequestMapping(value = "/indicators-queue/high", method = RequestMethod.GET)
    public IndicatorsQueueDataPage getHighIndicatorsQueue(HttpServletRequest request,
                                                          @RequestParam(required = false, defaultValue = "0") Integer page,
                                                          @RequestParam(required = false, defaultValue = "10") Integer limit,
                                                          @RequestParam(required = false, name = "region") List<String> regions) {

        String requestUrl = request.getRequestURL().toString();
        return indicatorsQueueService.getHighIndicatorsQueue(requestUrl, page, limit, regions);
    }

    @ApiVersion({0.1})
    @RequestMapping(value = "/indicators-queue/regions", method = RequestMethod.GET)
    public List<String> getRegions(@RequestParam(required = false) String impactCategory) {
        return indicatorsQueueService.getRegions(impactCategory);
    }

    @ApiVersion({0.1})
    @RequestMapping(value = "/indicators-queue/configuration", method = RequestMethod.GET)
    public IndicatorsQueueConfiguration getConfiguration() {
        return indicatorsQueueConfigurationService.getConfigurationById(1);
    }

    @ApiVersion({0.1})
    @RequestMapping(value = "/indicators-queue/configuration", method = RequestMethod.POST)
    public IndicatorsQueueConfiguration updateConfiguration(@RequestBody IndicatorsQueueConfiguration indicatorsQueueConfiguration) {
        return indicatorsQueueConfigurationService.save(indicatorsQueueConfiguration);
    }
}

