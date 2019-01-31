package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.TenderData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenderDataRepository extends PagingAndSortingRepository<TenderData, Long>, JpaRepository<TenderData, Long> {

    @Query("SELECT coalesce(max(td.id), 0) FROM TenderData td")
    Long getMaxId();

    @Query("SELECT coalesce(min(td.id), 0) FROM TenderData td")
    Long getMinId();

}
