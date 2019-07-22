package com.datapath.auditorsindicators.coordinator;

import com.datapath.elasticsearchintegration.services.TenderObjectsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

import java.io.IOException;
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
        ConfigurableApplicationContext applicationContext = SpringApplication.run(CoordinatorApplication.class, args);
        try {
            applicationContext.getBean(TenderObjectsProvider.class).init();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

}