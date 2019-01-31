package com.datapath.persistence.migration.impl;

import com.datapath.persistence.migration.ApplicationCondition;
import com.datapath.persistence.migration.EnableMigration;
import com.datapath.persistence.migration.Migration;
import com.datapath.persistence.repositories.ProcuringEntityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@EnableMigration
public class ProcuringEntityMigration extends Migration {

    private ProcuringEntityRepository procuringEntityRepository;

    public ProcuringEntityMigration(List<? extends ApplicationCondition> conditions,
                                    ProcuringEntityRepository procuringEntityRepository) {
        super(conditions);
        this.procuringEntityRepository = procuringEntityRepository;
    }

    @Override
    public void apply() {
        log.info("Applying {} migration", this.getClass().getSimpleName());
        long maxId = procuringEntityRepository.getMaxId();
        long minId = procuringEntityRepository.getMinId();
        long step = 5000;

        log.info("ProcuringEntity maxId = {}, minId = {}", maxId, minId);

        Long currentId = minId;
        while (currentId <= maxId) {
            procuringEntityRepository.updateRegionFromTenderData(currentId, currentId + step);
            currentId += step;
            log.info("Updated {} procuring entities", currentId);
        }
        log.info("Procuring entities regions updated", this.getClass().getSimpleName());
    }
}
