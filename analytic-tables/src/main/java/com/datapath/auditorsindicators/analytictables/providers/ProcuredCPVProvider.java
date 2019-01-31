package com.datapath.auditorsindicators.analytictables.providers;

import com.datapath.persistence.entities.derivatives.ProcuredCPV;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProcuredCPVProvider {

    private static final String QUERY = "" +
            "SELECT  row_number() OVER () AS id, cpv FROM (\n" +
            "  SELECT  DISTINCT unnest(tv_tender_cpv_list) cpv\n" +
            "  FROM tender\n" +
            "  WHERE status = 'complete'\n" +
            "        AND procurement_method_type IN ('aboveThresholdUA', 'aboveThresholdEU')\n" +
            "        AND date > now() - INTERVAL '1 year'\n" +
            ")a";

    private JdbcTemplate jdbcTemplate;

    public ProcuredCPVProvider(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ProcuredCPV> provide() {
        return jdbcTemplate.query(QUERY, new BeanPropertyRowMapper<>(ProcuredCPV.class));
    }

}
