package com.datapath.web.mappers;

import com.datapath.web.domain.KeyDimensionsItem;

public class KeyDimensionsItemMapper {

    public static KeyDimensionsItem mapToIndicatorInfo(String val) {
        KeyDimensionsItem item = new KeyDimensionsItem();
        item.setId(val);
        item.setName(val);
        item.setCode(val);
        item.setAvailable(true);
        return item;
    }
}
