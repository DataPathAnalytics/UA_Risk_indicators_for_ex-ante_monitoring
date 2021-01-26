package com.datapath.integration.services;

import com.datapath.integration.utils.ServiceStatus;

public interface ContractUpdatesManager {

    void loadLastModifiedContracts();

    void removeExpiredContacts();

    void changeServiceStatus(ServiceStatus serviceStatus);

    ServiceStatus getServiceStatus();

    boolean isUpdatesAvailable();

    void setUpdatesAvailability(boolean availability);

}
