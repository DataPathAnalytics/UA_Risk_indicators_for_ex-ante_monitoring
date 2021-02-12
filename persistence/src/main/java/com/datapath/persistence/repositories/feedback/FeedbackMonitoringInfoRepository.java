package com.datapath.persistence.repositories.feedback;

import com.datapath.persistence.entities.feedback.FeedbackMonitoringInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackMonitoringInfoRepository extends JpaRepository<FeedbackMonitoringInfo, Long> {
    FeedbackMonitoringInfo findByTenderOuterId(String tenderOuterId);

    List<FeedbackMonitoringInfo> findByTenderIdIn(List<String> ids);
}
