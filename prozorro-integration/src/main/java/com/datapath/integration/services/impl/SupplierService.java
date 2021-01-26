package com.datapath.integration.services.impl;

import com.datapath.integration.services.EntityService;
import com.datapath.persistence.entities.AgreementSupplier;
import com.datapath.persistence.entities.Supplier;
import com.datapath.persistence.repositories.AgreementSupplierRepository;
import com.datapath.persistence.repositories.SupplierRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SupplierService implements EntityService<Supplier> {

    private SupplierRepository repository;
    private AgreementSupplierRepository agreementRepository;

    public SupplierService(SupplierRepository repository,
                           AgreementSupplierRepository agreementRepository) {
        this.repository = repository;
        this.agreementRepository = agreementRepository;
    }

    @Override
    public Supplier getById(Long id) {
        return repository.getOne(id);
    }

    @Override
    public Supplier findById(Long id) {
        final Optional<Supplier> optionalSupplier = repository.findById(id);
        return optionalSupplier.orElseGet(optionalSupplier::get);
    }

    @Override
    public List<Supplier> findAll() {
        return repository.findAll();
    }

    @Override
    public Supplier save(Supplier entity) {
        return repository.save(entity);
    }

    @Override
    public List<Supplier> save(List<Supplier> entities) {
        return repository.saveAll(entities);
    }

    public Supplier findByIdentifierIdAndScheme(String identifierId, String identifierScheme) {
        return repository.findFirstByIdentifierIdAndIdentifierScheme(identifierId, identifierScheme);
    }

    public AgreementSupplier findAgreementSupplierByIdentifierIdAndScheme(String identifierId, String identifierScheme) {
        return agreementRepository.findFirstByIdentifierIdAndIdentifierScheme(identifierId, identifierScheme);
    }
}
