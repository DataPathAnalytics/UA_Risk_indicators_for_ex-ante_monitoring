package com.datapath.persistence;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Slf4j
@Component
public class DBInitializer implements InitializingBean {

    @Value("${com.datapath.scheduling.enabled}")
    private boolean schedulingEnabled;

    private DataSource dataSource;

    @Autowired
    public DBInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private void initIndicators() {
        log.info("Database tables initialization starts...");
        Resource indicatorsResource = new ClassPathResource("indicators.sql");
        Resource cpvResource = new ClassPathResource("cpvCatalogue.sql");
        Resource natureMonopolyProcuringEntity = new ClassPathResource("natureMonopolyProcuringEntity.sql");
        Resource queueRegions = new ClassPathResource("queueRegion.sql");
        Resource queueConfig = new ClassPathResource("queueConfig.sql");
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(indicatorsResource, cpvResource, natureMonopolyProcuringEntity, queueRegions, queueConfig);
        populator.execute(dataSource);
        log.info("Database tables initialization completed successfully");
    }

    @Override
    public void afterPropertiesSet() {
        if (schedulingEnabled) {
            initIndicators();
        }
    }
}
