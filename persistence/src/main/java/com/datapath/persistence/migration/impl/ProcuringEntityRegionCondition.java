package com.datapath.persistence.migration.impl;

import com.datapath.persistence.migration.ApplicationCondition;
import com.datapath.persistence.migration.ConditionForMigration;
import com.datapath.persistence.repositories.ProcuringEntityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionForMigration(ProcuringEntityMigration.class)
public class ProcuringEntityRegionCondition implements ApplicationCondition {

    private ProcuringEntityRepository procuringEntityRepository;

    public ProcuringEntityRegionCondition(ProcuringEntityRepository procuringEntityRepository) {
        this.procuringEntityRepository = procuringEntityRepository;
    }

    @Override
    public boolean check() {
        log.info("Check {}", getClass().getSimpleName());
        Long nullableRegionsCount = procuringEntityRepository.countAllByRegionIsNull();
        Long procuringEntityCount = procuringEntityRepository.count();
        log.info("{} is {}", getClass().getSimpleName(), procuringEntityCount.equals(nullableRegionsCount));
        return nullableRegionsCount.equals(procuringEntityCount);
    }
}
