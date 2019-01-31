package com.datapath.integration.services.impl;

import com.datapath.integration.services.CpvCatalogueService;
import com.datapath.persistence.entities.CpvCatalogue;
import com.datapath.persistence.repositories.CpvCatalogueRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CpvCatalogueServiceImpl implements CpvCatalogueService {

    private CpvCatalogueRepository repository;

    public CpvCatalogueServiceImpl(CpvCatalogueRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<CpvCatalogue> getCatalogue(List<String> cpvs) {
        return repository.findByCpvCodeIn(cpvs);
    }

    @Override
    public CpvCatalogue findByCpv(String cpv) {
        return repository.findByCpv(cpv);
    }
}
