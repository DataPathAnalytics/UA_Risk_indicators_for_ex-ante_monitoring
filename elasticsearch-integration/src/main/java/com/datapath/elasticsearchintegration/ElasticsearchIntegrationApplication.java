package com.datapath.elasticsearchintegration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
//@PropertySource("file:application.properties")
@ComponentScan(basePackages = {"com.datapath"})
@EntityScan(basePackages = {"com.datapath"})
@EnableJpaRepositories(basePackages = {"com.datapath"})
public class ElasticsearchIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElasticsearchIntegrationApplication.class, args);
    }
}
