package com.datapath.integration.scheduling;

import com.datapath.integration.services.AgreementUpdatesManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AgreementsUpdateScheduler {

    private static final int AGREEMENTS_UPDATES_RATE = 3_600_000;
    private static final int AGREEMENTS_UPDATES_DELAY = 10_000;

    private final AgreementUpdatesManager agreementUpdatesManager;

    public AgreementsUpdateScheduler(AgreementUpdatesManager agreementUpdatesManager) {
        this.agreementUpdatesManager = agreementUpdatesManager;
    }

    @Scheduled(fixedDelay = AGREEMENTS_UPDATES_RATE, initialDelay = AGREEMENTS_UPDATES_DELAY)
    public void checkAgreementsForUpdates() {
        agreementUpdatesManager.loadLastModifiedAgreements();
    }
}
