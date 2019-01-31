package com.datapath.auditorsindicators.analytictables.providers;

import com.datapath.persistence.entities.derivatives.EDRPOU;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EDRPOUProvider {

    private static final String QUERY =
            "SELECT\n" +
                    "    row_number()\n" +
                    "    OVER () AS id,\n" +
                    "    *\n" +
                    "  FROM (\n" +
                    "         SELECT\n" +
                    "           count(DISTINCT supplier_identifier_legal_name) count,\n" +
                    "           supplier\n" +
                    "         FROM (\n" +
                    "                SELECT\n" +
                    "                  concat(supplier_identifier_scheme, '-', supplier_identifier_id) supplier,\n" +
                    "                  supplier_identifier_legal_name\n" +
                    "                FROM bid\n" +
                    "                UNION\n" +
                    "                SELECT\n" +
                    "                  concat(supplier_identifier_scheme, '-', supplier_identifier_id) supplier,\n" +
                    "                  supplier_identifier_legal_name\n" +
                    "                FROM award\n" +
                    "              ) a\n" +
                    "         GROUP BY supplier\n" +
                    "       ) b\n" +
                    "  WHERE count > 1";

    private JdbcTemplate jdbcTemplate;

    public EDRPOUProvider(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<EDRPOU> provide() {
        return jdbcTemplate.query(QUERY, new BeanPropertyRowMapper<>(EDRPOU.class));
    }

}
