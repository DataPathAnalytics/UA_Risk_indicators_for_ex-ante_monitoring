package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.TenderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface TenderItemRepository extends JpaRepository<TenderItem, Long> {

    Long countAllByQuantityIsNull();

    @Query("SELECT coalesce(max(t.id), 0) FROM TenderItem t")
    Long getMaxId();

    @Query("SELECT coalesce(min(t.id), 0) FROM TenderItem t")
    Long getMinId();

    @Transactional
    @Modifying
    @Query(value = "" +
            "WITH item_quantity AS (\n" +
            "    SELECT\n" +
            "      item ->> 'id'       id,\n" +
            "      item ->> 'quantity' quantity\n" +
            "    FROM (\n" +
            "           SELECT json_array_elements(cast(data AS JSON) -> 'data' -> 'items') item\n" +
            "           FROM (\n" +
            "                  SELECT DISTINCT data\n" +
            "                  FROM tender_data\n" +
            "                    JOIN tender t ON tender_data.tender_id = t.id\n" +
            "                    JOIN tender_item t2 ON t.id = t2.tender_id\n" +
            "                  WHERE t2.id >= ?1 AND t2.id <= ?2\n" +
            "                ) a) b\n" +
            ")\n" +
            "UPDATE tender_item\n" +
            "SET quantity = CASE WHEN item_quantity.quantity :::: DOUBLE PRECISION > 9223372036854775807\n" +
            "  THEN NULL ELSE item_quantity.quantity :::: DOUBLE PRECISION END FROM item_quantity\n" +
            "WHERE tender_item.outer_id = item_quantity.id\n" +
            "AND tender_item.id >= ?1 AND tender_item.id <= ?2",
            nativeQuery = true)
    void updateQuantityFromTenderData(Long minId, Long maxId);

    @Query(value = "SELECT tender.outer_id, tender.amount, sum(quantity) " +
            "FROM tender_item " +
            "  JOIN tender ON tender_item.tender_id = tender.id " +
            "WHERE status = 'unsuccessful' " +
            "      AND procurement_method_type IN ('aboveThresholdUA', 'aboveThresholdEU') " +
            "      AND tender.tv_procuring_entity = ?1 " +
            "      AND tender_item.classification_id = ?2 " +
            "GROUP BY tender.outer_id, tender.amount, tender.date " +
            "ORDER BY tender.date DESC " +
            "LIMIT 1", nativeQuery = true)
    Object getQuantityByProcuringEntityAndCPV(String procuringEntity, String cpv);
}