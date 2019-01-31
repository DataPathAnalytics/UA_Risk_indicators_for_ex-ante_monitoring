package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Supplier findFirstByIdentifierIdAndIdentifierScheme(String identifierId, String identifierScheme);

    Long countAllByEmailIsNull();

    @Query("SELECT coalesce(max(s.id), 0) FROM Supplier s")
    Long getMaxId();

    @Query("SELECT coalesce(min(s.id), 0) FROM Supplier s")
    Long getMinId();

    @Modifying
    @Transactional
    @Query(value = "WITH supplier_data AS (\n" +
            "    SELECT *\n" +
            "    FROM (\n" +
            "           SELECT\n" +
            "             supplier_data -> 'identifier' ->> 'scheme'      sheme,\n" +
            "             supplier_data -> 'identifier' ->> 'id'          id,\n" +
            "             supplier_data -> 'contactPoint' ->> 'telephone' phone,\n" +
            "             supplier_data -> 'contactPoint' ->> 'email'     email\n" +
            "           FROM (\n" +
            "                  SELECT json_array_elements(json_array_elements(json_build_array(\n" +
            "                                                                     CASE WHEN suppliers IS NOT NULL\n" +
            "                                                                       THEN suppliers\n" +
            "                                                                     ELSE json_build_array(json_object('{}')) END,\n" +
            "                                                                     CASE WHEN tenderers IS NOT NULL\n" +
            "                                                                       THEN tenderers\n" +
            "                                                                     ELSE json_build_array(json_object('{}')) END)\n" +
            "                                             )) supplier_data\n" +
            "                  FROM (\n" +
            "                         SELECT\n" +
            "                           json_array_elements(data -> 'data' -> 'awards') -> 'suppliers' suppliers,\n" +
            "                           json_array_elements(data -> 'data' -> 'bids') -> 'tenderers'   tenderers\n" +
            "\n" +
            "                         FROM (\n" +
            "                                SELECT cast(tender_data.data AS JSON)\n" +
            "                                FROM tender_data\n" +
            "                                WHERE tender_data.id >= ?1 AND tender_data.id <= ?2) a\n" +
            "                       ) b) tenderSupplier) result\n" +
            "    WHERE id IS NOT NULL\n" +
            ") UPDATE supplier\n" +
            "SET telephone = supplier_data.phone, email = supplier_data.email FROM supplier_data\n" +
            "WHERE supplier.identifier_scheme = supplier_data.sheme AND supplier.identifier_id = supplier_data.id", nativeQuery = true)
    void updateEmailFromTenderData(Long minId, Long maxId);
}
