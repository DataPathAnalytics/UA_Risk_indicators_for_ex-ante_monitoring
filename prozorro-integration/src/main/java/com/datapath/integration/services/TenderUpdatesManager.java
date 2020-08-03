package com.datapath.integration.services;

import com.datapath.integration.domain.TenderUpdateInfo;

import java.util.List;

public interface TenderUpdatesManager {

    void loadLastModifiedTenders();

    void saveTendersFromUpdateInfo(List<TenderUpdateInfo> tenderUpdateInfos);

}
