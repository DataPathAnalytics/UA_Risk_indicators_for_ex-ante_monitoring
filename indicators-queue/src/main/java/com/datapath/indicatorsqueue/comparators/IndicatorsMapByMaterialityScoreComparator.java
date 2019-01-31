package com.datapath.indicatorsqueue.comparators;

import com.datapath.persistence.entities.queue.IndicatorsQueueItem;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class IndicatorsMapByMaterialityScoreComparator implements Comparator<String> {

    private Map<String, List<IndicatorsQueueItem>> base;

    public IndicatorsMapByMaterialityScoreComparator(Map<String, List<IndicatorsQueueItem>> base) {
        this.base = base;
    }

    @Override
    public int compare(String o1, String o2) {
        Double sum1 = base.get(o1).stream().mapToDouble(IndicatorsQueueItem::getMaterialityScore).sum();
        Double sum2 = base.get(o2).stream().mapToDouble(IndicatorsQueueItem::getMaterialityScore).sum();
        if (sum1 >= sum2) {
            return -1;
        } else {
            return 1;
        }
    }
}
