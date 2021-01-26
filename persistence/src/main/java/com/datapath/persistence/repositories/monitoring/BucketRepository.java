package com.datapath.persistence.repositories.monitoring;

import com.datapath.persistence.entities.monitoring.BucketItem;
import com.datapath.persistence.entities.monitoring.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BucketRepository extends JpaRepository<BucketItem, Long> {

    List<BucketItem> findAllByUserOrAssigned(User user, User assigned);

    List<BucketItem> findAllByAssigned(User user);

    void deleteAllByUserAndTenderIdIn(User user, List<String> tenderIds);

    void deleteAllByUser(User user);

    Long countByUser(User user);

    List<BucketItem> findAllByUserAndTenderIdIn(User user, List<String> ids);

    boolean existsByTenderIdAndUser(String tenderId, User user);
}
