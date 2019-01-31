package com.datapath.persistence.migration.impl;

import com.datapath.persistence.migration.ApplicationCondition;
import com.datapath.persistence.migration.EnableMigration;
import com.datapath.persistence.migration.Migration;
import com.datapath.persistence.repositories.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@EnableMigration
public class TenderDocumentMigration extends Migration {

    private DocumentRepository documentRepository;

    public TenderDocumentMigration(List<? extends ApplicationCondition> conditions,
                                   DocumentRepository documentRepository) {
        super(conditions);
        this.documentRepository = documentRepository;
    }

    @Override
    public void apply() {
        log.info("Applying {} migration", this.getClass().getSimpleName());
        long maxId = documentRepository.getMaxId();
        long minId = documentRepository.getMinId();
        long step = 50000;

        log.info("Document maxId = {}, minId = {}", maxId, minId);

        Long currentId = minId;
        while (currentId <= maxId) {
            documentRepository.removeTenderIdFromDocument(currentId, currentId + step);
            currentId += step;
            log.info("Updated {} documents", currentId);
        }
        log.info("Document updated", this.getClass().getSimpleName());
    }
}
