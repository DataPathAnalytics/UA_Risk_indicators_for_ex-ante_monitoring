package com.datapath.integration.scheduling;

import com.datapath.integration.utils.DateUtils;
import com.datapath.integration.validation.TenderExistenceValidator;
import com.datapath.integration.validation.TenderUpdatesValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Slf4j
@Component
public class TendersValidationScheduler {

    private static final long EXISTENCE_VALIDATION_FIXED_DELAY = 43_200_000;
    private static final long EXISTENCE_VALIDATION_INITIAL_DELAY = 10_000;

    private static final long UPDATE_VALIDATION_FIXED_DELAY = 21_600_000;

    private static final ZonedDateTime START_ALL_TENDER_VALIDATION_DATE = ZonedDateTime.now(ZoneId.of("UTC"))
            .withYear(2010)
            .withMonth(1)
            .withDayOfMonth(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0);

    private TenderExistenceValidator tenderExistenceValidator;
    private TenderUpdatesValidator tenderUpdatesValidator;

    public TendersValidationScheduler(TenderExistenceValidator tenderExistenceValidator,
                                      TenderUpdatesValidator tenderUpdatesValidator) {
        this.tenderExistenceValidator = tenderExistenceValidator;
        this.tenderUpdatesValidator = tenderUpdatesValidator;
    }

    @Scheduled(fixedDelay = EXISTENCE_VALIDATION_FIXED_DELAY, initialDelay = EXISTENCE_VALIDATION_INITIAL_DELAY)
    public void validateTenderExistence() {
        tenderExistenceValidator.validate(DateUtils.monthEarlierFromNow());
    }

    //TODO move to properties
    @Scheduled(cron = "0 0 4 ? * 0")
    public void validateAllTenderExistence() {
        tenderExistenceValidator.validate(DateUtils.yearEarlierFromNow());
    }

    @Scheduled(fixedDelay = UPDATE_VALIDATION_FIXED_DELAY)
    public void validateTenderUpdates() {
        log.info("Update validation scheduler started");
        tenderUpdatesValidator.validate(DateUtils.monthEarlierFromNow());
        log.info("Update validation scheduler finished");
    }

    //TODO move to properties
    @Scheduled(cron = "0 0 4 ? * 0")
    public void validateAllTenderUpdates() {
        tenderUpdatesValidator.validate(START_ALL_TENDER_VALIDATION_DATE);
    }
}
