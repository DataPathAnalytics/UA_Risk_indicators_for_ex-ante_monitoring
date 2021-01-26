package com.datapath.auditorsindicators.coordinator;

import com.datapath.persistence.migration.MigrationActuator;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class MigrationInitializer implements InitializingBean {

    private final MigrationActuator migrationActuator;

    @Override
    public void afterPropertiesSet() {
        migrationActuator.startMigration();
    }
}
