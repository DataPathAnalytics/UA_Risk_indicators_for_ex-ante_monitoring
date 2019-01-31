package com.datapath.indicatorsqueue.mappers;

import com.datapath.druidintegration.model.TenderScore;
import com.datapath.persistence.entities.queue.IndicatorsQueueItem;
import com.datapath.persistence.entities.queue.RegionIndicatorsQueueItem;
import org.apache.commons.math3.util.Precision;

public class TenderScoreMapper {

    public static IndicatorsQueueItem mapToIndicatorsQueueItem(TenderScore tenderScore) {
        IndicatorsQueueItem item = new IndicatorsQueueItem();
        item.setTenderOuterId(tenderScore.getOuterId());
        item.setTenderId(tenderScore.getTenderId());
        item.setExpectedValue(tenderScore.getExpectedValue());
        item.setMaterialityScore(Precision.round(tenderScore.getScore(), 2));
        item.setTenderScore(Precision.round(tenderScore.getImpact(), 2));
        item.setProcuringEntityId(tenderScore.getProcuringEntityId());
        item.setRegion(tenderScore.getRegion());
        return item;
    }

    public static RegionIndicatorsQueueItem mapToRegionIndicatorsQueueItem(TenderScore tenderScore) {
        RegionIndicatorsQueueItem item = new RegionIndicatorsQueueItem();
        item.setTenderOuterId(tenderScore.getOuterId());
        item.setTenderId(tenderScore.getTenderId());
        item.setExpectedValue(tenderScore.getExpectedValue());
        item.setMaterialityScore(Precision.round(tenderScore.getScore(), 2));
        item.setTenderScore(Precision.round(tenderScore.getImpact(), 2));
        item.setProcuringEntityId(tenderScore.getProcuringEntityId());
        item.setRegion(tenderScore.getRegion());
        return item;
    }
}
