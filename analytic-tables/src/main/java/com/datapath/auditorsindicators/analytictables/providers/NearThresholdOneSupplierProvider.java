package com.datapath.auditorsindicators.analytictables.providers;

import com.datapath.persistence.entities.derivatives.NearThresholdOneSupplier;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NearThresholdOneSupplierProvider {

    private static final String QUERY = "" +
            "SELECT\n" +
            "  row_number()\n" +
            "  OVER () AS id,\n" +
            "  procuring_entity,\n" +
            "  supplier\n" +
            "FROM (\n" +
            "       SELECT\n" +
            "         procuring_entity,\n" +
            "         supplier\n" +
            "       FROM (\n" +
            "              SELECT\n" +
            "                tender.outer_id,\n" +
            "                substr(tender.tender_id, 4, 10)                                             date,\n" +
            "                tender.procuring_entity_kind,\n" +
            "                tender.tv_subject_of_procurement,\n" +
            "                tender.tv_procuring_entity                                                  procuring_entity,\n" +
            "                concat(award.supplier_identifier_scheme, '-', award.supplier_identifier_id) supplier,\n" +
            "                CASE WHEN tender.currency = 'UAH'\n" +
            "                  THEN tender.amount\n" +
            "                ELSE\n" +
            "                  tender.amount *\n" +
            "                  (SELECT rate\n" +
            "                   FROM exchange_rate\n" +
            "                   WHERE tender.currency = exchange_rate.code AND\n" +
            "                         concat(substr(tender.tender_id, 4, 10), ' 00:00:00.000000') :: DATE = exchange_rate.date)\n" +
            "                END                                                                         amount\n" +
            "              FROM tender\n" +
            "                JOIN award ON tender.id = award.tender_id\n" +
            "              WHERE tender.status = 'complete' AND\n" +
            "             (procurement_method_type = 'belowThreshold' OR\n" +
            "              (procurement_method_type = 'reporting' AND DATE_PART('day', current_date-tender.date) > 3)) " +
            "                    AND substr(tender.tender_id, 4, 4) = to_char(CURRENT_DATE, 'YYYY')\n" +
            "                    AND award.status = 'active') a\n" +
            "       WHERE (procuring_entity_kind IN ('general','authority','central','social') AND tv_subject_of_procurement LIKE '45%' AND amount > 1350000 AND amount < 1500000)\n" +
            "             OR (procuring_entity_kind IN ('general','authority','central','social') AND tv_subject_of_procurement NOT LIKE '45%' AND amount > 190000 AND amount < 200000)\n" +
            "             OR (procuring_entity_kind = 'special' AND tv_subject_of_procurement LIKE '45%' AND amount > 4500000 AND amount < 5000000)\n" +
            "             OR (procuring_entity_kind = 'special' AND tv_subject_of_procurement NOT LIKE '45%' AND amount > 950000 AND amount < 1000000)\n" +
            "       GROUP BY procuring_entity, supplier\n" +
            "     ) b";

    private JdbcTemplate jdbcTemplate;

    public NearThresholdOneSupplierProvider(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<NearThresholdOneSupplier> provide() {
        return jdbcTemplate.query(QUERY, new BeanPropertyRowMapper<>(NearThresholdOneSupplier.class));
    }

}
