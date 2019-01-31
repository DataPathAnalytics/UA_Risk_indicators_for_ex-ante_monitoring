package com.datapath.persistence.migration.impl;

import com.datapath.persistence.migration.ApplicationCondition;
import com.datapath.persistence.migration.ConditionForMigration;
import com.datapath.persistence.repositories.QualificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionForMigration(QualificationMigration.class)
public class QualificationCondition implements ApplicationCondition {

    private QualificationRepository qualificationRepository;

    public QualificationCondition(QualificationRepository qualificationRepository) {
        this.qualificationRepository = qualificationRepository;
    }

    @Override
    public boolean check() {
        log.info("Check {}", getClass().getSimpleName());
        Long numberOfQualifications = qualificationRepository.count();
        System.out.println(numberOfQualifications+ " - "+numberOfQualifications.equals(0L));
        return numberOfQualifications.equals(0L);
    }
}
