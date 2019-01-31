package com.datapath.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = "com.datapath")
@EntityScan(basePackages = "com.datapath")
@EnableJpaRepositories(basePackages = "com.datapath")
@EnableScheduling
@EnableAsync
public class ProzorroIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProzorroIntegrationApplication.class);
    }
}
