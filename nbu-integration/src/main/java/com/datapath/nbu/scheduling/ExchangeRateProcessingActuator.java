package com.datapath.nbu.scheduling;

import com.datapath.nbu.service.ExchangeRateProcessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExchangeRateProcessingActuator implements InitializingBean {

    private ExchangeRateProcessor exchangeRateProcessor;

    public ExchangeRateProcessingActuator(ExchangeRateProcessor exchangeRateProcessor) {
        this.exchangeRateProcessor = exchangeRateProcessor;
    }

    @Override
    public void afterPropertiesSet() {
        processExchangeRateForAllTime();
    }

    @Scheduled(cron = "0 0 * * * *")
    public void processExchangeRateForCurrentDate() {
        exchangeRateProcessor.processExchangeRateForCurrentDate();
    }


    public void processExchangeRateForAllTime() {
        exchangeRateProcessor.processExchangeRateForAllTime();
    }
}
