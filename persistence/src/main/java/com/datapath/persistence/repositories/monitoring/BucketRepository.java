package com.datapath.persistence.repositories.monitoring;

import com.datapath.persistence.entities.monitoring.BucketItem;
import com.datapath.persistence.entities.monitoring.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BucketRepository extends JpaRepository<BucketItem, Long> {

    List<BucketItem> findAllByUser(User user);

    void deleteAllByUserAndTenderIdIn(User user, List<String> tenderIds);

    Long countByUser(User user);
}
