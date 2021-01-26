package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface ContractRepository extends PagingAndSortingRepository<Contract, Long>, JpaRepository<Contract, Long> {

    Contract findFirstBySourceAndDateModifiedIsNotNullOrderByDateModifiedDesc(String source);

    Contract findFirstByOuterId(String outerId);

    @Query(value = "SELECT contract.outer_id FROM contract\n" +
            "  JOIN tender_contract ON contract.tender_contract_id = tender_contract.id\n" +
            "  JOIN tender t ON tender_contract.tender_id = t.id\n" +
            "WHERE t.date_created > ?1 AND t.date > now() - INTERVAL '1 year'\n" +
            "      AND t.status IN ?2\n" +
            "      AND t.procurement_method_type IN ?3\n" +
            "      AND t.procuring_entity_kind IN ?4\n" +
            "ORDER BY t.date_created LIMIT 100", nativeQuery = true)
    List<String> getContarctIdByTenderSratusAndProcedureTypeAndProcuringEntityType(ZonedDateTime date,
                                                                                   List<String> procedureStatus,
                                                                                   List<String> procedureType,
                                                                                   List<String> procuringEntityKind);

    @Query(value = "SELECT c.*\n" +
            "FROM contract c\n" +
            "       " +
            "  JOIN tender t ON t.id = c.tender_id\n" +
            "WHERE c.date_created > ?1\n" +
            "  AND c.date_modified > now() - INTERVAL '1 year'\n" +
            "  AND c.status IN ?2\n" +
            "  AND t.procuring_entity_kind IN ?3\n" +
            "ORDER BY c.date_created\n" +
            "LIMIT 100", nativeQuery = true)
    List<Contract> findContracts(ZonedDateTime date,
                                 List<String> statuses,
                                 List<String> procuringEntityKind);

    @Query(value = "SELECT c.*\n" +
            "FROM contract c\n" +
            "       " +
            "  JOIN tender t ON t.id = c.tender_id\n" +
            "WHERE c.date_created > ?1\n" +
            "  AND c.date_modified > now() - INTERVAL '1 year'\n" +
            "  AND c.status IN ?2\n" +
            "  AND t.procurement_method_type IN ?3\n" +
            "  AND t.procuring_entity_kind IN ?4\n" +
            "ORDER BY c.date_created\n" +
            "LIMIT 100", nativeQuery = true)
    List<Contract> findContracts(ZonedDateTime date,
                                 List<String> statuses,
                                 List<String> procedureTypes,
                                 List<String> procuringEntityKinds);

    @Query(value = "SELECT c.id\n" +
            "FROM contract c\n" +
            "       " +
            "  JOIN tender t ON t.id = c.tender_id\n" +
            "WHERE c.date_created > ?1\n" +
            "  AND c.date_modified > now() - INTERVAL '1 year'\n" +
            "  AND c.status IN ?2\n" +
            "  AND t.procuring_entity_kind IN ?3\n" +
            "ORDER BY c.date_created\n" +
            "LIMIT 100", nativeQuery = true)
    List<Long> findContractIds(ZonedDateTime date,
                              List<String> statuses,
                              List<String> procuringEntityKind);

    @Query(value = "select * from contract where id in ?1", nativeQuery = true)
    List<Contract> findByIdIn(List<Long> ids);

}