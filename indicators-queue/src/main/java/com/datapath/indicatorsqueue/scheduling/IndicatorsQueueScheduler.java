package com.datapath.indicatorsqueue.scheduling;

import com.datapath.indicatorsqueue.services.IndicatorsQueueUpdaterService;
import com.datapath.indicatorsqueue.services.RegionIndicatorsQueueUpdaterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class IndicatorsQueueScheduler implements InitializingBean {

    @Value("${com.datapath.scheduling.enabled}")
    private boolean schedulingEnabled;

    private IndicatorsQueueUpdaterService indicatorsQueueUpdaterService;
    private RegionIndicatorsQueueUpdaterService regionIndicatorsQueueUpdaterService;

    public IndicatorsQueueScheduler(IndicatorsQueueUpdaterService indicatorsQueueUpdaterService,
                                    RegionIndicatorsQueueUpdaterService regionIndicatorsQueueUpdaterService) {
        this.indicatorsQueueUpdaterService = indicatorsQueueUpdaterService;
        this.regionIndicatorsQueueUpdaterService = regionIndicatorsQueueUpdaterService;
    }

    //    @Scheduled(cron = "0 0 0 * * *")
    public void updateIndicatorsQueue() {
        try {
            indicatorsQueueUpdaterService.updateIndicatorsQueue();
        } catch (Exception e) {
            log.error("Updating indicators queue failed.", e);
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    public void updateRegionIndicatorsQueue() {
        try {
            regionIndicatorsQueueUpdaterService.updateIndicatorsQueue();
        } catch (Exception e) {
            log.error("Updating indicators queue failed.", e);
        }
    }

    @Override
    public void afterPropertiesSet() {
        if (schedulingEnabled) {
            updateRegionIndicatorsQueue();
        }
    }
}
