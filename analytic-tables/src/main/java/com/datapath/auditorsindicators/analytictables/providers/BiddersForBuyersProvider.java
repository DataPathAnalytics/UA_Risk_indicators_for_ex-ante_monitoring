package com.datapath.auditorsindicators.analytictables.providers;

import com.datapath.persistence.entities.derivatives.BiddersForBuyers;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BiddersForBuyersProvider {

    private static final String QUERY = "" +
            "SELECT row_number()\n" +
            "       OVER () AS  id, " +
            " a.*\n" +
            "FROM (\n" +
            "       SELECT\n" +
            "         tender.tv_procuring_entity,\n" +
            "         concat(bid.supplier_identifier_scheme, '-', bid.supplier_identifier_id) supplier\n" +
            "       FROM tender\n" +
            "         JOIN bid ON tender.id = bid.tender_id) a\n" +
            "GROUP BY tv_procuring_entity, supplier";

    private JdbcTemplate jdbcTemplate;

    public BiddersForBuyersProvider(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<BiddersForBuyers> provide() {
        return jdbcTemplate.query(QUERY, new BeanPropertyRowMapper<>(BiddersForBuyers.class));
    }

}
