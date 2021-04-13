package com.datapath.auditorsindicators.coordinator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Slf4j
@Component
public class DBInitializer implements InitializingBean {

    private DataSource dataSource;

    @Autowired
    public DBInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private void initIndicators() {
        log.info("Database tables initialization starts...");
        Resource indicatorsResource = new ClassPathResource("sql/indicators.sql");
        Resource cpvResource = new ClassPathResource("sql/cpvCatalogue.sql");
        Resource natureMonopolyProcuringEntity = new ClassPathResource("sql/natureMonopolyProcuringEntity.sql");
        Resource queueRegions = new ClassPathResource("sql/queueRegion.sql");
        Resource queueConfig = new ClassPathResource("sql/queueConfig.sql");
        Resource configuration = new ClassPathResource("sql/configuration.sql");
        Resource indicatorBaseQuestion = new ClassPathResource("sql/indicator_base_question.sql");
        Resource indicatorEvaluations = new ClassPathResource("sql/indicator_evaluations.sql");
        Resource indicatorResponses = new ClassPathResource("sql/feedback_indicator_responses.sql");
        Resource getTenderAmountUah = new ClassPathResource("sql/get_tender_amount_uah.sql");
        Resource workDateCount = new ClassPathResource("sql/workDateCount.sql");
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(indicatorsResource, cpvResource, natureMonopolyProcuringEntity, queueRegions, queueConfig, configuration, indicatorBaseQuestion, indicatorEvaluations, indicatorResponses);
        populator.execute(dataSource);
        log.info("Database tables initialization completed successfully");
    }

    @Override
    public void afterPropertiesSet() {
        initIndicators();
    }
}
