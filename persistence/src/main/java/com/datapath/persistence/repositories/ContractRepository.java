package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.Contract;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface ContractRepository extends PagingAndSortingRepository<Contract, Long>, JpaRepository<Contract, Long> {

    Contract findFirstBySourceOrderByDateModifiedDesc(String source);

    Contract findFirstByOuterId(String outerId);

    @Query(value = "SELECT contract.outer_id FROM contract\n" +
            "  join tender_contract on contract.tender_contract_id = tender_contract.id\n" +
            "  join tender t on tender_contract.tender_id = t.id\n" +
            "WHERE t.date_created > ?1 AND t.date > now() - INTERVAL '1 year'\n" +
            "      AND t.status IN ?2\n" +
            "      AND t.procurement_method_type IN ?3\n" +
            "      AND t.procuring_entity_kind IN ?4\n" +
            "ORDER BY t.date_created", nativeQuery = true)
    List<String> getContarctIdByTenderSratusAndProcedureTypeAndProcuringEntityType(ZonedDateTime date,
                                                                                   List<String> procedureStatus,
                                                                                   List<String> procedureType,
                                                                                   List<String> procuringEntityKind,
                                                                                   Pageable pageable);


}