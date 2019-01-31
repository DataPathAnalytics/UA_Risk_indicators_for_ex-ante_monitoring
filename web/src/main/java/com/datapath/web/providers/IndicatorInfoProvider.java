package com.datapath.web.providers;

import com.datapath.web.domain.IndicatorInfo;

public interface IndicatorInfoProvider {

    IndicatorInfo getIndicatorById(String indicatorId);

    IndicatorInfo getIndicatorByCode(String indicatorCode);

}
