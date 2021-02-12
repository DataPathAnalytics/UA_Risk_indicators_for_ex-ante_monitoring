package com.datapath.persistence.repositories.feedback;

import com.datapath.persistence.entities.feedback.FeedbackViolation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackViolationRepository extends JpaRepository<FeedbackViolation, Long> {
    FeedbackViolation findByTenderOuterId(String tenderOuterId);

    List<FeedbackViolation> findByTenderIdIn(List<String> tenderIdBatch);
}
