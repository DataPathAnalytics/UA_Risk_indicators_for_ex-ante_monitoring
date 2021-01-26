package com.datapath.integration.services.impl;

import com.datapath.integration.services.EntityService;
import com.datapath.integration.utils.EntitySource;
import com.datapath.persistence.entities.Contract;
import com.datapath.persistence.repositories.ContractRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service("integrationContractService")
public class ContractService implements EntityService<Contract> {

    private ContractRepository repository;

    public ContractService(ContractRepository repository) {
        this.repository = repository;
    }

    @Override
    public Contract getById(Long id) {
        return repository.getOne(id);
    }

    @Override
    public Contract findById(Long id) {
        final Optional<Contract> optionalContract = repository.findById(id);
        return optionalContract.orElseGet(optionalContract::get);
    }

    @Override
    public List<Contract> findAll() {
        return repository.findAll();
    }

    @Override
    public Contract save(Contract entity) {
        return repository.save(entity);
    }

    @Override
    public List<Contract> save(List<Contract> entities) {
        return repository.saveAll(entities);
    }

    public Contract findLastModifiedEntry() {
        return repository.findFirstBySourceAndDateModifiedIsNotNullOrderByDateModifiedDesc(EntitySource.CONTRACTING.toString());
    }

    public Contract findByOuterId(String outerId) {
        return repository.findFirstByOuterId(outerId);
    }
}
