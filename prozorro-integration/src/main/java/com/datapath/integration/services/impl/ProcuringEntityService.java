package com.datapath.integration.services.impl;

import com.datapath.integration.services.EntityService;
import com.datapath.persistence.entities.ProcuringEntity;
import com.datapath.persistence.repositories.ProcuringEntityRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProcuringEntityService implements EntityService<ProcuringEntity> {

    private ProcuringEntityRepository repository;

    public ProcuringEntityService(ProcuringEntityRepository repository) {
        this.repository = repository;
    }

    @Override
    public ProcuringEntity getById(Long id) {
        return repository.getOne(id);
    }

    @Override
    public ProcuringEntity findById(Long id) {
        final Optional<ProcuringEntity> optionalProcuringEntity = repository.findById(id);
        return optionalProcuringEntity.orElseGet(optionalProcuringEntity::get);
    }

    @Override
    public List<ProcuringEntity> findAll() {
        return repository.findAll();
    }

    @Override
    public ProcuringEntity save(ProcuringEntity entity) {
        return repository.save(entity);
    }

    @Override
    public List<ProcuringEntity> save(List<ProcuringEntity> entities) {
        return repository.saveAll(entities);
    }

    public ProcuringEntity findByIdentifierIdAndScheme(String identifierId, String identifierScheme) {
        return repository.findByIdentifierIdAndIdentifierScheme(identifierId, identifierScheme);
    }
}
