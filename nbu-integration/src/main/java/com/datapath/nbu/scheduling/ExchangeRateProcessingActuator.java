package com.datapath.nbu.scheduling;

import com.datapath.nbu.service.ExchangeRateProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ExchangeRateProcessingActuator implements InitializingBean {

    @Value("${com.datapath.scheduling.enabled}")
    private boolean schedulingEnabled;

    private ExchangeRateProcessor exchangeRateProcessor;

    public ExchangeRateProcessingActuator(ExchangeRateProcessor exchangeRateProcessor) {
        this.exchangeRateProcessor = exchangeRateProcessor;
    }

    @Override
    public void afterPropertiesSet() {
        if (schedulingEnabled) {
            processExchangeRateForAllTime();
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    public void processExchangeRateForCurrentDate() {
        exchangeRateProcessor.processExchangeRateForCurrentDate();
    }


    public void processExchangeRateForAllTime() {
        try {
            exchangeRateProcessor.processExchangeRateForAllTime();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }
}
