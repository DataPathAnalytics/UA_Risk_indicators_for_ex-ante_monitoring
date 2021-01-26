package com.datapath.auditorsindicators.analytictables.providers;

import com.datapath.persistence.entities.derivatives.WinsCount;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WinsCountProvider {

    private static final String QUERY = "select row_number() over ()                                        as id,\n" +
            "       t.tv_procuring_entity                                       as procuring_entity,\n" +
            "       concat(supplier_identifier_scheme, supplier_identifier_id)  as supplier,\n" +
            "       string_agg(distinct classification_id, ',') as cpv_list\n" +
            "from tender t\n" +
            "         join tender_contract tc on t.id = tc.tender_id\n" +
            "         join tender_item i on t.id = i.tender_id\n" +
            "where procurement_method_type in ('aboveThresholdUA', 'aboveThresholdEU')\n" +
            "  and extract(years from now()) = extract(years from tc.date_signed)\n" +
            "group by procuring_entity, supplier;";

    private JdbcTemplate jdbcTemplate;

    public WinsCountProvider(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<WinsCount> provide() {
        return jdbcTemplate.query(QUERY, new BeanPropertyRowMapper<>(WinsCount.class));
    }
}
