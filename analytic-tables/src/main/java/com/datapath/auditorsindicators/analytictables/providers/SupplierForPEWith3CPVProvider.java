package com.datapath.auditorsindicators.analytictables.providers;

import com.datapath.persistence.entities.derivatives.SupplierForPEWith3CPV;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SupplierForPEWith3CPVProvider {

    private static final String QUERY = "" +
            "select " +
            "row_number() OVER () AS id, " +
            "tv_procuring_entity procuring_entity, " +
            "supplier, " +
            "count(cpv) cpv_count FROM (\n" +
            "  SELECT\n" +
            "    tv_procuring_entity,\n" +
            "    supplier,\n" +
            "    substr(cpv, 0, 5) cpv\n" +
            "  FROM (\n" +
            "         SELECT\n" +
            "           tv_procuring_entity,\n" +
            "           supplier,\n" +
            "           unnest(cpvs) cpv\n" +
            "         FROM (\n" +
            "                SELECT\n" +
            "                  tender.tv_procuring_entity,\n" +
            "                  CASE WHEN tender_contract.status = 'active'\n" +
            "                    THEN tender_contract.contract_cpv_list END   cpvs,\n" +
            "                  concat(tender_contract.supplier_identifier_scheme, '-',\n" +
            "                         tender_contract.supplier_identifier_id) supplier\n" +
            "                FROM tender\n" +
            "                  JOIN tender_contract ON tender.id = tender_contract.tender_id\n" +
            "                WHERE tender.status = 'complete'\n" +
            "              ) a\n" +
            "       ) b\n" +
            ")c GROUP BY tv_procuring_entity, supplier";

    private JdbcTemplate jdbcTemplate;

    public SupplierForPEWith3CPVProvider(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SupplierForPEWith3CPV> provide() {
        return jdbcTemplate.query(QUERY, new BeanPropertyRowMapper<>(SupplierForPEWith3CPV.class));
    }

}
