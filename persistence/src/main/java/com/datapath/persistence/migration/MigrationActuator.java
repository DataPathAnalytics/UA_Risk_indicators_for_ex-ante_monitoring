package com.datapath.persistence.migration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MigrationActuator implements InitializingBean {

    @Value("${com.datapath.scheduling.enabled}")
    private boolean schedulingEnabled;

    private MigrationRegistry migrationRegistry;

    MigrationActuator(MigrationRegistry migrationRegistry) {
        this.migrationRegistry = migrationRegistry;
    }

    private void startMigration() {
        int migrationsCount = migrationRegistry.getMigrations().size();
        log.info("Found {} registered migrations", migrationsCount);
        migrationRegistry.getMigrations().forEach((name, migration) -> {
            log.info("{} migration start", name);
            if (migration.checkApplicationCondition()) {
                migration.apply();
            }
            log.info("{} migration completed successfully", name);
        });
    }

    @Override
    public void afterPropertiesSet() {
        if (schedulingEnabled) {
            startMigration();
        }
    }
}
