package com.datapath.persistence.repositories.derivatives;

import com.datapath.persistence.entities.derivatives.SuppliersSingleBuyer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SuppliersSingleBuyerRepository extends JpaRepository<SuppliersSingleBuyer, Long> {

    @Query(value = "SELECT ssb.supplier FROM SuppliersSingleBuyer ssb where ssb.buyerId=?1 and ssb.supplier = ?2")
    List<String> getBuyersSuppliers(String buyerId, String suppliers);

}
