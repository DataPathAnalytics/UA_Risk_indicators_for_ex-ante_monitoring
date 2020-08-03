package com.datapath.integration.services;

import com.datapath.integration.utils.ServiceStatus;

public interface ContractUpdatesManager {

    void loadLastModifiedContracts();

    void changeServiceStatus(ServiceStatus serviceStatus);

    ServiceStatus getServiceStatus();

}
