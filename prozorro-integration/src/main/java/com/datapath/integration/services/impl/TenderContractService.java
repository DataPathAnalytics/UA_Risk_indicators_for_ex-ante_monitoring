package com.datapath.integration.services.impl;

import com.datapath.integration.services.EntityService;
import com.datapath.persistence.entities.TenderContract;
import com.datapath.persistence.repositories.TenderContractRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TenderContractService implements EntityService<TenderContract> {

    private TenderContractRepository repository;

    public TenderContractService(TenderContractRepository repository) {
        this.repository = repository;
    }

    @Override
    public TenderContract getById(Long id) {
        return repository.getOne(id);
    }

    @Override
    public TenderContract findById(Long id) {
        final Optional<TenderContract> optionalTenderContract = repository.findById(id);
        return optionalTenderContract.orElseGet(optionalTenderContract::get);
    }

    @Override
    public List<TenderContract> findAll() {
        return repository.findAll();
    }

    @Override
    public TenderContract save(TenderContract entity) {
        return repository.save(entity);
    }

    @Override
    public List<TenderContract> save(List<TenderContract> entities) {
        return repository.saveAll(entities);
    }

    public TenderContract findByOuterId(String outerId) {
        return repository.findFirstByOuterId(outerId);
    }
}
