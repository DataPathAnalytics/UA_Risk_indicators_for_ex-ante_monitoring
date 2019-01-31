package com.datapath.integration.scheduling;

import com.datapath.integration.validation.TenderUpdatesValidator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TendersValidationScheduler implements InitializingBean {

    private TenderUpdatesValidator tenderUpdatesValidator;

    public TendersValidationScheduler(TenderUpdatesValidator tenderUpdatesValidator) {
        this.tenderUpdatesValidator = tenderUpdatesValidator;
    }

    @Scheduled(cron = "0 0 */6 * * *")
    public void validateTenderUpdates() {
        tenderUpdatesValidator.validate();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        validateTenderUpdates();
    }
}
