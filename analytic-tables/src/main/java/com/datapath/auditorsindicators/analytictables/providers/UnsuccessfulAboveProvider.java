package com.datapath.auditorsindicators.analytictables.providers;

import com.datapath.persistence.entities.derivatives.UnsuccessfulAbove;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UnsuccessfulAboveProvider {

    private static final String QUERY = "with pec as (SELECT tv_procuring_entity, unnest(tv_tender_cpv_list) as cpv, date\n" +
            "             FROM tender\n" +
            "             WHERE procurement_method_type IN ('negotiation', 'negotiation.quick')\n" +
            "               AND status = 'complete'\n" +
            "               AND cause = 'twiceUnsuccessful'\n" +
            "               AND date > CURRENT_DATE - INTERVAL '1 year'\n" +
            "),\n" +
            "     uniq_pec as (select distinct on (pec.tv_procuring_entity, pec.cpv) pec.tv_procuring_entity pe,\n" +
            "                                                                        pec.cpv,\n" +
            "                                                                        pec.date\n" +
            "                  from pec\n" +
            "                  order by pec.tv_procuring_entity, pec.cpv, date desc\n" +
            "     ),\n" +
            "     tender_lots as (SELECT t.id tender_id, l.id lot_id\n" +
            "                     FROM tender t\n" +
            "                              join lot l on t.id = l.tender_id\n" +
            "                              left JOIN bid_lot bl ON l.id = bl.lot_id\n" +
            "                              left JOIN bid b ON bl.bid_id = b.id\n" +
            "                     WHERE t.procurement_method_type IN ('aboveThresholdUA', 'aboveThresholdEU')\n" +
            "                       AND t.date > CURRENT_DATE - INTERVAL '1 year'\n" +
            "                       and (l.status = 'unsuccessful' OR l.outer_id = 'autocreated')\n" +
            "                       AND (b.status = 'active' or b.id is null)\n" +
            "                     GROUP BY t.id, l.id\n" +
            "                     HAVING COUNT(b.id) < 2),\n" +
            "     tender_items as (\n" +
            "         select t.tender_id, i.classification_id cpv\n" +
            "         from tender_lots t\n" +
            "                  join tender_item i ON i.lot_id = t.lot_id\n" +
            "         group by t.tender_id, i.classification_id\n" +
            "     ),\n" +
            "     tender_data as (\n" +
            "         select ti.*, t.tv_procuring_entity pe, t.date\n" +
            "         from tender_items ti\n" +
            "                  join tender t on t.id = ti.tender_id\n" +
            "     )\n" +
            "select row_number() OVER () id,\n" +
            "       td.cpv,\n" +
            "       count(*) as lots_count,\n" +
            "       td.pe as procuring_entity\n" +
            "from tender_data td\n" +
            "         left join uniq_pec pec on pec.pe = td.pe and pec.cpv = td.cpv\n" +
            "where td.date BETWEEN COALESCE(pec.date, CURRENT_DATE - INTERVAL '1 year') AND CURRENT_DATE\n" +
            "group by td.pe, td.cpv;";

    private JdbcTemplate jdbcTemplate;

    public UnsuccessfulAboveProvider(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<UnsuccessfulAbove> provide() {
        return jdbcTemplate.query(QUERY, new BeanPropertyRowMapper<>(UnsuccessfulAbove.class));
    }

}
