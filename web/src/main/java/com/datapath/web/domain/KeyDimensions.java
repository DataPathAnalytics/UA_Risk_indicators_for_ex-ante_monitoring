package com.datapath.web.domain;

import lombok.Data;

import java.util.List;

@Data
public class KeyDimensions {

    private List<KeyDimensionsItem> procedureTypes;
    private List<KeyDimensionsItem> indicators;

}
