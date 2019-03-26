package com.datapath.nbu.scheduling;

import com.datapath.nbu.service.ExchangeRateProcessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
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
        exchangeRateProcessor.processExchangeRateForAllTime();
    }
}
