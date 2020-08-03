package com.datapath.integration.scheduling;

import com.datapath.integration.services.ContractUpdatesManager;
import com.datapath.integration.services.TenderUpdatesManager;
import com.datapath.integration.utils.ServiceStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class TendersUpdateScheduler {

    private static final int TENDERS_UPDATES_RATE = 180_000;
    private static final int CONTRACTS_UPDATES_RATE = 180_000;

    private final TenderUpdatesManager tenderUpdatesManager;
    private final ContractUpdatesManager contractUpdatesManager;

    @Scheduled(fixedDelay = TENDERS_UPDATES_RATE)
    public void checkTendersForUpdates() {
        tenderUpdatesManager.loadLastModifiedTenders();
    }

    @Scheduled(fixedDelay = CONTRACTS_UPDATES_RATE)
    public void checkContractsForUpdates() {
        log.info("Check contract updates scheduled started.");
        try {
            if (contractUpdatesManager.getServiceStatus() == ServiceStatus.ENABLED) {
                contractUpdatesManager.loadLastModifiedContracts();
            } else {
                log.info("ContractsUpdateManager disabled.");
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            this.contractUpdatesManager.changeServiceStatus(ServiceStatus.ENABLED);
        }
    }
}
