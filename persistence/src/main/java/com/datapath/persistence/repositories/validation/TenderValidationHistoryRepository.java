package com.datapath.persistence.repositories.validation;

import com.datapath.persistence.entities.validation.TenderValidationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TenderValidationHistoryRepository extends JpaRepository<TenderValidationHistory, Integer> {

    List<TenderValidationHistory> findAllByOrderByDateDesc();

}
