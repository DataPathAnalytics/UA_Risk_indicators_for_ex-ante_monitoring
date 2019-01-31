package com.datapath.persistence.migration.impl;

import com.datapath.persistence.migration.ApplicationCondition;
import com.datapath.persistence.migration.ConditionForMigration;
import com.datapath.persistence.repositories.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionForMigration(TenderDocumentMigration.class)
public class TenderDocumentCondition implements ApplicationCondition {

    private DocumentRepository documentRepository;

    public TenderDocumentCondition(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Override
    public boolean check() {
        log.info("Check {}", getClass().getSimpleName());
        Long nullableBidsCount = documentRepository.countAllByTenderIdAndAwardIdIsNotNull();
        log.info("{} is {}", getClass().getSimpleName(), nullableBidsCount > 0);
        return nullableBidsCount > 0;
    }
}
