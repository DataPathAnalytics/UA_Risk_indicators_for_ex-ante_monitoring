package com.datapath.persistence.repositories.feedback;

import com.datapath.persistence.entities.feedback.FeedbackIndicatorResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackIndicatorResponseRepository extends JpaRepository<FeedbackIndicatorResponse, Integer> {
}
