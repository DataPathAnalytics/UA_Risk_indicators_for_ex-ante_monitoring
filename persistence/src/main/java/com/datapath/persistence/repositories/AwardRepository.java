package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.Award;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AwardRepository extends JpaRepository<Award, Long> {

    @Query(value = "SELECT count(*) from award where bid_id is null", nativeQuery = true)
    Long countAllByBidIsNull();

    @Query(value = "SELECT tenderid, lotid,\n" +
            "  string_agg(DISTINCT CASE WHEN status = 'active' THEN format END,',') from (\n" +
            "  SELECT\n" +
            "    tender.outer_id tenderid,\n" +
            "    award.outer_id  awardid,\n" +
            "    award.status,\n" +
            "    lot.outer_id    lotid,\n" +
            "    document.format\n" +
            "  FROM document\n" +
            "    RIGHT JOIN award ON document.award_id = award.id\n" +
            "    JOIN lot ON award.lot_id = lot.id\n" +
            "    JOIN tender ON award.tender_id = tender.id\n" +
            "  WHERE\n" +
            "    tender.outer_id = ANY (SELECT regexp_split_to_table(?1, ','))\n" +
            ") a GROUP by tenderid, lotid", nativeQuery = true)
    List<Object[]> getActiveAwardWithDocumentTypesByTenderId(String tenderId);

    @Modifying
    @Transactional
    @Query(value = "WITH award_supplier_data AS (\n" +
            "    SELECT\n" +
            "      award_id,\n" +
            "      supplier_data -> 'identifier' ->> 'legalName' legal_name,\n" +
            "      supplier_data -> 'identifier' ->> 'id'        supplier_id,\n" +
            "      supplier_data -> 'identifier' ->> 'scheme'    supplier_sheme\n" +
            "    FROM (\n" +
            "           SELECT\n" +
            "             award_id,\n" +
            "             json_array_elements(suppliers) supplier_data\n" +
            "           FROM (\n" +
            "                  SELECT\n" +
            "                    json_array_elements(data -> 'data' -> 'awards') ->> 'id'      award_id,\n" +
            "                    json_array_elements(data -> 'data' -> 'awards') -> 'suppliers' suppliers\n" +
            "                  FROM (\n" +
            "                         SELECT cast(tender_data.data AS JSON)\n" +
            "                         FROM tender_data\n" +
            "                         WHERE tender_data.id >= ?1 AND tender_data.id <= ?2\n" +
            "                       ) a) b) c)\n" +
            "UPDATE award\n" +
            "SET supplier_identifier_legal_name = award_supplier_data.legal_name FROM award_supplier_data\n" +
            "WHERE outer_id = award_supplier_data.award_id\n" +
            "      AND supplier_identifier_scheme = award_supplier_data.supplier_sheme\n" +
            "      AND supplier_identifier_id = award_supplier_data.supplier_id", nativeQuery = true)
    void updateSupplierNameFromTenderData(long minTenderId, long maxTenderId);

    @Modifying
    @Transactional
    @Query(value = "WITH award_data AS (select tender_data_id,\n" +
            "                           tender_id,\n" +
            "                           data_award_id,\n" +
            "                           data_bid_id,\n" +
            "                           (select id from bid where outer_id = data_bid_id) bid_id\n" +
            "                    from (select tender_data.id tender_data_id,\n" +
            "                                 tender.id      tender_id,\n" +
            "                                 json_array_elements(tender_data.data :::: JSON -> 'data' -> 'awards') ->> 'id' data_award_id,\n" +
            "                                 json_array_elements(tender_data.data :::: JSON -> 'data' -> 'awards') ->> 'bid_id' data_bid_id\n" +
            "                          from tender_data\n" +
            "                                 JOIN tender tender on tender_data.tender_id = tender.id\n" +
            "                          WHERE tender_data.id >= ?1 AND tender_data.id <= ?2) as data where data_bid_id is not null)  \n" +
            "UPDATE award\n" +
            "SET bid_id = award_data.bid_id\n" +
            "FROM award_data\n" +
            "where award.outer_id = award_data.data_award_id\n" +
            "  and award.tender_id = award_data.tender_id", nativeQuery = true)
    void updateAwardBid(long minTenderId, long maxTenderId);
}