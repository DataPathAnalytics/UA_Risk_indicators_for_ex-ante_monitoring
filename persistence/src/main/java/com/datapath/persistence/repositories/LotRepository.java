package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.Lot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LotRepository extends JpaRepository<Lot, Long> {

    @Query(value = "select DISTINCT l.outer_id from lot l join award a on l.id = a.lot_id where a.outer_id = ?1 ", nativeQuery = true)
    List<String> findByAwardId(String awardId);

    @Query(value = "SELECT\n" +
            "  tender.outer_id tenderid, lot.outer_id,\n" +
            "  CASE WHEN lot.currency <> lot.guarantee_currency\n" +
            "    THEN\n" +
            "      CASE WHEN lot.currency = 'UAH'\n" +
            "        THEN lot.amount\n" +
            "      ELSE\n" +
            "        lot.amount *\n" +
            "        (SELECT rate\n" +
            "         FROM exchange_rate\n" +
            "         WHERE lot.currency = exchange_rate.code AND\n" +
            "               concat(substr(to_char(coalesce(tender.start_date, tender.date), 'YYYY-MM-DD HH:mm:ss.zzzzzz'), 0, 11),\n" +
            "                      ' 00:00:00.000000') :::: DATE = exchange_rate.date)\n" +
            "      END ELSE lot.amount\n" +
            "  END amount,\n" +
            "  CASE WHEN lot.currency <> lot.guarantee_currency\n" +
            "    THEN\n" +
            "      CASE WHEN lot.guarantee_currency = 'UAH'\n" +
            "        THEN lot.guarantee_amount\n" +
            "      ELSE\n" +
            "        lot.guarantee_amount *\n" +
            "        (SELECT rate\n" +
            "         FROM exchange_rate\n" +
            "         WHERE lot.guarantee_currency = exchange_rate.code AND\n" +
            "               concat(substr(to_char(coalesce(tender.start_date, tender.date), 'YYYY-MM-DD HH:mm:ss.zzzzzz'), 0, 11),\n" +
            "                      ' 00:00:00.000000') :::: DATE = exchange_rate.date)\n" +
            "      END ELSE lot.guarantee_amount\n" +
            "  END guarantee_amount\n" +
            "FROM lot\n" +
            "  JOIN tender ON lot.tender_id = tender.id\n" +
            "WHERE tender.outer_id in ?1 " +
            "AND case WHEN lot.outer_id<>'autocreated' THEN lot.status = 'active'  ELSE  (lot.status = 'active' or lot.status <> 'active') END ", nativeQuery = true)
    List<Object[]> getActiveLotsAmountAndGuaranteeAmountWithCurrencies(List<String> tenderId);

    @Query(value = "" +
            "SELECT\n" +
            "  tender_outer_id, \n" +
            "  lot_outer_id,\n" +
            "  CASE WHEN (sum(CASE WHEN award_status = 'active' THEN 1 ELSE 0 END) > 0) THEN 1  ELSE 0 END winner,\n" +
            "  count(DISTINCT (CASE WHEN award_status = 'unsuccessful'\n" +
            "    THEN supplier END)) disquals,\n" +
            "  count(DISTINCT (CASE WHEN bid_status = 'active'\n" +
            "    THEN bidder END))   participation\n" +
            "FROM (\n" +
            "\n" +
            "       SELECT\n" +
            "         tender_outer_id,\n" +
            "         lot_outer_id,\n" +
            "         a.lot_id,\n" +
            "         bid.status                                                                 bid_status,\n" +
            "         award.status                                                               award_status,\n" +
            "         concat(bid.supplier_identifier_scheme, '', bid.supplier_identifier_id)     bidder,\n" +
            "         concat(award.supplier_identifier_scheme, '', award.supplier_identifier_id) supplier\n" +
            "       FROM (\n" +
            "\n" +
            "              SELECT\n" +
            "                tender.outer_id tender_outer_id,\n" +
            "                lot.outer_id    lot_outer_id,\n" +
            "                lot.id          lot_id\n" +
            "              FROM tender\n" +
            "                JOIN lot ON tender.id = lot.tender_id\n" +
            "              WHERE tender.outer_id = ANY (SELECT regexp_split_to_table(?1, ',')) " +
            "            ) a\n" +
            "         JOIN bid_lot ON a.lot_id = bid_lot.lot_id\n" +
            "         JOIN bid ON bid_lot.bid_id = bid.id\n" +
            "         JOIN award ON a.lot_id = award.lot_id\n" +
            "     ) b\n" +
            "GROUP BY tender_outer_id, lot_outer_id;", nativeQuery = true)
    List<Object[]> findLotWinnerDisqualsParticipationsByTenderId(String tenderId);

    @Query(value = "select * from lot where id in (select lot_id from bid_lot where bid_id = :bidId) and tender_id = :tenderId", nativeQuery = true)
    List<Lot> findByBidIdAndTenderId(@Param("bidId") Long bidId, @Param("tenderId") Long tenderId);
}
