package com.datapath.persistence.migration.impl;

import com.datapath.persistence.migration.ApplicationCondition;
import com.datapath.persistence.migration.ConditionForMigration;
import org.springframework.stereotype.Component;

@Component
@ConditionForMigration(SchemaChangeMigration.class)
public class TrueCondition implements ApplicationCondition {
    @Override
    public boolean check() {
        return true;
    }
}
