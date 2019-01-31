package com.datapath.persistence.migration.impl;

import com.datapath.persistence.migration.ApplicationCondition;
import com.datapath.persistence.migration.ConditionForMigration;
import com.datapath.persistence.repositories.SupplierRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionForMigration(SupplierMigration.class)
public class ProcuringContactsCondition implements ApplicationCondition {

    private SupplierRepository supplierRepository;

    public ProcuringContactsCondition(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Override
    public boolean check() {
        log.info("Check {}", getClass().getSimpleName());
        Long nullableEmailsCount = supplierRepository.countAllByEmailIsNull();
        Long suppliersCount = supplierRepository.count();
        log.info("{} is {}", getClass().getSimpleName(), suppliersCount.equals(nullableEmailsCount));
        return nullableEmailsCount.equals(suppliersCount);
    }
}
