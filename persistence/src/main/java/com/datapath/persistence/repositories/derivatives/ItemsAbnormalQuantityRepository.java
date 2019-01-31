package com.datapath.persistence.repositories.derivatives;

import com.datapath.persistence.entities.derivatives.ItemsAbnormalQuantity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemsAbnormalQuantityRepository extends JpaRepository<ItemsAbnormalQuantity, Long> {

}
