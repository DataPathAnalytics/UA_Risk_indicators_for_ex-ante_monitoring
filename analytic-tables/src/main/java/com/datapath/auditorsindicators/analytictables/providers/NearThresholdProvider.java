package com.datapath.auditorsindicators.analytictables.providers;

import com.datapath.persistence.entities.derivatives.NearThreshold;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NearThresholdProvider {

    private static final String QUERY = "" +
            "SELECT row_number() OVER () AS id, " +
            "tv_procuring_entity procuring_entity, " +
            "tv_subject_of_procurement cpv, " +
            "amount\n" +
            "FROM (SELECT DISTINCT tv_procuring_entity, tv_subject_of_procurement, sum(amount) amount\n" +
            "      FROM (SELECT tender_id,\n" +
            "                   tv_procuring_entity,\n" +
            "                   procuring_entity_kind,\n" +
            "                   tv_subject_of_procurement,\n" +
            "                   substr(tender_id, 4, 10),\n" +
            "                   CASE\n" +
            "                     WHEN currency = 'UAH'\n" +
            "                             THEN amount\n" +
            "                     ELSE amount *\n" +
            "                          (SELECT rate\n" +
            "                           FROM exchange_rate\n" +
            "                           WHERE tender.currency = exchange_rate.code\n" +
            "                             AND concat(substr(tender_id, 4, 10), ' 00:00:00.000000') :: DATE =\n" +
            "                                 exchange_rate.date)\n" +
            "                       END amount\n" +
            "            FROM tender\n" +
            "            WHERE substr(tender_id, 4, 4) = date_part('year', current_date) :: TEXT\n" +
            "              AND tender.status = 'complete'\n" +
            "              AND (procurement_method_type = 'belowThreshold' OR\n" +
            "                   (procurement_method_type = 'reporting' AND\n" +
            "                    DATE_PART('day', current_date - tender.date) > 3))) a\n" +
            "      GROUP BY tv_procuring_entity, tv_subject_of_procurement) b";

    private JdbcTemplate jdbcTemplate;

    public NearThresholdProvider(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<NearThreshold> provide() {
        return jdbcTemplate.query(QUERY, new BeanPropertyRowMapper<>(NearThreshold.class));
    }

}
