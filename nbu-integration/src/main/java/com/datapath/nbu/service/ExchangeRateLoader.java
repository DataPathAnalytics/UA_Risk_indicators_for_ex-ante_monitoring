package com.datapath.nbu.service;

import com.datapath.persistence.entities.nbu.ExchangeRate;
import com.datapath.persistence.entities.nbu.ExchangeRateId;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ExchangeRateLoader {

    private static final String API_URL = "https://bank.gov.ua/NBUStatService/v1/statdirectory/exchange";

    private RestTemplate restTemplate;

    public ExchangeRateLoader(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<ExchangeRate> loadExchangeRatesByDate(ZonedDateTime date) {
        String formattedDate = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        UriBuilder builder = UriComponentsBuilder.fromHttpUrl(API_URL);
        builder.queryParam("date", formattedDate);
        builder.queryParam("json");
        URI requestURI = builder.build();

        log.info("Load exchange rate for date {}", date);

        String exchangeRateResponse = restTemplate.getForObject(
                requestURI, String.class);

        List<ExchangeRate> exchangeRates = parseExchangeRateResponse(exchangeRateResponse);

        log.info("Exchange rates size - {}", exchangeRates.size());

        return exchangeRates;
    }

    @Nullable
    private List<ExchangeRate> parseExchangeRateResponse(String response) {
        try {
            List<ExchangeRate> exchangeRates = new ArrayList<>();
            JsonNode rootNode = new ObjectMapper().readTree(response);
            for (JsonNode node : rootNode) {
                String currency = node.at("/txt").asText();
                Double rate = node.at("/rate").asDouble();
                String code = node.at("/cc").asText();
                String rawDateStr = node.at("/exchangedate").asText()
                        .replace(".", "-");

                SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
                Date rawDate = formatter.parse(rawDateStr);
                ZonedDateTime date = ZonedDateTime.ofInstant(rawDate.toInstant(),
                        ZoneId.of("Europe/Kiev"));

                String[] dateParts = rawDateStr.split("-");
                String dateStr = dateParts[2] + dateParts[1] + dateParts[0];

                ExchangeRateId exchangeRateId = new ExchangeRateId();
                exchangeRateId.setId(Integer.parseInt(dateStr));
                exchangeRateId.setCode(code);

                ExchangeRate exchangeRate = new ExchangeRate();
                exchangeRate.setId(exchangeRateId);
                exchangeRate.setCurrency(currency);
                exchangeRate.setDate(date);
                exchangeRate.setRate(rate);

                exchangeRates.add(exchangeRate);
            }
            return exchangeRates;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Cant't parse NBU api response: {}", response);
            return null;
        }
    }
}
