package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.MonitoringEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MonitoringRepository extends JpaRepository<MonitoringEntity, String > {
    MonitoringEntity findFirstByOrderByModifiedDateDesc();

    @Query(nativeQuery = true, value = "SELECT * FROM monitoring WHERE status = 'active'")
    List<MonitoringEntity> findAllByActiveStatus();

    List<MonitoringEntity> findAllByTenderIdIn(List<String> tenderIds);
}
