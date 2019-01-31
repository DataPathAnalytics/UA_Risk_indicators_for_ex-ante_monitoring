package com.datapath.auditorsindicators.analytictables.providers;

import com.datapath.persistence.entities.derivatives.SuppliersSingleBuyer;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SuppliersSingleBuyerProvider {

    private JdbcTemplate jdbcTemplate;

    public SuppliersSingleBuyerProvider(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String QUERY = "WITH entity_supplier AS (\n" +
            "         SELECT t.procuring_entity_id,\n" +
            "            c.supplier_id,\n" +
            "            count(c.id) AS contracts_count\n" +
            "           FROM tender t\n" +
            "             JOIN tender_contract c ON c.tender_id = t.id\n" +
            "          WHERE t.status = 'complete' AND c.supplier_id IS NOT NULL AND c.status = 'active'\n" +
            "          GROUP BY t.procuring_entity_id, c.supplier_id\n" +
            "         HAVING count(c.id) > 0\n" +
            "        ), constant_supplier AS (\n" +
            "         SELECT entity_supplier.supplier_id\n" +
            "           FROM entity_supplier\n" +
            "          GROUP BY entity_supplier.supplier_id\n" +
            "         HAVING count(entity_supplier.procuring_entity_id) = 1\n" +
            "        )\n" +
            " SELECT row_number() OVER () AS id,\n" +
            "    ( SELECT procuring_entity.identifier_scheme::TEXT || procuring_entity.identifier_id::TEXT\n" +
            "           FROM procuring_entity\n" +
            "          WHERE procuring_entity.id = es.procuring_entity_id) AS buyer_id,\n" +
            "    ( SELECT supplier.identifier_scheme::TEXT || supplier.identifier_id::TEXT\n" +
            "           FROM supplier\n" +
            "          WHERE supplier.id = es.supplier_id) AS supplier\n" +
            "   FROM entity_supplier es\n" +
            "     JOIN constant_supplier s ON es.supplier_id = s.supplier_id\n" +
            "  WHERE es.contracts_count > 3\n" +
            "  ORDER BY es.procuring_entity_id";

    public List<SuppliersSingleBuyer> provide() {
        return jdbcTemplate.query(QUERY, new BeanPropertyRowMapper<>(SuppliersSingleBuyer.class));
    }

}