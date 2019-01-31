package com.datapath.auditorsindicators.analytictables.providers;

import com.datapath.persistence.entities.derivatives.ItemsAbnormalQuantity;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ItemsAbnormalQuantityProvider {

    private static final String QUERY =
            " WITH tender_cpv AS (\n" +
                    "         SELECT \"substring\"(i.classification_id::text, 0, 3) AS cpv,\n" +
                    "            count(i.id) AS count\n" +
                    "           FROM tender_item i\n" +
                    "             JOIN tender t ON i.tender_id = t.id\n" +
                    "          WHERE t.status::text = 'complete'::text\n" +
                    "          GROUP BY i.tender_id, (\"substring\"(i.classification_id::text, 0, 3))\n" +
                    "        )\n" +
                    " SELECT row_number() OVER () AS id,\n" +
                    "    tender_cpv.cpv,\n" +
                    "    percentile_cont(0.998::double precision) WITHIN GROUP (ORDER BY (tender_cpv.count::double precision)) AS percentile_count\n" +
                    "   FROM tender_cpv\n" +
                    "  GROUP BY tender_cpv.cpv";

    private JdbcTemplate jdbcTemplate;

    public ItemsAbnormalQuantityProvider(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ItemsAbnormalQuantity> provide() {
        return jdbcTemplate.query(QUERY, new BeanPropertyRowMapper<>(ItemsAbnormalQuantity.class));
    }

}