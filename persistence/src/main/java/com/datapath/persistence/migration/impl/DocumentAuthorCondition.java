package com.datapath.persistence.migration.impl;

import com.datapath.persistence.migration.ApplicationCondition;
import com.datapath.persistence.migration.ConditionForMigration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionForMigration(DocumentAuthorMigration.class)
public class DocumentAuthorCondition implements ApplicationCondition {

    @Override
    public boolean check() {
        log.info("Check {}", getClass().getSimpleName());
        log.info("{} is {}", getClass().getSimpleName(), false);
        return false;
    }
}
