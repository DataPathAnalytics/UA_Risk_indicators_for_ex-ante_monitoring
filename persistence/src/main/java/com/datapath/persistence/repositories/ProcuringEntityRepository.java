package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.ProcuringEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ProcuringEntityRepository extends JpaRepository<ProcuringEntity, Long> {

    ProcuringEntity findByIdentifierIdAndIdentifierScheme(String identifierId, String identifierScheme);

    Long countAllByRegionIsNull();

    @Query("SELECT coalesce(max(p.id), 0) FROM ProcuringEntity p")
    Long getMaxId();

    @Query("SELECT coalesce(min(p.id), 0) FROM ProcuringEntity p")
    Long getMinId();

    @Modifying
    @Transactional
    @Query(value =
            "WITH pe_subquery AS ( " +
                    "    SELECT " +
                    "      pe.id, " +
                    "      MAX(t.id), " +
                    "      MAX(td.data), " +
                    "      cast(MAX(td.data) AS JSON) " +
                    "            -> 'data' -> 'procuringEntity'-> 'address' ->> 'region' " +
                    "                AS procuring_entity_region\n" +
                    "    FROM procuring_entity pe " +
                    "      LEFT JOIN tender t ON pe.id = t.procuring_entity_id " +
                    "      LEFT JOIN tender_data td ON t.id = td.tender_id " +
                    "    WHERE pe.id >= ?1 AND pe.id <= ?2 " +
                    "    GROUP BY pe.id " +
                    ") " +
                    "UPDATE procuring_entity " +
                    "SET region = pe_subquery.procuring_entity_region FROM pe_subquery " +
                    "WHERE procuring_entity.id = pe_subquery.id",
            nativeQuery = true)
    void updateRegionFromTenderData(Long minId, Long maxId);
}
