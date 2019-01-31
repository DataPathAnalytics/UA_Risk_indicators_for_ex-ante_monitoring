package com.datapath.persistence.repositories.derivatives;

import com.datapath.persistence.entities.derivatives.BiddersForBuyers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BiddersForBuyersRepository extends JpaRepository<BiddersForBuyers, Long> {
    @Query(value = "select distinct bb.procuringEntity from BiddersForBuyers bb where bb.supplier = ?1")
    List<String> getProcuringEntitiesBySupplier(String supplier);

}
