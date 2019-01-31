package com.datapath.persistence.migration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class MigrationRegistry {

    private final Map<String, Migration> registry;

    @Autowired
    public MigrationRegistry(List<? extends Migration> migrations) {
        registry = new HashMap<>();
        migrations.forEach(this::register);
    }

    public Map<String, Migration> getMigrations() {
        return registry;
    }

    private void register(Migration migration) {
        boolean enabledMigration = AnnotationUtils.isAnnotationDeclaredLocally(
                EnableMigration.class, migration.getClass());
        if (enabledMigration) {
            registry.put(migration.getName(), migration);
            log.info("{} migration registered", migration.getName());
        } else {
            log.info("{} migration disabled", migration.getName());
        }
    }
}
