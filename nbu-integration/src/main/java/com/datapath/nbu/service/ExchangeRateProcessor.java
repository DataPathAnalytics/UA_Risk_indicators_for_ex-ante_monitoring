package com.datapath.nbu.service;

import com.datapath.persistence.entities.nbu.ExchangeRate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ExchangeRateProcessor {

    private ExchangeRateLoader exchangeRateLoader;
    private ExchangeRateService exchangeRateService;

    public ExchangeRateProcessor(ExchangeRateLoader exchangeRateLoader,
                                 ExchangeRateService exchangeRateService) {

        this.exchangeRateLoader = exchangeRateLoader;
        this.exchangeRateService = exchangeRateService;
    }

    public void processExchangeRateForCurrentDate() {
        log.info("Process exchange rate for current date...");
        ZonedDateTime today = getCurrentDate();

        List<ExchangeRate> newExchangeRates = exchangeRateLoader.loadExchangeRatesByDate(today);

        List<ExchangeRate> savedExchangeRates = exchangeRateService.save(newExchangeRates);
        log.info("Exchange rates for date {} successfully saved. Rates size {}", today, savedExchangeRates.size());
    }

    public void processExchangeRateForAllTime() {
        ZonedDateTime endDate = getCurrentDate();
        ZonedDateTime startDate = getCurrentDate().minusYears(8);

        Optional<ExchangeRate> latestExchangeRate = exchangeRateService.getLatest();
        if (latestExchangeRate.isPresent()) {
            startDate = latestExchangeRate.get().getDate();
        }

        while (startDate.isBefore(endDate) || startDate.equals(endDate)) {
            log.info("Process exchange rate for current date...");
            List<ExchangeRate> exchangeRates = exchangeRateLoader.loadExchangeRatesByDate(startDate);
            exchangeRateService.save(exchangeRates);
            log.info("Exchange rates for date {} successfully saved", startDate);
            startDate = startDate.plusDays(1);
        }
    }

    private ZonedDateTime getCurrentDate() {
        return ZonedDateTime.now(ZoneId.of("Europe/Kiev"));
    }

}
