package com.datapath.web.util;

import com.datapath.web.domain.DruidIndicator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MapUtil {

    public static Map<String, List<DruidIndicator>> sortIndicatorsByDate(Map<String, List<DruidIndicator>> map) {
        List<Map.Entry<String, List<DruidIndicator>>> list = new ArrayList<>(map.entrySet());
        list.sort((o1, o2) -> {
            o1.getValue().sort((o11, o21) -> o11.getDate().isBefore(o21.getDate()) ? 1 : -1);
            o2.getValue().sort((o11, o21) -> o11.getDate().isBefore(o21.getDate()) ? 1 : -1);
            return o1.getValue().get(0).getDate().isAfter(o2.getValue().get(0).getDate()) ? 1 : -1;
        });

        Map<String, List<DruidIndicator>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<DruidIndicator>> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }
}
