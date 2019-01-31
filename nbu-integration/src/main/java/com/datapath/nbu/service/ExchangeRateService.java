package com.datapath.nbu.service;

import com.datapath.persistence.entities.nbu.ExchangeRate;
import com.datapath.persistence.entities.nbu.ExchangeRateId;
import com.datapath.persistence.repositories.nbu.ExchangeRateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ExchangeRateService {

    private ExchangeRateRepository exchangeRateRepository;

    public ExchangeRateService(ExchangeRateRepository exchangeRateRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
    }

    public ExchangeRate getOneByCodeAndDate(String code, ZonedDateTime date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        ExchangeRateId id = new ExchangeRateId();
        id.setId(Integer.parseInt(date.format(formatter)));
        id.setCode(code);

        return exchangeRateRepository.findOneById(id);
    }

    public Optional<ExchangeRate> getLatest() {
        return exchangeRateRepository.findFirstByOrderByDateDesc();
    }

    public List<ExchangeRate> save(List<ExchangeRate> exchangeRates) {
        return exchangeRateRepository.saveAll(exchangeRates);
    }
}
