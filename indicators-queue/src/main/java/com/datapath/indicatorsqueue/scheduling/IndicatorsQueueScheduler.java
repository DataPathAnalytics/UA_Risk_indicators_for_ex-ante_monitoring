package com.datapath.indicatorsqueue.scheduling;

import com.datapath.indicatorsqueue.services.IndicatorsQueueUpdaterService;
import com.datapath.indicatorsqueue.services.RegionIndicatorsQueueUpdaterService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class IndicatorsQueueScheduler {

    private IndicatorsQueueUpdaterService indicatorsQueueUpdaterService;
    private RegionIndicatorsQueueUpdaterService regionIndicatorsQueueUpdaterService;

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
}
