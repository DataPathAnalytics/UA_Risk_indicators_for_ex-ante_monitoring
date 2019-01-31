package com.datapath.persistence.repositories.derivatives;

import com.datapath.persistence.entities.derivatives.SupplierLot;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AnalyticTableRepository {

    private JdbcTemplate jdbcTemplate;

    public AnalyticTableRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SupplierLot> getSupplierLots() {
        String query = "SELECT\n" +
                "  s.identifier_scheme || s.identifier_id supplier,\n" +
                "  bl.lot_id\n" +
                "FROM tender t\n" +
                "  JOIN bid b ON b.tender_id = t.id\n" +
                "  JOIN supplier s ON b.supplier_id = s.id\n" +
                "  JOIN bid_lot bl ON bl.bid_id = b.id\n" +
                "WHERE t.status = 'complete' AND b.supplier_id IS NOT NULL\n" +
                "      AND b.status = 'active'\n" +
                "      AND t.date >= CURRENT_DATE - INTERVAL '1 year'";

        return jdbcTemplate.query(query, new BeanPropertyRowMapper<>(SupplierLot.class));
    }

}