package com.datapath.integration.scheduling;

import com.datapath.integration.validation.TenderExistenceValidator;
import com.datapath.integration.validation.TenderUpdatesValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TendersValidationScheduler {

    @Value("${com.datapath.scheduling.enabled}")
    private boolean schedulingEnabled;

    private static final long VALIDATION_FIXED_DELAY = 43_200_000;
    private static final long VALIDATION_INITIAL_DELAY = 10_000;

    private TenderExistenceValidator tenderExistenceValidator;
    private TenderUpdatesValidator tenderUpdatesValidator;

    public TendersValidationScheduler(TenderExistenceValidator tenderExistenceValidator,
                                      TenderUpdatesValidator tenderUpdatesValidator) {
        this.tenderExistenceValidator = tenderExistenceValidator;
        this.tenderUpdatesValidator = tenderUpdatesValidator;
    }

    @Scheduled(fixedDelay = VALIDATION_FIXED_DELAY, initialDelay = VALIDATION_INITIAL_DELAY)
    public void validateTenderExistence() {
        tenderExistenceValidator.validate();
    }

    @Scheduled(cron = "0 0 */6 * * *")
    public void validateTenderUpdates() {
        tenderUpdatesValidator.validate();
    }
}
