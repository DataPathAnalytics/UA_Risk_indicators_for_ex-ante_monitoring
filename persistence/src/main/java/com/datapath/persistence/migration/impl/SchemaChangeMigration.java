package com.datapath.persistence.migration.impl;

import com.datapath.persistence.migration.ApplicationCondition;
import com.datapath.persistence.migration.EnableMigration;
import com.datapath.persistence.migration.Migration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@EnableMigration
public class SchemaChangeMigration extends Migration {

    private JdbcTemplate jdbcTemplate;

    public SchemaChangeMigration(List<? extends ApplicationCondition> conditions, JdbcTemplate jdbcTemplate) {
        super(conditions);
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void apply() {
        jdbcTemplate.execute("ALTER TABLE supplier ALTER COLUMN telephone TYPE TEXT");
        jdbcTemplate.execute("ALTER TABLE bid ALTER COLUMN supplier_telephone TYPE TEXT");
        jdbcTemplate.execute("ALTER TABLE award ALTER COLUMN supplier_telephone TYPE TEXT");
        jdbcTemplate.execute("ALTER TABLE tender_contract ALTER COLUMN supplier_telephone TYPE TEXT");
    }
}
