package com.datapath.elasticsearchintegration.converters;

import com.datapath.elasticsearchintegration.constants.RiskedProcedure;

import java.beans.PropertyEditorSupport;

/**
 * @author vitalii
 */
public class RiskedProcedureEnumConverter extends PropertyEditorSupport {
    @Override
    public void setAsText(String text) {
        RiskedProcedure riskedProcedure = RiskedProcedure.valueOf(text);
        setValue(riskedProcedure);
    }
}
