package com.datapath.persistence.repositories.feedback;

import com.datapath.persistence.entities.feedback.FeedbackIndicator;
import com.datapath.persistence.entities.feedback.FeedbackIndicatorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface FeedbackIndicatorRepository extends JpaRepository<FeedbackIndicator, FeedbackIndicatorId> {

    List<FeedbackIndicator> findAllByTenderOuterId(String tenderOuterId);

    List<FeedbackIndicator> findByTenderIdIn(List<String> tenderIdBatch);

    @Modifying
    void deleteAllByTenderOuterId(String tenderOuterId);
}
