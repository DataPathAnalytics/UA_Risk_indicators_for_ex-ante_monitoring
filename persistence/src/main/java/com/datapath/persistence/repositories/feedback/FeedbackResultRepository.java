package com.datapath.persistence.repositories.feedback;

import com.datapath.persistence.entities.feedback.FeedbackResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackResultRepository extends JpaRepository<FeedbackResult, Long> {
    FeedbackResult findByTenderOuterId(String tenderOuterId);

    List<FeedbackResult> findByTenderIdIn(List<String> tenderIdBatch);
}
