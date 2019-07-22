package com.datapath.elasticsearchintegration.converters;

import com.datapath.elasticsearchintegration.constants.TenderScoreRank;

import java.beans.PropertyEditorSupport;

/**
 * @author vitalii
 */
public class TenderScoreRankEnumConverter extends PropertyEditorSupport {
    @Override
    public void setAsText(String text) {
        TenderScoreRank riskedProcedure = TenderScoreRank.valueOf(text);
        setValue(riskedProcedure);
    }
}
