package com.datapath.integration.services;

import com.datapath.persistence.entities.CpvCatalogue;

import java.util.List;

public interface CpvCatalogueService {

    List<CpvCatalogue> getCatalogue(List<String> cpv);

    CpvCatalogue findByCpv(String cpv);

}