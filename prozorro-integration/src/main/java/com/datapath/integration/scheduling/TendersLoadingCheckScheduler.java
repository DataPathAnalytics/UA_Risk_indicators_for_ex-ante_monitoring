package com.datapath.integration.scheduling;

import com.datapath.integration.services.impl.TendersLoadingChecker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TendersLoadingCheckScheduler {

    private TendersLoadingChecker tendersLoadingChecker;

    public TendersLoadingCheckScheduler(TendersLoadingChecker tendersLoadingChecker) {
        this.tendersLoadingChecker = tendersLoadingChecker;
    }

    @Scheduled(cron = "0 0 */3 * * *")
    public void checkTendersForUpdates() {
        try {
            boolean alive = tendersLoadingChecker.isAlive();
            if (!alive) {
                log.error("Tenders loading FAILED!!!");
            }
        } catch (Exception ex) {
            log.error("TendersLoadingCheckScheduler failed {}", ex.getMessage());
        }
    }
}
