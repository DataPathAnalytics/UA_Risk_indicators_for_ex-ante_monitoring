package com.datapath.auditorsindicators.analytictables.providers;

import com.datapath.persistence.entities.derivatives.GeneralSpecial;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GeneralSpecialProvider {

    private static final String QUERY = "" +
            "SELECT\n" +
            "  row_number()\n" +
            "  OVER () AS id,\n" +
            "  concat('UA-EDR-', procuring_entity_id) procuring_entity," +
            "  procuring_entity_id\n" +
            "FROM (\n" +
            "       SELECT DISTINCT procuring_entity.identifier_id procuring_entity_id\n" +
            "       FROM tender\n" +
            "         JOIN procuring_entity ON tender.procuring_entity_id = procuring_entity.id\n" +
            "       WHERE procuring_entity_kind IN ('general','authority','central','social','special')\n" +
            "             AND procuring_entity.identifier_scheme = 'UA-EDR'\n" +
            "             AND tender.date > now() - INTERVAL '1 year'\n" +
            "     ) a";

    private JdbcTemplate jdbcTemplate;

    public GeneralSpecialProvider(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<GeneralSpecial> provide() {
        return jdbcTemplate.query(QUERY, new BeanPropertyRowMapper<>(GeneralSpecial.class));
    }

}
