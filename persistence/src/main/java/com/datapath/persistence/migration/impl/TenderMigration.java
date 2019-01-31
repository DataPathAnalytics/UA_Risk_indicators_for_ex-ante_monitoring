package com.datapath.persistence.migration.impl;

import com.datapath.persistence.migration.ApplicationCondition;
import com.datapath.persistence.migration.EnableMigration;
import com.datapath.persistence.migration.Migration;
import com.datapath.persistence.repositories.TenderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@EnableMigration
public class TenderMigration extends Migration {

    private TenderRepository tenderRepository;

    public TenderMigration(List<? extends ApplicationCondition> conditions,
                           TenderRepository tenderRepository) {
        super(conditions);
        this.tenderRepository = tenderRepository;
    }

    @Override
    public void apply() {
        log.info("Applying {} migration", this.getClass().getSimpleName());
        long maxId = tenderRepository.getMaxId();
        long minId = tenderRepository.getMinId();
        long step = 10000;

        log.info("Tender maxId = {}, minId = {}", maxId, minId);

        Long currentId = minId;
        while (currentId <= maxId) {
            tenderRepository.updateTitleFromTenderData(currentId, currentId + step);
            currentId += step;
            log.info("Updated {} tenders", currentId);
        }
        log.info("Tenders titles updated", this.getClass().getSimpleName());
    }
}
