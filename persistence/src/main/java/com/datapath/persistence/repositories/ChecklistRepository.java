package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.Checklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import javax.transaction.Transactional;
import java.util.List;

public interface ChecklistRepository extends JpaRepository<Checklist, Long> {

    Checklist findByTenderOuterId(String tenderOuterId);

    List<Checklist> findByTenderOuterIdIn(List<String> ids);

    @Modifying
    @Transactional
    void removeByTenderOuterId(String id);
}
