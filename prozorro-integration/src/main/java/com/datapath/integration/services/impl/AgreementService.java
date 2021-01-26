package com.datapath.integration.services.impl;

import com.datapath.integration.utils.EntitySource;
import com.datapath.persistence.entities.Agreement;
import com.datapath.persistence.repositories.AgreementRepository;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

import static java.util.Objects.nonNull;

@Service
public class AgreementService {

    private final AgreementRepository repository;

    public AgreementService(AgreementRepository repository) {
        this.repository = repository;
    }

    public ZonedDateTime getLastDateModified() {
        Agreement entity = getLastDateModifiedAgreement();
        return nonNull(entity) && nonNull(entity.getDateModified()) ?
                entity.getDateModified().plusNanos(1000) :
                null;
    }

    public Agreement getLastDateModifiedAgreement() {
        return repository.findFirstBySourceAndDateModifiedIsNotNullOrderByDateModifiedDesc(EntitySource.AGREEMENT.toString());
    }

    public void save(Agreement agreement) {
        repository.save(agreement);
    }
}
