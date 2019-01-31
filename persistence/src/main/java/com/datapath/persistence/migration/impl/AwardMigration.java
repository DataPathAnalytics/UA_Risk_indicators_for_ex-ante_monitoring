package com.datapath.persistence.migration.impl;

import com.datapath.persistence.migration.ApplicationCondition;
import com.datapath.persistence.migration.EnableMigration;
import com.datapath.persistence.migration.Migration;
import com.datapath.persistence.repositories.AwardRepository;
import com.datapath.persistence.repositories.TenderDataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@EnableMigration
public class AwardMigration extends Migration {

    private AwardRepository awardRepository;
    private TenderDataRepository tenderDataRepository;

    public AwardMigration(List<? extends ApplicationCondition> conditions,
                          AwardRepository awardRepository,
                          TenderDataRepository tenderDataRepository) {
        super(conditions);
        this.awardRepository = awardRepository;
        this.tenderDataRepository = tenderDataRepository;
    }

    @Override
    public void apply() {
        log.info("Applying {} migration", this.getClass().getSimpleName());
        long maxId = tenderDataRepository.getMaxId();
        long minId = tenderDataRepository.getMinId();
        long step = 1000;

        log.info("Tender data maxId = {}, minId = {}", maxId, minId);

        ExecutorService executorService = Executors.newFixedThreadPool(50);

        Long currentId = minId;
        while (currentId <= maxId) {
            final long tempCurrentId = currentId;
            executorService.execute(() -> {
                awardRepository.updateAwardBid(tempCurrentId, tempCurrentId + step);
                log.info("Updated 500 awards");
            });
            currentId += step;
        }

        executorService.shutdown();

        while (!executorService.isTerminated()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        log.info("Awards updated", this.getClass().getSimpleName());
    }
}
