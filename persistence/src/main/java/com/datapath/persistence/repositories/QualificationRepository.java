package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.Qualification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Repository
public interface QualificationRepository extends JpaRepository<Qualification, Long> {

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO qualification (outer_id, status, eligible, qualified, date, lot_id, tender_id)\n" +
            "  SELECT\n" +
            "    c.outer_id,\n" +
            "    c.status,\n" +
            "    c.eligible :::: BOOLEAN,\n" +
            "    c.qualified :::: BOOLEAN,\n" +
            "    c.date :::: TIMESTAMP AT TIME ZONE 'UTC',\n" +
            "    lot.id lot_id,\n" +
            "    c.tender_id tender_id\n" +
            "  FROM (\n" +
            "         SELECT\n" +
            "           tender_id,\n" +
            "           qualification_data ->> 'id'        outer_id,\n" +
            "           qualification_data ->> 'status'    status,\n" +
            "           qualification_data ->> 'eligible'  eligible,\n" +
            "           qualification_data ->> 'qualified' qualified,\n" +
            "           qualification_data ->> 'date'      date,\n" +
            "           CASE WHEN qualification_data ->> 'lotID' IS NOT NULL\n" +
            "             THEN qualification_data ->> 'lotID'\n" +
            "           ELSE 'autocreated' END             lot_outer_id\n" +
            "         FROM (\n" +
            "                SELECT\n" +
            "                  json_array_elements(qualifications) qualification_data,\n" +
            "                  tender_id\n" +
            "                FROM (\n" +
            "                       SELECT\n" +
            "                         cast(data AS JSON) -> 'data' -> 'qualifications' qualifications,\n" +
            "                         tender_id\n" +
            "                       FROM tender_data\n" +
            "                       WHERE id >= ?1 AND id < ?2) a\n" +
            "                WHERE qualifications IS NOT NULL) b\n" +
            "       ) c\n" +
            "    JOIN lot ON c.tender_id = lot.tender_id AND lot_outer_id = lot.outer_id;\n", nativeQuery = true)
    void insertQualification(Long minTenderDataId, Long maxTenderDataId);

}
