package com.datapath.persistence.migration.impl;

import com.datapath.persistence.migration.ApplicationCondition;
import com.datapath.persistence.migration.EnableMigration;
import com.datapath.persistence.migration.Migration;
import com.datapath.persistence.repositories.QualificationRepository;
import com.datapath.persistence.repositories.TenderDataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@EnableMigration
public class QualificationMigration extends Migration {

    private TenderDataRepository tenderDataRepository;
    private QualificationRepository qualificationRepository;

    public QualificationMigration(List<? extends ApplicationCondition> conditions,
                                  TenderDataRepository tenderDataRepository,
                                  QualificationRepository qualificationRepository) {
        super(conditions);
        this.tenderDataRepository = tenderDataRepository;
        this.qualificationRepository = qualificationRepository;
    }

    @Override
    public void apply() {
        log.info("Applying {} migration", this.getClass().getSimpleName());
        long maxId = tenderDataRepository.getMaxId();
        long minId = tenderDataRepository.getMinId();
        long step = 10000;

        log.info("Tender maxTenderDaraId = {}, minTenderDataId = {}", maxId, minId);

        Long currentId = minId;
        while (currentId <= maxId) {
            qualificationRepository.insertQualification(currentId, currentId + step);
            currentId += step;
            log.info("Updated {} tenderData", currentId);
        }
        log.info("Qualifications updated", this.getClass().getSimpleName());
    }
}
