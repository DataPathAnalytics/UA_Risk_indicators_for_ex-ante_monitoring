package com.datapath.auditorsindicators.analytictables.providers;

import com.datapath.persistence.entities.derivatives.UnsuccessfulAbove;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UnsuccessfulAboveProvider {

    private static final String QUERY = "WITH psd AS (\n" +
            "    SELECT\n" +
            "      tv_procuring_entity,\n" +
            "      unnest(tv_tender_cpv_list)                   tv_tender_cpv_item,\n" +
            "      CASE WHEN MAX(date) > CURRENT_DATE - INTERVAL '1 year' AND procurement_method_type IN ('negotiation', 'negotiation.quick')\n" +
            "      AND status = 'complete'\n" +
            "        THEN MAX(date)\n" +
            "      ELSE CURRENT_DATE - INTERVAL '1 year' END AS date\n" +
            "    FROM tender\n" +
            "    GROUP BY tv_procuring_entity, tv_tender_cpv_item, procurement_method_type, status\n" +
            ")" +
            "SELECT\n" +
            "  row_number()\n" +
            "  OVER () AS                   id,\n" +
            "  t.tv_procuring_entity        procuring_entity,\n" +
            "  unnest(t.tv_tender_cpv_list) tender_cpv,\n" +
            "  COUNT(distinct t.outer_id)                     unsuccessful_above_procedures_count\n" +
            "FROM tender t\n" +
            "  JOIN psd ON t.tv_procuring_entity = psd.tv_procuring_entity\n" +
            "              AND psd.tv_tender_cpv_item = ANY (tv_tender_cpv_list)\n" +
            "WHERE t.status = 'unsuccessful'\n" +
            "      AND t.procurement_method_type IN ('aboveThresholdUA', 'aboveThresholdEU')\n" +
            "      AND t.date BETWEEN psd.date AND CURRENT_DATE\n" +
            "GROUP BY t.tv_procuring_entity, tender_cpv";

    private JdbcTemplate jdbcTemplate;

    public UnsuccessfulAboveProvider(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<UnsuccessfulAbove> provide() {
        return jdbcTemplate.query(QUERY, new BeanPropertyRowMapper<>(UnsuccessfulAbove.class));
    }

}
