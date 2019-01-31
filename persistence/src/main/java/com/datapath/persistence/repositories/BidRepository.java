package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    Long countAllBySupplierIdentifierLegalNameIsNull();

    @Modifying
    @Transactional
    @Query(value = "WITH bid_supplier_data AS (\n" +
            "    SELECT\n" +
            "      bid_id,\n" +
            "      supplier_data -> 'identifier' ->> 'legalName' legal_name,\n" +
            "      supplier_data -> 'identifier' ->> 'id'        supplier_id,\n" +
            "      supplier_data -> 'identifier' ->> 'scheme'    supplier_sheme\n" +
            "    FROM (\n" +
            "           SELECT\n" +
            "             bid_id,\n" +
            "             json_array_elements(tenderers) supplier_data\n" +
            "           FROM (\n" +
            "                  SELECT\n" +
            "                    json_array_elements(data -> 'data' -> 'bids') ->> 'id'       bid_id,\n" +
            "                    json_array_elements(data -> 'data' -> 'bids') -> 'tenderers' tenderers\n" +
            "\n" +
            "                  FROM (\n" +
            "                         SELECT cast(tender_data.data AS JSON)\n" +
            "                         FROM tender_data\n" +
            "                         WHERE tender_data.id >= ?1 AND tender_data.id <= ?2\n" +
            "                       ) a) b) c)\n" +
            "\n" +
            "\n" +
            "UPDATE bid\n" +
            "SET supplier_identifier_legal_name = bid_supplier_data.legal_name FROM bid_supplier_data\n" +
            "WHERE outer_id = bid_supplier_data.bid_id " +
            "AND supplier_identifier_scheme = bid_supplier_data.supplier_sheme " +
            "AND supplier_identifier_id = bid_supplier_data.supplier_id", nativeQuery = true)
    void updateSupplierNameFromTenderData(long minTenderId, long maxTenderId);
}