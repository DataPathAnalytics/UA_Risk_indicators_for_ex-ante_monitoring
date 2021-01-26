package com.datapath.persistence.repositories.derivatives;

import com.datapath.persistence.entities.derivatives.NoNeed;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoNeedRepository extends JpaRepository<NoNeed, Integer> {

    List<NoNeed> findByProcuringEntity(String procuringEntity);
}
