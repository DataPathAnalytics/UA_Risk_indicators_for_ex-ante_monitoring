package com.datapath.auditorsindicators.coordinator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

import java.time.ZoneId;
import java.util.TimeZone;

@Slf4j
@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = {"com.datapath"})
@EntityScan(basePackages = {"com.datapath"})
@EnableJpaRepositories(basePackages = {"com.datapath"})
public class CoordinatorApplication {

    private static final ZoneId DEFAULT_TIMEZONE = ZoneId.of("UTC");

    static {
        TimeZone.setDefault(TimeZone.getTimeZone(DEFAULT_TIMEZONE));
    }

    public static void main(String[] args) {
        SpringApplication.run(CoordinatorApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setReadTimeout(180000);
        factory.setConnectTimeout(180000);
        return new RestTemplate(factory);
    }

}
