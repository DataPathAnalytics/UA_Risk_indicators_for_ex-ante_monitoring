package com.datapath.persistence.repositories.feedback;

import com.datapath.persistence.entities.feedback.FeedbackSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackSummaryRepository extends JpaRepository<FeedbackSummary, Long> {
    FeedbackSummary findByTenderOuterId(String tenderOuterId);

    List<FeedbackSummary> findByTenderIdIn(List<String> tenderIdBatch);
}
