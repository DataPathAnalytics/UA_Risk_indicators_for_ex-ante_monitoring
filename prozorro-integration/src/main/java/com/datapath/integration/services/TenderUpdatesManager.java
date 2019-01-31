package com.datapath.integration.services;

import com.datapath.integration.domain.TenderUpdateInfo;
import com.datapath.integration.utils.ServiceStatus;

import java.util.List;

public interface TenderUpdatesManager {

    void loadLastModifiedTenders();

    void saveTendersFromUpdateInfo(List<TenderUpdateInfo> tenderUpdateInfos);

    void removeExpiredTenders();

    void changeServiceStatus(ServiceStatus serviceStatus);

    ServiceStatus getServiceStatus();

    boolean isUpdatesAvailable();

    void setUpdatesAvailability(boolean availability);

}
