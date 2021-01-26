package com.datapath.auditorsindicators.analytictables.providers;

import com.datapath.persistence.entities.derivatives.NoNeed;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NoNeedProvider {

    private static final String QUERY = "with tenders as (select t.id, t.tv_procuring_entity, t.tv_tender_cpv_list, c.cancellation_of, c.lot_id\n" +
            "                 from tender t\n" +
            "                          join cancellation c on t.id = c.tender_id\n" +
            "                 where true\n" +
            "                   and extract(years from now()) = substr(t.tender_id, 4, 4)::int\n" +
            "                   and t.procurement_method_type in ('aboveThresholdEU', 'aboveThresholdUA')\n" +
            "                   and c.status = 'active'\n" +
            "                   and c.reason similar to '%подальш%відсутн%|%відсутн%потреб%'),\n" +
            "     items_data as (select t.tv_procuring_entity        entity,\n" +
            "                           unnest(t.tv_tender_cpv_list) cpv\n" +
            "                    from tenders t\n" +
            "                    where cancellation_of = 'tender'\n" +
            "     ),\n" +
            "     lot_data as (\n" +
            "         select t.tv_procuring_entity entity, i.classification_id cpv\n" +
            "         from tenders t\n" +
            "                  join tender_item i on t.id = i.tender_id\n" +
            "         where t.cancellation_of = 'lot'\n" +
            "           and i.lot_id = t.lot_id\n" +
            "     ),\n" +
            "     union_data as (\n" +
            "         select *\n" +
            "         from items_data\n" +
            "         union\n" +
            "         select *\n" +
            "         from lot_data\n" +
            "     )\n" +
            "select row_number() over () as id, entity as procuring_entity, cpv\n" +
            "from union_data\n" +
            "group by entity, cpv;";

    private JdbcTemplate jdbcTemplate;

    public NoNeedProvider(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<NoNeed> provide() {
        return jdbcTemplate.query(QUERY, new BeanPropertyRowMapper<>(NoNeed.class));
    }

}
