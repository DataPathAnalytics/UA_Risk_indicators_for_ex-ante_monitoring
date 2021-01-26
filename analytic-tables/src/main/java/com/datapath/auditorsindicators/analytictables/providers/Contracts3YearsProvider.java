package com.datapath.auditorsindicators.analytictables.providers;

import com.datapath.persistence.entities.derivatives.Contracts3Years;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Contracts3YearsProvider {

    private static final String QUERY = "with data as (select t.tv_procuring_entity,\n" +
            "                     tc.supplier_identifier_scheme,\n" +
            "                     tc.supplier_identifier_id,\n" +
            "                     i.classification_id,\n" +
            "                     t.amount,\n" +
            "                     c.date_signed\n" +
            "              from tender t\n" +
            "                       join tender_contract tc on t.id = tc.tender_id\n" +
            "                       join contract c on c.tender_contract_id = tc.id\n" +
            "                       join tender_item i on t.id = i.tender_id\n" +
            "              where procurement_method_type in ('aboveThresholdUA', 'aboveThresholdEU')\n" +
            "                and extract(years from now()) - extract(years from c.date_signed) <= 3\n" +
            "                and (c.status = 'active' or (c.status = 'terminated' and c.termination_details is null))\n" +
            ")\n" +
            "select distinct on (\n" +
            "    tv_procuring_entity,\n" +
            "    supplier_identifier_id,\n" +
            "    classification_id\n" +
            "    ) row_number() over ()                                       as id,\n" +
            "      tv_procuring_entity                                        as procuring_entity,\n" +
            "      concat(supplier_identifier_scheme, supplier_identifier_id) as supplier,\n" +
            "      classification_id                                          as cpv,\n" +
            "      amount\n" +
            "from data\n" +
            "order by tv_procuring_entity, supplier_identifier_id, classification_id, date_signed;";

    private JdbcTemplate jdbcTemplate;

    public Contracts3YearsProvider(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Contracts3Years> provide() {
        return jdbcTemplate.query(QUERY, new BeanPropertyRowMapper<>(Contracts3Years.class));
    }
}
