package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.ContractChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractChangeRepository extends PagingAndSortingRepository<ContractChange, Long>, JpaRepository<ContractChange, Long> {

    @Query(value = "select c2.outer_id, " +
            "count( DISTINCT case WHEN c4.status = 'active' and 'itemPriceVariation' = any(c4.rationale_types) THEN  c4.id END ) " +
            "from tender \n " +
            "  LEFT join tender_contract tc on tender.id = tc.tender_id\n" +
            "  left join contract c2 on tc.id = c2.tender_contract_id\n" +
            "  left join contract_change c4 on c2.id = c4.contract_id\n" +
            "where c2.outer_id = ANY (SELECT regexp_split_to_table(?1, ',')) " +
            "GROUP BY c2.outer_id", nativeQuery = true)
    List<Object[]> getActiveChangesWithItemPriceVariationByTenderIds(String contractIds);

    @Query(value = "select c2.outer_id," +
            " count(DISTINCT c4.id) allids, " +
            "count(DISTINCT  case WHEN  c4.status = 'active' THEN c4.id END ) activeids \n" +
            "from contract c2 " +
            "  left join contract_change c4 on c2.id = c4.contract_id\n" +
            "where c2.outer_id = ANY (SELECT regexp_split_to_table(?1, ','))" +
            " GROUP BY c2.outer_id", nativeQuery = true)
    List<Object[]> getActiveChangesByTenderIds(String tenderId);

}

