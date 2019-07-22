package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.Tender;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.Size;
import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface TenderRepository extends PagingAndSortingRepository<Tender, Long>, JpaRepository<Tender, Long> {

    Tender findFirstBySourceOrderByDateModifiedDesc(String source);

    Tender findFirstByOuterId(String outerId);

    Long countAllByTitleIsNull();

    Long countAllByProcuringEntityKindIsNull();

    @Query(value = "select max(t.dateModified) from Tender t")
    ZonedDateTime findMaxDateModified();

    void deleteAllByDateBefore(ZonedDateTime date);

    @Modifying
    @Transactional
    long deleteAllByOuterId(String outerId);

    boolean existsByOuterIdAndDateModifiedAfter(String outerId, ZonedDateTime dateModified);

    @Query(value = "SELECT outer_id, tender_id, date, date_modified, procurement_method_type, status, date_created  FROM tender WHERE outer_id = ANY (SELECT regexp_split_to_table(?1, ','))", nativeQuery = true)
    List<Object[]> findAllByOuterIdIn(String outerIds);

    @Query("SELECT coalesce(max(t.id), 0) FROM Tender t")
    Long getMaxId();

    @Query("SELECT coalesce(min(t.id), 0) FROM Tender t")
    Long getMinId();


    @Query(value = "SELECT t.outerId from Tender t where t.outerId in ?1")
    List<String> findAllOuterIdsByOuterIdIn(List<String> outerIds);

    @Query(value = "SELECT t.outerId from Tender t where t.outerId in ?2 and t.status = 'complete' and t.date > ?1")
    List<String> findCompletedTenderNotOlderThanByOuterIdIn(ZonedDateTime zonedDateTime, List<String> outerIds);

    @Query(value = "SELECT t.outerId from Tender t where t.outerId in ?1 and t.status <> 'complete'")
    List<String> findNotCompletedTenders(List<String> outerIds);

    @Query(value = "SELECT t.outerId, t.dateModified from Tender t where t.outerId in ?1")
    List<Object[]> findAllOuterIdsAndDateModifiedByOuterIdIn(List<String> outerIds);

    @Query(value = "Select t.outerId, amount from Tender t " +
            "where t.status not in ?1 " +
            "and t.procurementMethodType in ('aboveThresholdUA', 'aboveThresholdEU', 'belowThreshold') " +
            "and t.procuringEntityKind in ('general', 'special')" +
            "and amount is not null order by amount")
    List<Object> findTendersWithAmountByExcludingStatus(List<String> statuses);

    @Query(value = "Select t.outer_id, amount from tender t " +
            "where t.status not in ?1 " +
            "and t.outer_id = ANY (SELECT regexp_split_to_table(?2, ',')) " +
            "and amount is not null order by amount", nativeQuery = true)
    List<Object> findTendersWithAmountByTendersExcludingStatus(List<String> statuses, String tenderIds);

    @Query(value = "" +
            "select tenderid,\n" +
            "       lotid,\n" +
            "       award_date,\n" +
            "       case\n" +
            "         when procurement_method_type = 'aboveThresholdUA' then min_bid_doc_date\n" +
            "         else least(min_bid_doc_date, min_eligibility_doc_date, min_financial_doc_date) end min_date,\n" +
            "       docs\n" +
            "from (select tenderid,\n" +
            "             lotid,\n" +
            "             procurement_method_type,\n" +
            "             award.date                               award_date,\n" +
            "             case\n" +
            "               when procurement_method_type = 'aboveThresholdUA'\n" +
            "                       then count(distinct case\n" +
            "                                             when document.document_of <> 'lot' or document.related_item = lotid\n" +
            "                                                     then document.id end)\n" +
            "               else greatest(count(distinct case\n" +
            "                                               when document.document_of <> 'lot' or document.related_item = lotid\n" +
            "                                                       then document.id end),\n" +
            "                              count(distinct case\n" +
            "                                               when eligibility_document.document_of <> 'lot' or\n" +
            "                                                    eligibility_document.related_item = lotid\n" +
            "                                                       then eligibility_document.id end),\n" +
            "                              count(distinct case\n" +
            "                                               when financial_document.document_of <> 'lot' or\n" +
            "                                                    financial_document.related_item = lotid\n" +
            "                                                       then financial_document.id end))\n" +
            "                 end                                  docs,\n" +
            "             min(document.date_published)             min_bid_doc_date,\n" +
            "             min(eligibility_document.date_published) min_eligibility_doc_date,\n" +
            "             min(financial_document.date_published)   min_financial_doc_date\n" +
            "\n" +
            "      from (select tenderid, lotid, procurement_method_type, unnest(active_award_id) :::: BIGINT award_id\n" +
            "            from (select tenderid,\n" +
            "                         procurement_method_type,\n" +
            "                         lotid,\n" +
            "                         case\n" +
            "                           when active_award_id is null\n" +
            "                                   then '{null}'\n" +
            "                           else string_to_array(active_award_id, ',') end active_award_id\n" +
            "                  from (select tender.outer_id                                                                      tenderid,\n" +
            "                               tender.procurement_method_type,\n" +
            "                               lot.outer_id                                                                         lotid,\n" +
            "                               string_agg(\n" +
            "                                 case when award.status = 'active' then award.id :::: TEXT end,\n" +
            "                                 ',')                                                                               active_award_id\n" +
            "                        from tender\n" +
            "                               join lot on tender.id = lot.tender_id\n" +
            "                               join award on lot.id = award.lot_id\n" +
            "                                WHERE tender.outer_id = ANY (SELECT regexp_split_to_table(?1, ','))\n" +
            "                        group by tender.outer_id,\n" +
            "                                 procurement_method_type,\n" +
            "                                 lot.outer_id) a)b)c\n" +
            "             left join award on award.id = award_id\n" +
            "             left join bid on award.bid_id = bid.id\n" +
            "             left join document on bid.id = document.bid_id\n" +
            "             left join eligibility_document on bid.id = eligibility_document.bid_id\n" +
            "             left join financial_document on bid.id = financial_document.bid_id\n" +
            "      group by tenderid, lotid, award.date, procurement_method_type) a\n", nativeQuery = true)
    List<Object[]> getTenderLotWithActiveAwardDateMinDocumentPublishedAndDocsCount(String tenderIds);


    @Query(value = "select tenderid,\n" +
            "       lotid,\n" +
            "       unsuccessful_awards,\n" +
            "       bid.supplier_identifier_scheme,\n" +
            "       bid.supplier_identifier_id,\n" +
            "       (select cpv_count\n" +
            "        from supplier_for_pe_with_3cpv\n" +
            "        where supplier_for_pe_with_3cpv.supplier = concat(bid.supplier_identifier_scheme, '-', bid.supplier_identifier_id)\n" +
            "          and procuring_entity = a.procuring_entity)\n" +
            "from (select tender.outer_id                                                           tenderid,\n" +
            "             tender.tv_procuring_entity                                                procuring_entity,\n" +
            "             lot.outer_id                                                              lotid,\n" +
            "             count(distinct case when award.status = 'unsuccessful' then award.id end) unsuccessful_awards,\n" +
            "             string_agg(distinct case when award.status = 'active' then award.id :::: text end,\n" +
            "                                 ',')                                                  active_awards\n" +
            "      from tender\n" +
            "             join lot on tender.id = lot.tender_id\n" +
            "             join award on lot.id = award.lot_id\n" +
            "             join bid on award.bid_id = bid.id\n" +
            "      where tender.outer_id = ANY (SELECT regexp_split_to_table(?1, ','))\n" +
            "      group by tender.outer_id, lot.outer_id, tender.tv_procuring_entity) a\n" +
            "       left join award on active_awards :::: BIGINT = award.id\n" +
            "       left join bid on award.bid_id = bid.id", nativeQuery = true)
    List<Object[]> findTendersWithLotUnsuccessfulAwardsSupplierAndCpvCount(String tenderIds);

    @Query(value = "SELECT\n" +
            "  tender.outer_id,\n" +
            "  count(edrpou.id)\n" +
            "FROM tender\n" +
            "  JOIN bid ON tender.id = bid.tender_id\n" +
            "  LEFT JOIN edrpou ON concat(bid.supplier_identifier_id, '-', bid.supplier_identifier_scheme) = edrpou.supplier\n" +
            "WHERE tender.date_created > ?1 AND tender.date > now() - INTERVAL '1 year'\n" +
            "      AND tender.status IN ?2\n" +
            "      AND tender.procurement_method_type IN ?3\n" +
            "      AND tender.procuring_entity_kind IN ?4\n" +
            "GROUP BY tender.outer_id, tender.date_created \n" +
            "ORDER BY tender.date_created", nativeQuery = true)
    List<Object[]> findTendersWithUnuniqueTenderersCount(ZonedDateTime date,
                                                         List<String> procedureStatus,
                                                         List<String> procedureType,
                                                         List<String> procuringEntityKind,
                                                         Pageable pageable);


    @Query(value = "" +
            "SELECT\n" +
            "  tender.outer_id,\n" +
            "  array_to_string(tv_tender_cpv_list, ','),\n" +
            "  count(DISTINCT CASE WHEN tender_contract.status = 'pending'\n" +
            "    THEN tender_contract.id END)\n" +
            "FROM tender\n" +
            "  LEFT JOIN tender_contract ON tender.id = tender_contract.tender_id\n" +
            "WHERE tender.date_created > ?1 AND tender.date > now() - INTERVAL '1 year'\n" +
            "      AND tender.status IN ?2\n" +
            "      AND tender.procurement_method_type IN ?3\n" +
            "      AND tender.procuring_entity_kind IN ?4\n" +
            "GROUP BY tender.outer_id, tender.date_created, tender.tv_tender_cpv_list " +
            "ORDER BY tender.date_created ", nativeQuery = true)
    List<Object[]> findTendersWithCPVAndPendingContractsCount(ZonedDateTime date,
                                                              List<String> procedureStatus,
                                                              List<String> procedureType,
                                                              List<String> procuringEntityKind,
                                                              Pageable pageable);

    @Query(value = "SELECT tender.outer_id, tender.date_created, string_agg(document.format, ',') FROM tender " +
            "LEFT JOIN document ON tender.id = document.tender_id " +
            "WHERE tender.date_created > ?1 AND tender.date > now() - INTERVAL '1 year' " +
            "AND tender.procurement_method_type IN ?2 " +
            "AND tender.procuring_entity_kind IN ?3  AND tender.status <> 'cancelled' " +
            "GROUP BY tender.outer_id, tender.date_created " +
            "ORDER BY tender.date_created", nativeQuery = true)
    List<Object[]> getTendersWithDocumentTypes(ZonedDateTime datetime,
                                               List<String> procedureType,
                                               List<String> procuringEntityKind,
                                               Pageable pageable);

    @Query(value = "SELECT t.outer_id FROM tender t " +
            "WHERE t.date_created > ?1 AND t.date > now() - INTERVAL '1 year' " +
            "AND t.status IN ?2 " +
            "AND t.procurement_method_type IN ?3 " +
            "AND t.procuring_entity_kind IN ?4 " +
            "ORDER BY t.date_created", nativeQuery = true)
    List<String> findTenderIdByProcedureStatusAndProcedureType(ZonedDateTime date,
                                                               List<String> procedureStatus,
                                                               List<String> procedureType,
                                                               List<String> procuringEntityKind,
                                                               Pageable pageable);

    @Query(value = "SELECT t.outer_id FROM tender t " +
            "WHERE t.procurement_method_type IN ?3 " +
            "AND t.status IN ?2 AND t.date_created > ?1 AND t.date > now() - INTERVAL '1 year' " +
            "AND t.procuring_entity_kind IN ?4 " +
            "AND t.tv_subject_of_procurement NOT LIKE '45%'" +
            "ORDER BY t.date_created", nativeQuery = true)
    List<String> findGoodsServicesTenderIdByProcedureStatusAndProcedureType(ZonedDateTime date,
                                                                            List<String> procedureStatus,
                                                                            List<String> procedureType,
                                                                            List<String> procuringEntityKind,
                                                                            Pageable pageable);

    @Query(value = "SELECT t.outer_id FROM tender t " +
            "WHERE t.procurement_method_type IN ?3 " +
            "AND t.status IN ?2 " +
            "AND t.date_created > ?1 AND t.date > now() - INTERVAL '1 year' " +
            "AND t.procuring_entity_kind IN ?4 " +
            "AND t.tv_subject_of_procurement LIKE '45%' " +
            "ORDER BY t.date_created", nativeQuery = true)
    List<String> findWorksTenderIdByProcedureStatusAndProcedureType(ZonedDateTime date,
                                                                    List<String> procedureStatus,
                                                                    List<String> procedureType,
                                                                    List<String> procuringEntityKind,
                                                                    Pageable pageable);

    @Query(value = "" +
            "SELECT\n" +
            "  tender.outer_id,\n" +
            "  count(DISTINCT CASE WHEN tender_contract.status = 'pending'\n" +
            "    THEN tender_contract.outer_id\n" +
            "                 ELSE NULL END),\n" +
            "  CASE WHEN procured_cpv.id IS NOT NULL\n" +
            "    THEN FALSE\n" +
            "  ELSE TRUE END  contains_cpv,\n" +
            "  CASE WHEN tender.amount >=\n" +
            "            CASE\n" +
            "            WHEN procuring_entity_kind = 'general' AND tv_tender_cpv NOT LIKE '45%'\n" +
            "              THEN 200000\n" +
            "            WHEN procuring_entity_kind = 'general' AND tv_tender_cpv LIKE '45%'\n" +
            "              THEN 1500000\n" +
            "            WHEN procuring_entity_kind = 'special' AND tv_tender_cpv NOT LIKE '45%'\n" +
            "              THEN 1000000\n" +
            "            WHEN procuring_entity_kind = 'special' AND tv_tender_cpv LIKE '45%'\n" +
            "              THEN 5000000\n" +
            "            END\n" +
            "    THEN TRUE\n" +
            "  ELSE FALSE END amount_limit_condition,\n" +
            "  CASE WHEN general_special.id IS NOT NULL OR nature_monopoly_procuring_entity.id IS NOT NULL\n" +
            "    THEN TRUE\n" +
            "  ELSE FALSE END\n" +
            "    monopoly_supplier,\n" +
            "  case WHEN cause = 'noCompetition' THEN true ELSE FALSE END noCompetition\n" +
            "FROM tender\n" +
            "  LEFT JOIN tender_contract ON tender.id = tender_contract.tender_id\n" +
            "  LEFT JOIN procured_cpv ON procured_cpv.cpv = ANY (tv_tender_cpv_list)\n" +
            "  LEFT JOIN supplier ON tender_contract.supplier_id = supplier.id\n" +
            "  LEFT JOIN general_special ON supplier.identifier_id = general_special.procuring_entity_id\n" +
            "  LEFT JOIN nature_monopoly_procuring_entity ON supplier.identifier_id = nature_monopoly_procuring_entity.identifier_id\n" +
            "WHERE tender.date_created > ?1 AND tender.date > now() - INTERVAL '1 year'\n" +
            "      AND tender.status IN ?2 \n" +
            "      AND tender.procurement_method_type IN ?3 \n" +
            "      AND tender.procuring_entity_kind IN ?4 \n" +
            "GROUP BY tender.outer_id, tv_tender_cpv_list, tender.amount, procuring_entity_kind, tv_tender_cpv, general_special.id,\n" +
            "  procured_cpv.id, date_created, nature_monopoly_procuring_entity.id, general_special.id, tender.cause\n" +
            "ORDER BY date_created", nativeQuery = true)
    List<Object[]> findTenderIdsWithCPVListWithPendingContractsAndNotMonopolySupplier(ZonedDateTime date,
                                                                                      List<String> procedureStatus,
                                                                                      List<String> procedureType,
                                                                                      List<String> procuringEntityKind,
                                                                                      Pageable pageable);

    @Query(value = "SELECT\n" +
            "  tenderid,\n" +
            "  lotid,\n" +
            "  split_part(buyersupplies, ',', 1) buyer,\n" +
            "  split_part(buyersupplies, ',', 2) supplier\n" +
            "FROM (\n" +
            "       SELECT\n" +
            "         tenderid,\n" +
            "         lotid,\n" +
            "         (SELECT a\n" +
            "          FROM unnest(buyersupplier) a\n" +
            "          WHERE a IS NOT NULL) buyersupplies\n" +
            "       FROM (\n" +
            "              SELECT\n" +
            "                tenderid,\n" +
            "                lotid,\n" +
            "                array_agg(buyer_supplier) buyersupplier\n" +
            "              FROM (\n" +
            "                     SELECT\n" +
            "                       tender.outer_id                                                   tenderid,\n" +
            "                       lot.outer_id                                                      lotid,\n" +
            "                       tender_contract.outer_id                                          tendercontractid,\n" +
            "                       tender_contract.status                                            tendercontractstatus,\n" +
            "                       CASE WHEN tender_contract.status = 'pending'\n" +
            "                         THEN concat(tender.tv_procuring_entity, ',',\n" +
            "                                     concat(tender_contract.supplier_identifier_scheme, '-',\n" +
            "                                            tender_contract.supplier_identifier_id)) END buyer_supplier\n" +
            "                     FROM tender\n" +
            "                       JOIN lot ON tender.id = lot.tender_id\n" +
            "                       JOIN award ON lot.id = award.lot_id\n" +
            "                       JOIN tender_contract ON award.id = tender_contract.award_id\n" +
            "                     WHERE tender.outer_id =  ANY (SELECT regexp_split_to_table(?1, ',')) " +
            "                   ) a\n" +
            "              GROUP BY tenderid, lotid) b) c", nativeQuery = true)
    List<Object[]> getTenderLotBuyerSupplier(String tenderIds);

    @Query(value = "" +
            "SELECT tenderid,\n" +
            "       lotid,\n" +
            "       unsuccessfulaward,\n" +
            "       split_part(buyersupplies, ',', 1) buyer,\n" +
            "       split_part(buyersupplies, ',', 2) supplier\n" +
            "FROM (SELECT tenderid,\n" +
            "             lotid,\n" +
            "             unsuccessfulaward,\n" +
            "             (SELECT a FROM unnest(buyersupplier) a WHERE a IS NOT NULL) buyersupplies\n" +
            "      FROM (SELECT tenderid,\n" +
            "                   lotid,\n" +
            "                   sum(case when awardstatus = 'unsuccessful' then 1 else 0 end) unsuccessfulaward,\n" +
            "                   array_agg(buyer_supplier)                                     buyersupplier\n" +
            "            FROM (SELECT tender.outer_id                                                           tenderid,\n" +
            "                         lot.outer_id                                                              lotid,\n" +
            "                         tender_contract.outer_id                                                  tendercontractid,\n" +
            "                         tender_contract.status                                                    tendercontractstatus,\n" +
            "                         award.status                                                              awardstatus,\n" +
            "                         CASE\n" +
            "                           WHEN tender_contract.status = 'pending'\n" +
            "                                   THEN concat(tender.tv_procuring_entity, ',',\n" +
            "                                               concat(tender_contract.supplier_identifier_scheme, '-',\n" +
            "                                                      tender_contract.supplier_identifier_id)) END buyer_supplier\n" +
            "                  FROM tender\n" +
            "                         JOIN lot ON tender.id = lot.tender_id\n" +
            "                         JOIN award ON lot.id = award.lot_id\n" +
            "                         JOIN tender_contract ON award.id = tender_contract.award_id\n" +
            "               WHERE tender.outer_id =  ANY (SELECT regexp_split_to_table(?1, ','))\n" +
            "                 ) a\n" +
            "            GROUP BY tenderid, lotid) b) c", nativeQuery = true)
    List<Object[]> getTenderLotBuyerSupplierWithUnsuccessfulAward(String tenderIds);

    @Query(value = "" +
            "SELECT outer_id, tv_procuring_entity, pending_contracts, (SELECT string_agg(DISTINCT a,',')\n" +
            "                                     FROM unnest(bidders) a\n" +
            "                                     WHERE a IS NOT NULL) bidders FROM (\n" +
            "  SELECT\n" +
            "    tender.outer_id,\n" +
            "    tender.tv_procuring_entity,\n" +
            "    sum(CASE WHEN tender_contract.status = 'pending' THEN 1 ELSE 0 END) pending_contracts,\n" +
            "    array_agg(CASE WHEN tender_contract.status = 'pending' THEN" +
            "    concat(tender_contract.supplier_identifier_scheme, '-', tender_contract.supplier_identifier_id) END) bidders\n" +
            "  FROM tender\n" +
            "    LEFT JOIN tender_contract ON tender.id = tender_contract.tender_id\n" +
            "    LEFT JOIN bid ON tender.id = bid.tender_id\n" +
            "  WHERE tender.outer_id =  ANY (SELECT regexp_split_to_table(?1, ',')) " +
            "  GROUP BY tender.outer_id, tender.tv_procuring_entity\n" +
            ") a", nativeQuery = true)
    List<Object[]> getTenderBiddersWithPendingContracts(String tenderIds);

    @Query(value = "SELECT outer_id, start_date, procuring_entity_kind, currency, amount\n" +
            "FROM tender t\n" +
            "WHERE tv_tender_cpv NOT LIKE '45%' AND \n" +
            "      tv_tender_cpv NOT LIKE  '6611%' AND \n" +
            "      title NOT LIKE  '%кредит%' AND \n" +
            "      title NOT LIKE '%гарант%' AND \n" +
            "      title NOT LIKE '%лізинг%' AND \n" +
            "      t.date_created > ?1 AND t.date > now() - INTERVAL '1 year' AND \n" +
            "      procurement_method_type IN ?3 AND\n" +
            "      t.procuring_entity_kind IN ?4 AND status IN ?2 " +
            "ORDER BY t.date_created", nativeQuery = true)
    List<Object[]> findGoodsServicesTenderIdPAKindAndAmountExceptFinances(ZonedDateTime date,
                                                                          List<String> procedureStatus,
                                                                          List<String> procedureType,
                                                                          List<String> procuringEntityKind,
                                                                          Pageable pageable);

    @Query(value = "SELECT outer_id, tv_procuring_entity, procuring_entity_kind,  \n" +
            "  CASE WHEN currency = 'UAH'\n" +
            "    THEN amount\n" +
            "  ELSE\n" +
            "    amount *\n" +
            "    (SELECT rate\n" +
            "     FROM exchange_rate\n" +
            "     WHERE currency = exchange_rate.code AND\n" +
            "           concat(substr(to_char(t.start_date, 'YYYY-MM-DD HH:mm:ss.zzzzzz'), 0, 11),' 00:00:00.000000')\n" +
            "           :::: DATE = exchange_rate.date)\n" +
            "    END amount, tv_subject_of_procurement \n" +
            "FROM tender t\n" +
            "WHERE tv_tender_cpv NOT LIKE '45%'\n" +
            "      AND tv_tender_cpv NOT LIKE  '6611%'\n" +
            "      AND title NOT LIKE  '%кредит%'\n" +
            "      AND title NOT LIKE '%гарант%'\n" +
            "      AND title NOT LIKE '%лізинг%'\n" +
            "      AND t.date_created > ?1\n" +
            "      AND t.date > now() - INTERVAL '1 year'\n" +
            "      AND procurement_method_type IN ?3\n" +
            "      AND t.procuring_entity_kind IN ?4\n" +
            "      AND status IN ?2\n" +
            "ORDER BY t.date_created", nativeQuery = true)
    List<Object[]> findGoodsServicesProcuringEntityKindAmount(ZonedDateTime date,
                                                              List<String> procedureStatus,
                                                              List<String> procedureType,
                                                              List<String> procuringEntityKind,
                                                              Pageable pageable);

    @Query(value = "" +
            "SELECT\n" +
            "  outer_id,\n" +
            "  tv_procuring_entity,\n" +
            "  procuring_entity_kind,\n" +
            "  suppliers,\n" +
            "  CASE WHEN currency = 'UAH'\n" +
            "    THEN amount\n" +
            "  ELSE CASE WHEN rate IS NOT NULL\n" +
            "    THEN amount * rate\n" +
            "       ELSE NULL END END\n" +
            "FROM (\n" +
            "       SELECT\n" +
            "         tender.outer_id,\n" +
            "         string_agg(DISTINCT CASE WHEN award.status = 'active' THEN concat(award.supplier_identifier_scheme, '-', award.supplier_identifier_id) END, ',') suppliers,\n" +
            "         tender.currency,\n" +
            "         tender.amount,\n" +
            "         tv_procuring_entity, procuring_entity_kind ,\n" +
            "         CASE WHEN  tender.currency <> 'UAH' THEN (SELECT rate\n" +
            "                                                   FROM exchange_rate\n" +
            "                                                   WHERE tender.currency = exchange_rate.code AND\n" +
            "                                                         concat(substr(tender.tender_id, 4, 10), ' 00:00:00.000000')\n" +
            "           :::: DATE = exchange_rate.date) ELSE NULL END rate,\n" +
            "         tender.date_created\n" +
            "\n" +
            "       FROM tender\n" +
            "         LEFT JOIN award ON tender.id = award.tender_id\n" +
            "       WHERE  tv_tender_cpv NOT LIKE '45%'\n" +
            "             AND tender.date_created > ?1\n" +
            "             AND tender.date > now() - INTERVAL '2 day'\n" +
            "             AND procurement_method_type IN ?3\n" +
            "             AND tender.procuring_entity_kind IN ?4\n" +
            "             AND tender.status IN ?2\n" +
            "       GROUP BY tender.outer_id, tender.currency,\n" +
            "         tender.amount,\n" +
            "         tv_procuring_entity, procuring_entity_kind,tender.tender_id,tender.date_created\n" +
            "     ) a\n" +
            "ORDER BY date_created", nativeQuery = true)
    List<Object[]> findGoodsServicesProcuringEntityKindAndSupplierAmount(ZonedDateTime date,
                                                                         List<String> procedureStatus,
                                                                         List<String> procedureType,
                                                                         List<String> procuringEntityKind,
                                                                         Pageable pageable);

    @Query(value = "" +
            "SELECT\n" +
            "  outer_id,\n" +
            "  tv_procuring_entity,\n" +
            "  procuring_entity_kind,\n" +
            "  suppliers,\n" +
            "  CASE WHEN currency = 'UAH'\n" +
            "    THEN amount\n" +
            "  ELSE CASE WHEN rate IS NOT NULL\n" +
            "    THEN amount * rate\n" +
            "       ELSE NULL END END\n" +
            "FROM (\n" +
            "       SELECT\n" +
            "         tender.outer_id,\n" +
            "         string_agg(DISTINCT CASE WHEN award.status = 'active' THEN concat(award.supplier_identifier_scheme, '-', award.supplier_identifier_id) END, ',') suppliers,\n" +
            "         tender.currency,\n" +
            "         tender.amount,\n" +
            "         tv_procuring_entity, procuring_entity_kind ,\n" +
            "         CASE WHEN  tender.currency <> 'UAH' THEN (SELECT rate\n" +
            "                                                   FROM exchange_rate\n" +
            "                                                   WHERE tender.currency = exchange_rate.code AND\n" +
            "                                                         concat(substr(tender.tender_id, 4, 10), ' 00:00:00.000000')\n" +
            "           :::: DATE = exchange_rate.date) ELSE NULL END rate,\n" +
            "         tender.date_created\n" +
            "\n" +
            "       FROM tender\n" +
            "         LEFT JOIN award ON tender.id = award.tender_id\n" +
            "       WHERE  tv_tender_cpv LIKE '45%'\n" +
            "             AND tender.date_created > ?1\n" +
            "             AND tender.date > now() - INTERVAL '2 day'\n" +
            "             AND procurement_method_type IN ?3\n" +
            "             AND tender.procuring_entity_kind IN ?4\n" +
            "             AND tender.status IN ?2\n" +
            "       GROUP BY tender.outer_id, tender.currency,\n" +
            "         tender.amount,\n" +
            "         tv_procuring_entity, procuring_entity_kind,tender.tender_id,tender.date_created\n" +
            "     ) a\n" +
            "ORDER BY date_created", nativeQuery = true)
    List<Object[]> findWorksProcuringEntityKindAndSupplierAmount(ZonedDateTime date,
                                                                 List<String> procedureStatus,
                                                                 List<String> procedureType,
                                                                 List<String> procuringEntityKind,
                                                                 Pageable pageable);

    @Query(value = "" +
            "SELECT\n" +
            "  outer_id,\n" +
            "  pending_contracts_count, " +
            "  tv_procuring_entity, " +
            "  procuring_entity_kind, \n" +
            "  suppliers,\n" +
            "  CASE WHEN currency = 'UAH'\n" +
            "    THEN amount\n" +
            "  ELSE CASE WHEN rate IS NOT NULL\n" +
            "    THEN amount * rate\n" +
            "       ELSE NULL END END\n" +
            "FROM (\n" +
            "       SELECT\n" +
            "         tender.outer_id,\n" +
            "         count(DISTINCT (CASE WHEN tender_contract.status = 'pending'\n" +
            "           THEN tender_contract.outer_id END)) pending_contracts_count,\n" +
            "         string_agg(DISTINCT CASE WHEN award.status = 'active' THEN concat(award.supplier_identifier_scheme, '-', award.supplier_identifier_id) END, ',') suppliers,\n" +
            "         tender.currency,\n" +
            "         tender.amount,\n" +
            "         tv_procuring_entity, procuring_entity_kind ,  " +
            "         CASE WHEN  tender.currency <> 'UAH' THEN (SELECT rate\n" +
            "          FROM exchange_rate\n" +
            "          WHERE tender.currency = exchange_rate.code AND\n" +
            "                concat(substr(to_char(tender.start_date, 'YYYY-MM-DD HH:mm:ss.zzzzzz'), 0, 11), ' 00:00:00.000000')\n" +
            "                :::: DATE = exchange_rate.date) ELSE NULL END rate,\n" +
            "         tender.date_created\n" +
            "\n" +
            "       FROM tender\n" +
            "         LEFT JOIN tender_contract ON tender.id = tender_contract.tender_id\n" +
            "         LEFT JOIN award ON tender.id = award.tender_id\n" +
            "       WHERE  tv_tender_cpv NOT LIKE '45%' \n" +
            "             AND tender.date_created > ?1\n" +
            "             AND tender.date > now() - INTERVAL '1 year'\n" +
            "             AND procurement_method_type IN ?3\n" +
            "             AND tender.procuring_entity_kind IN ?4\n" +
            "             AND tender.status IN ?2\n" +
            "       GROUP BY tender.outer_id, tender.date_created, tender.currency, tender.amount, tender.start_date, tv_procuring_entity, procuring_entity_kind \n" +
            "     ) a\n" +
            "ORDER BY date_created", nativeQuery = true)
    List<Object[]> findGoodsServicesPendingContractsCountProcuringEntityKindAndSupplierAmount(ZonedDateTime date,
                                                                                              List<String> procedureStatus,
                                                                                              List<String> procedureType,
                                                                                              List<String> procuringEntityKind,
                                                                                              Pageable pageable);

    @Query(value = "" +
            "SELECT\n" +
            "  outer_id,\n" +
            "  pending_contracts_count, " +
            "  tv_procuring_entity, " +
            "  procuring_entity_kind, \n" +
            "  suppliers,\n" +
            "  CASE WHEN currency = 'UAH'\n" +
            "    THEN amount\n" +
            "  ELSE CASE WHEN rate IS NOT NULL\n" +
            "    THEN amount * rate\n" +
            "       ELSE NULL END END\n" +
            "FROM (\n" +
            "       SELECT\n" +
            "         tender.outer_id,\n" +
            "         count(DISTINCT (CASE WHEN tender_contract.status = 'pending'\n" +
            "           THEN tender_contract.outer_id END)) pending_contracts_count,\n" +
            "         string_agg(DISTINCT CASE WHEN award.status = 'active' THEN concat(award.supplier_identifier_scheme, '-', award.supplier_identifier_id) END, ',') suppliers,\n" +
            "         tender.currency,\n" +
            "         tender.amount,\n" +
            "         tv_procuring_entity, procuring_entity_kind ,  " +
            "         CASE WHEN  tender.currency <> 'UAH' THEN (SELECT rate\n" +
            "          FROM exchange_rate\n" +
            "          WHERE tender.currency = exchange_rate.code AND\n" +
            "                concat(substr(to_char(tender.start_date, 'YYYY-MM-DD HH:mm:ss.zzzzzz'), 0, 11), ' 00:00:00.000000')\n" +
            "                :::: DATE = exchange_rate.date) ELSE NULL END rate,\n" +
            "         tender.date_created\n" +
            "\n" +
            "       FROM tender\n" +
            "         LEFT JOIN tender_contract ON tender.id = tender_contract.tender_id\n" +
            "         LEFT JOIN award ON tender.id = award.tender_id\n" +
            "       WHERE  tv_tender_cpv LIKE '45%' \n" +
            "             AND tender.date_created > ?1\n" +
            "             AND tender.date > now() - INTERVAL '1 year'\n" +
            "             AND procurement_method_type IN ?3\n" +
            "             AND tender.procuring_entity_kind IN ?4\n" +
            "             AND tender.status IN ?2\n" +
            "       GROUP BY tender.outer_id, tender.date_created, tender.currency, tender.amount, tender.start_date, tv_procuring_entity, procuring_entity_kind \n" +
            "     ) a\n" +
            "ORDER BY date_created", nativeQuery = true)
    List<Object[]> findWorksPendingContractsCountProcuringEntityKindAndSupplierAmount(ZonedDateTime date,
                                                                                      List<String> procedureStatus,
                                                                                      List<String> procedureType,
                                                                                      List<String> procuringEntityKind,
                                                                                      Pageable pageable);

    @Query(value = "SELECT\n" +
            "  outer_id,\n" +
            "  tv_procuring_entity,\n" +
            "  procuring_entity_kind,\n" +
            "  (CASE WHEN currency = 'UAH'\n" +
            "    THEN amount\n" +
            "   ELSE CASE WHEN rate IS NOT NULL\n" +
            "     THEN amount * rate\n" +
            "        ELSE NULL END END), " +
            "  tv_subject_of_procurement\n" +
            "FROM (\n" +
            "       SELECT\n" +
            "         date_created,\n" +
            "         outer_id,\n" +
            "         tv_procuring_entity,\n" +
            "         procuring_entity_kind,\n" +
            "         currency,\n" +
            "         amount,\n" +
            "         start_date," +
            "         tv_subject_of_procurement,\n" +
            "         (SELECT rate\n" +
            "          FROM exchange_rate\n" +
            "          WHERE currency = exchange_rate.code\n" +
            "                AND concat(substr(to_char(t.start_date, 'YYYY-MM-DD HH:mm:ss.zzzzzz'), 0, 11),\n" +
            "                           ' 00:00:00.000000') :::: DATE = exchange_rate.date) rate \n" +
            "       FROM tender t \n" +
            "       WHERE tv_tender_cpv LIKE '45%'\n" +
            "                  AND t.date_created > ?1\n" +
            "                  AND t.date > now() - INTERVAL '1 year'\n" +
            "                  AND procurement_method_type IN ?3\n" +
            "                  AND t.procuring_entity_kind IN ?4\n" +
            "                  AND status IN ?2\n" +
            "     ) a\n" +
            "ORDER BY date_created", nativeQuery = true)
    List<Object[]> findWorksProcuringEntityKindAmount(ZonedDateTime date,
                                                      List<String> procedureStatus,
                                                      List<String> procedureType,
                                                      List<String> procuringEntityKind,
                                                      Pageable pageable);


    @Query(value = "" +
            "SELECT tender.outer_id tender_id, contract.outer_id contract_id, contract.date_signed," +
            " min(case when contract_change.status = 'active' THEN contract_change.date_signed ELSE null END ), " +
            " count(DISTINCT contract_change.id) contractChanges \n" +
            "FROM tender\n" +
            "  JOIN tender_contract ON tender.id = tender_contract.tender_id\n" +
            "  JOIN contract ON tender_contract.id = contract.tender_contract_id\n" +
            "  LEFT JOIN contract_change ON contract.id = contract_change.contract_id\n" +
            "WHERE  contract.date_created > ?1\n" +
            "      AND tender.date > now() - INTERVAL '36 day'\n" +
            "      AND tender.procurement_method_type IN ?3\n" +
            "      AND tender.procuring_entity_kind IN ?4\n" +
            "      AND tender.status IN ?2\n" +
            "GROUP BY tender.outer_id, contract.outer_id, contract.date_signed, contract.date_created\n" +
            "ORDER BY contract.date_created", nativeQuery = true)
    List<Object[]> findTenderWithContractDateSignedAndMinChangeDateSigned(ZonedDateTime date,
                                                                          List<String> procedureStatus,
                                                                          List<String> procedureType,
                                                                          List<String> procuringEntityKind,
                                                                          Pageable pageable);

    @Query(value = "SELECT outer_id, start_date, procuring_entity_kind, currency, amount\n" +
            "FROM tender t\n " +
            "WHERE tv_tender_cpv LIKE '45%' AND \n" +
            "      t.date_created > ?1 AND t.date > now() - INTERVAL '1 year' AND \n" +
            "      procurement_method_type IN ?3 AND\n" +
            "      t.procuring_entity_kind IN ?4 AND status IN ?2 " +
            "ORDER BY t.date_created", nativeQuery = true)
    List<Object[]> findWorkTenderIdPAKindAndAmount(ZonedDateTime date,
                                                   List<String> procedureStatus,
                                                   List<String> procedureType,
                                                   List<String> procuringEntityKind,
                                                   Pageable pageable);


    @Transactional
    @Query(value = "SELECT t.outer_id FROM tender t  " +
            "WHERE t.status IN ?2 " +
            "   AND t.date_created > ?1 AND t.date > now() - INTERVAL '1 year' " +
            "   AND t.procuring_entity_kind IN ?3 ", nativeQuery = true)
    List<String> findTenderIdByProcedureStatus(ZonedDateTime date,
                                               List<String> procedureStatus,
                                               List<String> procuringEntityKind,
                                               Pageable pageable);

    @Transactional
    @Query(value = "SELECT t.outer_id FROM tender t " +
            "WHERE t.procurement_method_type IN ?2 " +
            "AND t.date_created >  ?1 AND t.date > now() - INTERVAL '1 year' " +
            "AND t.procuring_entity_kind IN ?3 ", nativeQuery = true)
    List<String> findTenderIdByProcedureType(ZonedDateTime date,
                                             List<String> procedureType,
                                             List<String> procuringEntityKind,
                                             Pageable pageable);

    @Transactional
    @Query(value = "" +
            "SELECT tender.outer_id, " +
            "   concat(buyer.scheme, buyer.outer_id), " +
            "   lot.outer_id, " +
            "   string_agg(concat(supplier.identifier_scheme, supplier.identifier_id), ',') FROM tender " +
            "JOIN buyer ON tender.buyer_id = buyer.id " +
            "JOIN lot ON tender.id = lot.tender_id " +
            "JOIN award ON tender.id = award.tender_id " +
            "JOIN supplier ON award.supplier_id = supplier.id  " +
            "WHERE award.status IN ('pending', 'active') " +
            "   AND tender.date_created > ?1 AND tender.date > now() - INTERVAL '1 year' " +
            "GROUP BY tender.outer_id, lot.outer_id", nativeQuery = true)
    List<Object> getTenderBuyerLotsSuppliers(ZonedDateTime date, Pageable pageable);

    @Query(value = "" +
            "SELECT\n" +
            "  t.outer_id,\n" +
            "  t.tv_procuring_entity,\n" +
            "  count(DISTINCT CASE WHEN tc.status = 'pending'\n" +
            "    THEN tc.id END),\n" +
            "  array_to_string(t.tv_tender_cpv_list, ',') cpv_list,\n" +
            "  CASE WHEN t.cause = 'twiceUnsuccessful'\n" +
            "    THEN TRUE\n" +
            "  ELSE FALSE END                             twiceunsuccessful\n" +
            "FROM tender t\n" +
            "  LEFT JOIN tender_contract tc ON t.id = tc.tender_id\n" +
            "WHERE t.outer_id = ANY (SELECT regexp_split_to_table(?1, ','))\n" +
            "GROUP BY t.outer_id, t.tv_procuring_entity, cpv_list, t.date_created, t.cause\n" +
            "ORDER BY date_created", nativeQuery = true)
    List<Object[]> findTenderIdPAndProcuringEntityAndCPVListWithPendingContract(String tenderIds);

    @Query(value = "" +
            "SELECT tender.outer_id,\n" +
            "       tender_item.classification_id,\n" +
            "       tender.tv_procuring_entity,\n" +
            "       tender.amount,\n" +
            "       sum(tender_item.quantity),\n" +
            "       count(distinct case when tender_contract.status = 'pending' then tender_contract.id end)\n" +
            "FROM unsuccessful_above\n" +
            "       JOIN tender ON tender.tv_procuring_entity = unsuccessful_above.procuring_entity\n" +
            "                             AND unsuccessful_above.tender_cpv = ANY (tender.tv_tender_cpv_list)\n" +
            "       LEFT JOIN tender_contract ON tender.id = tender_contract.tender_id\n" +
            "       JOIN tender_item\n" +
            "         ON tender.id = tender_item.tender_id AND unsuccessful_above.tender_cpv = tender_item.classification_id\n" +
            "WHERE tender.date_created > ?1\n" +
            "  AND tender.date > now() - INTERVAL '1 year'\n" +
            "  AND tender.status IN ?2\n" +
            "  AND tender.procurement_method_type IN ?3\n" +
            "  AND tender.procuring_entity_kind IN ?4\n" +
            "  AND tender.cause = 'twiceUnsuccessful'\n" +
            "  AND unsuccessful_above.unsuccessful_above_procedures_count = 2\n" +
            "GROUP BY tender.outer_id, tender.date_created, tender_item.classification_id, " +
            " tender.tv_procuring_entity, tender.amount\n" +
            "order by tender.date_created", nativeQuery = true)
    List<Object[]> findTenderIdPWithPendingContractAndTwiceUnsuccessful(ZonedDateTime date,
                                                                        List<String> procedureStatus,
                                                                        List<String> procedureType,
                                                                        List<String> procuringEntityKind,
                                                                        Pageable pageable);

    @Query(value = "" +
            "SELECT tender.outer_id, \n" +
            "  count(DISTINCT (CASE WHEN lot.status = 'active' THEN lot.id ELSE NULL END)) lot_count, \n" +
            "  count(DISTINCT (CASE WHEN award.status = 'active' THEN supplier_id ELSE NULL END)) supplier_count \n" +
            "FROM tender\n" +
            "  JOIN lot ON tender.id = lot.tender_id\n" +
            "  JOIN award ON lot.id = award.lot_id\n" +
            "  JOIN supplier ON award.supplier_id = supplier.id\n" +
            "WHERE tender.outer_id = ANY (SELECT regexp_split_to_table(?1, ',')) " +
            "GROUP BY tender.outer_id", nativeQuery = true)
    List<Object[]> getDistinctLotAndSupplierCount(String tenderIds);

    @Query(value = "" +
            "SELECT tender.outer_id,\n" +
            "       count(DISTINCT  (CASE WHEN award.status = 'active' THEN lot.id else null end ))  lot_count,\n" +
            "       count(DISTINCT (CASE WHEN award.status = 'unsuccessful' THEN supplier_id ELSE NULL END)) unseccessfull_supplier_count,\n" +
            "       count(DISTINCT (CASE WHEN award.status = 'active' THEN supplier_id ELSE NULL END)) supplier_count\n" +
            "FROM tender\n" +
            "       JOIN lot ON tender.id = lot.tender_id\n" +
            "       JOIN award ON lot.id = award.lot_id\n" +
            "       JOIN supplier ON award.supplier_id = supplier.id\n" +
            "WHERE tender.outer_id = ANY (SELECT regexp_split_to_table(?1, ','))\n" +
            "GROUP BY tender.outer_id", nativeQuery = true)
    List<Object[]> getDistinctLotUnsuccessfulAndAwardsAndSupplierCount(String tenderIds);

    @Query(value = "SELECT t.outer_id, " +
            "t.currency, " +
            "t.amount, " +
            "CASE WHEN t.start_date < t.enquiry_start_date THEN t.start_date ELSE t.enquiry_start_date END,  " +
            "t.date_created " +
            "FROM tender t " +
            "WHERE t.date_created > ?1 AND t.date > now() - INTERVAL '1 year' " +
            "AND t.status IN ?2 " +
            "AND t.procurement_method_type IN ?3 " +
            "AND t.procuring_entity_kind IN ?4 " +
            "AND t.tv_subject_of_procurement LIKE '45%'" +
            "AND (t.start_date <= current_date OR t.enquiry_start_date <= current_date) " +
            "ORDER BY t.date_created", nativeQuery = true)
    List<Object[]> findWorkTenderIdCurrencyAmountByProcedureStatusAndProcedureType(ZonedDateTime date,
                                                                                   List<String> procedureStatus,
                                                                                   List<String> procedureType,
                                                                                   List<String> procuringEntityKind,
                                                                                   Pageable pageable);

    @Query(value = "SELECT t.outer_id, " +
            "t.currency, " +
            "t.amount, " +
            "CASE WHEN t.start_date < t.enquiry_start_date THEN t.start_date ELSE t.enquiry_start_date END, " +
            "t.date_created " +
            "FROM tender t " +
            "WHERE t.date_created > ?1 AND t.date > now() - INTERVAL '1 year' " +
            "AND t.status IN ?2 " +
            "AND t.procurement_method_type IN ?3 " +
            "AND t.procuring_entity_kind IN ?4 " +
            "AND t.tv_subject_of_procurement NOT LIKE '45%' " +
            "AND (t.start_date::::timestamp::::date <= current_date OR t.enquiry_start_date::::timestamp::::date <= current_date) " +
            "ORDER BY t.date_created", nativeQuery = true)
    List<Object[]> findGoodsServicesTenderIdCurrencyAmountByProcedureStatusAndProcedureType(ZonedDateTime date,
                                                                                            List<String> procedureStatus,
                                                                                            List<String> procedureType,
                                                                                            List<String> procuringEntityKind, Pageable pageable);

    @Query(value =
            "SELECT" +
                    "  tender.outer_id," +
                    "  tender.tender_id," +
                    "  tender.amount," +
                    "  tender.tv_procuring_entity, " +
                    "  procuring_entity.region " +
                    "FROM tender " +
                    "  JOIN procuring_entity ON tender.procuring_entity_id = procuring_entity.id " +
                    "WHERE tender.outer_id = ANY (SELECT regexp_split_to_table(?1, ','))\n", nativeQuery = true)
    List<Object> findAllTendersWithPERegionByOuterIdIn(String outerIds);

    @Query(value = "" +
            "SELECT tenderouterid,\n" +
            "       lot_outer_id,\n" +
            "       complaints_count,\n" +
            "       CASE\n" +
            "         WHEN award_status = 'active'\n" +
            "                 THEN award_date\n" +
            "         ELSE NULL END                                         active_award_date,\n" +
            "       tender_contract.status,\n" +
            "       count(DISTINCT CASE\n" +
            "                        WHEN (document.format <> 'application/pkcs7-signature' or document.format is null)\n" +
            "                                THEN document.id END)          tender_docs,\n" +
            "       count(distinct case\n" +
            "                        when contract_document.document_of = 'contract' and\n" +
            "                             (contract_document.format <> 'application/pkcs7-signature' or document.format is null)\n" +
            "                                then contract_document.id end) contract_docs,\n" +
            "       lotstatus\n" +
            "FROM (SELECT tenderouterid,\n" +
            "             lot_outer_id,\n" +
            "             complaints_count,\n" +
            "             lotstatus,\n" +
            "             award.id     award_id,\n" +
            "             award.date   award_date,\n" +
            "             award.status award_status\n" +
            "      FROM (SELECT tender.outer_id              tenderouterid,\n" +
            "                   lot.outer_id                 lot_outer_id,\n" +
            "                   lot.id                       lotid,\n" +
            "                   lot.status                   lotstatus,\n" +
            "                   count(DISTINCT complaint.id) complaints_count\n" +
            "            FROM tender\n" +
            "                   JOIN lot ON tender.id = lot.tender_id\n" +
            "                   JOIN award ON lot.id = award.lot_id\n" +
            "                   LEFT JOIN complaint ON award.id = complaint.award_id\n" +
            "                      WHERE tender.outer_id = ANY (SELECT regexp_split_to_table(?1, ','))\n" +
            "            GROUP BY tender.outer_id, lot.outer_id, lot.id) a\n" +
            "             LEFT JOIN award ON lotid = award.lot_id) b\n" +
            "       LEFT JOIN tender_contract ON b.award_id = tender_contract.award_id\n" +
            "       LEFT JOIN document ON tender_contract.id = document.contract_id\n" +
            "       LEFT JOIN contract on tender_contract.id = contract.tender_contract_id\n" +
            "       left join contract_document on contract.id = contract_document.contract_id\n" +
            "GROUP BY tenderouterid,\n" +
            "         lot_outer_id,\n" +
            "         complaints_count, active_award_date, tender_contract.status, lotstatus", nativeQuery = true)
    List<Object[]> getTenderLotsAndComplaintsCountAndActiveAwardDateAndContractStatus(String tenderIds);

    @Query(value = "SELECT\n" +
            "  tenderid,\n" +
            "  lotid,\n" +
            "  awardid,\n" +
            "  award.date,\n" +
            "  sum(CASE WHEN (document.format <> 'application/pkcs7-signature' or document.format is null) AND\n" +
            "                (document.author IS NULL OR document.author <> 'bots')\n" +
            "    THEN 1\n" +
            "      ELSE 0 END) doccount,\n" +
            "  count(DISTINCT complaint.id) complaintscount\n" +
            "FROM (\n" +
            "       SELECT\n" +
            "         tenderid,\n" +
            "         lotid,\n" +
            "         unnest(awards) awardid\n" +
            "       FROM (\n" +
            "              SELECT\n" +
            "                tenderid,\n" +
            "                lotid,\n" +
            "                CASE WHEN awards IS NULL\n" +
            "                  THEN '{null}'\n" +
            "                ELSE awards END\n" +
            "              FROM (\n" +
            "                     SELECT\n" +
            "                       tenderid,\n" +
            "                       lotid,\n" +
            "                       (SELECT array_agg(a)\n" +
            "                        FROM unnest(awardids) a\n" +
            "                        WHERE a IS NOT NULL) awards\n" +
            "                     FROM (\n" +
            "                            SELECT\n" +
            "                              tender.outer_id          tenderid,\n" +
            "                              lot.outer_id             lotid,\n" +
            "                              array_agg(\n" +
            "                                  CASE WHEN award.status = 'pending'\n" +
            "                                    THEN award.id END) awardids\n" +
            "                            FROM tender\n" +
            "                              JOIN lot ON tender.id = lot.tender_id\n" +
            "                              JOIN award ON lot.id = award.lot_id\n" +
            "                              LEFT JOIN complaint ON award.id = complaint.award_id\n" +
            "                              WHERE tender.outer_id= ANY (SELECT regexp_split_to_table(?1, ','))\n" +
            "                            GROUP BY tender.outer_id, lot.outer_id\n" +
            "                          ) a) b) c\n" +
            "     ) d\n" +
            "  LEFT JOIN award ON awardid = award.id\n" +
            "  LEFT JOIN document ON award.id = document.award_id\n" +
            "  LEFT JOIN complaint ON award.id = complaint.award_id\n" +
            "GROUP BY tenderid,\n" +
            "  lotid,\n" +
            "  awardid,\n" +
            "  award.date", nativeQuery = true)
    List<Object[]> getTenderLotDateNoSignatureDocComplaintCountByTenderIds(String tenderIds);


    @Query(value = "SELECT\n" +
            "  tenderid,\n" +
            "  lotid,\n" +
            "  awardid,\n" +
            "  award.date,\n" +
            "  sum(CASE WHEN document.format NOT IN ('application/pkcs7-signature','application/yaml') \n" +
            "    THEN 1\n" +
            "      ELSE 0 END) doccount\n " +
            "FROM (\n" +
            "       SELECT\n" +
            "         tenderid,\n" +
            "         lotid,\n" +
            "         unnest(awards) awardid\n" +
            "       FROM (\n" +
            "              SELECT\n" +
            "                tenderid,\n" +
            "                lotid,\n" +
            "                CASE WHEN awards IS NULL\n" +
            "                  THEN '{null}'\n" +
            "                ELSE awards END\n" +
            "              FROM (\n" +
            "                     SELECT\n" +
            "                       tenderid,\n" +
            "                       lotid,\n" +
            "                       (SELECT array_agg(a)\n" +
            "                        FROM unnest(awardids) a\n" +
            "                        WHERE a IS NOT NULL) awards\n" +
            "                     FROM (\n" +
            "                            SELECT\n" +
            "                              tender.outer_id          tenderid,\n" +
            "                              lot.outer_id             lotid,\n" +
            "                              array_agg(\n" +
            "                                  CASE WHEN award.status = 'pending'\n" +
            "                                    THEN award.id END) awardids\n" +
            "                            FROM tender\n" +
            "                              JOIN lot ON tender.id = lot.tender_id\n" +
            "                              JOIN award ON lot.id = award.lot_id\n" +
            "                              LEFT JOIN complaint ON award.id = complaint.award_id\n" +
            "                              WHERE tender.outer_id= ANY (SELECT regexp_split_to_table(?1, ','))\n" +
            "                            GROUP BY tender.outer_id, lot.outer_id\n" +
            "                          ) a) b) c\n" +
            "     ) d\n" +
            "  LEFT JOIN award ON awardid = award.id\n" +
            "  LEFT JOIN document ON award.id = document.award_id\n" +
            "GROUP BY tenderid,\n" +
            "  lotid,\n" +
            "  awardid,\n" +
            "  award.date", nativeQuery = true)
    List<Object[]> getTenderLotDateNoSignatureNoYamlDocComplaintCountByTenderIds(String tenderIds);

    @Query(value = "" +
            "SELECT\n" +
            "  tender_id,\n" +
            "  lot_id,\n" +
            "  (SELECT a :::: INTEGER DAYS\n" +
            "   FROM unnest(DAYS) a\n" +
            "   WHERE a IS NOT NULL)\n" +
            "FROM (\n" +
            "       SELECT\n" +
            "         tender_id,\n" +
            "         lot_id,\n" +
            "         array_agg(tender_contract_status),\n" +
            "         array_agg(DAYS) DAYS\n" +
            "       FROM (\n" +
            "              SELECT\n" +
            "                tender_id,\n" +
            "                lot_id,\n" +
            "                tender_contract_status,\n" +
            "                aword_date , min_date_modified,\n" +
            "                case when aword_date is not NULL  AND  min_date_modified is not NULL " +
            " THEN abs(date_part('day', aword_date::::TIMESTAMP - min_date_modified::::TIMESTAMP)) else null END DAYS\n" +
            "              FROM (\n" +
            "                     SELECT\n" +
            "                       tender.outer_id                    tender_id,\n" +
            "                       lot.outer_id                       lot_id,\n" +
            "                       tender_contract.status             tender_contract_status,\n" +
            "                       CASE WHEN tender_contract.status = 'active'\n" +
            "                         THEN (award.date::::timestamp with time zone at time zone 'Europe/Kiev')::::date\n" +
            "                       ELSE NULL END                      aword_date,\n" +
            "                       (MIN(CASE WHEN (document.format <> 'application/pkcs7-signature' or document.format is null)\n" +
            "                         THEN document.date_modified END)::::timestamp with time zone at time zone 'Europe/Kiev')::::date min_date_modified\n" +
            "                     FROM tender\n" +
            "                       JOIN lot ON tender.id = lot.tender_id\n" +
            "                       JOIN award ON lot.id = award.lot_id\n" +
            "                       JOIN tender_contract ON award.id = tender_contract.award_id\n" +
            "                       LEFT JOIN document ON tender_contract.id = document.contract_id\n" +
            "                     WHERE tender.outer_id = ANY (SELECT regexp_split_to_table(?1, ','))\n" +
            "                     GROUP BY tender.outer_id, lot.outer_id, tender_contract.status, award.date\n" +
            "                   ) a) b\n" +
            "       GROUP BY tender_id, lot_id) C", nativeQuery = true)
    List<Object[]> getTenderWithLotAndDaysBetweenAwardDateAndDocumentDateModified(String tenderIds);

    @Query(value = "SELECT\n" +
            "  tender.outer_id,\n" +
            "  min(CASE WHEN award.status = 'active' THEN award.date ELSE NULL END) min_active_award_date,\n" +
            "  min(CASE WHEN (document.author is null or document.author <> 'auction') " +
            " AND (document.format is null or document.format <>'application/pkcs7-signature')" +
            " THEN  document.date_published ELSE NULL END )\n" +
            "FROM tender\n" +
            "  JOIN award ON tender.id = award.tender_id\n" +
            "  JOIN document ON tender.id = document.tender_id\n" +
            " WHERE tender.outer_id =  ANY (SELECT regexp_split_to_table(?1, ',')) " +
            "GROUP BY tender.outer_id;\n", nativeQuery = true)
    List<Object[]> getTenderIdMinAwardDateMinDocumentDatePublished(String tenderIds);

    @Query(value = "SELECT\n" +
            "  tender.outer_id,\n" +
            "  tender.award_start_date,\n" +
            "  min(CASE WHEN ( document.author is null or document.author <> 'auction') " +
            " AND (document.author is null or document.format <> 'application/pkcs7-signature') \n" +
            "    THEN document.date_published\n" +
            "      ELSE NULL END)\n" +
            "FROM tender\n" +
            " LEFT JOIN document ON tender.id = document.tender_id\n" +
            " WHERE tender.outer_id =  ANY (SELECT regexp_split_to_table(?1, ',')) " +
            "GROUP BY tender.outer_id, tender.award_start_date", nativeQuery = true)
    List<Object[]> getTenderWithStartAwardDateAndMinDocDatePublished(String tenderIds);

    @Query(value = "SELECT\n" +
            "  tender.outer_id tender_outer_id, lot.outer_id, sum( CASE  when qualification.status ='unsuccessful' THEN 1 ELSE 0 END )\n" +
            "FROM lot\n" +
            "  LEFT JOIN qualification ON lot.id = qualification.lot_id\n" +
            "  join tender on lot.tender_id = tender.id\n" +
            " WHERE tender.outer_id =  ANY (SELECT regexp_split_to_table(?1, ',')) " +
            "GROUP BY tender.outer_id, lot.outer_id", nativeQuery = true)
    List<Object[]> findTenderLotWithUnsuccessfulQualificationsCountByTenderId(String tenderIds);

    @Query(value = "" +
            "SELECT\n" +
            "  tender.outer_id,\n" +
            "  count(DISTINCT CASE WHEN tender_contract.status = 'pending'\n" +
            "    THEN tender_contract.outer_id\n" +
            "                 ELSE NULL END),\n" +
            "  string_agg(DISTINCT (CASE WHEN award.status = 'active'\n" +
            "    THEN concat(award.supplier_identifier_scheme, '-', award.supplier_identifier_id) END), ','),\n" +
            "  CASE WHEN tender.currency = 'UAH'\n" +
            "    THEN tender.amount\n" +
            "  ELSE\n" +
            "    tender.amount *\n" +
            "    (SELECT rate\n" +
            "     FROM exchange_rate\n" +
            "     WHERE currency = exchange_rate.code AND\n" +
            "           concat(substr(to_char(tender.start_date, 'YYYY-MM-DD HH:mm:ss.zzzzzz'), 0, 11), ' 00:00:00.000000') :::: DATE =\n" +
            "           exchange_rate.date)\n" +
            "  END result_amount,\n" +
            "  tender.tv_procuring_entity,\n" +
            "  tender.procuring_entity_kind\n" +
            "FROM tender\n" +
            "  LEFT JOIN tender_contract ON tender.id = tender_contract.tender_id\n" +
            "  LEFT JOIN award ON tender.id = award.tender_id\n" +
            "WHERE tender.outer_id = ANY (SELECT regexp_split_to_table(?1, ','))\n" +
            "GROUP BY tender.outer_id, tender.currency, tender.amount, tender.start_date,\n" +
            "tender.tv_procuring_entity,\n" +
            "tender.procuring_entity_kind\n", nativeQuery = true)
    List<Object[]> getTenderIdPendingContractsCountProcuringEnrityIdKindSupplierAndAmount(String tenderIds);


    @Transactional
    @Modifying
    @Query(value =
            "WITH tender_subquery AS (\n" +
                    "    SELECT\n" +
                    "      t.id,\n" +
                    "      cast(tender_data.data AS JSON) -> 'data' ->> 'title' AS title, \n" +
                    "      cast(tender_data.data AS JSON) -> 'data' -> 'procuringEntity'->>'kind' AS procuring_entity_kind\n" +
                    "    FROM tender_data\n" +
                    "      JOIN tender t ON tender_data.tender_id = t.id\n" +
                    "    WHERE t.id >= ?1 AND t.id <= ?2 " +
                    ")\n" +
                    "UPDATE tender\n" +
                    "SET title = tender_subquery.title, procuring_entity_kind= tender_subquery.procuring_entity_kind FROM tender_subquery\n" +
                    "WHERE tender.id = tender_subquery.id",
            nativeQuery = true)
    void updateTitleFromTenderData(Long minId, Long maxId);

    @Query(value = "SELECT outer_id, status from Tender where tender.outer_id in ?1", nativeQuery = true)
    List<Object[]> findTenderOuterIdAndStatusByTendersOuterIdIn(List<String> outerIds);


    @Query(value = "select tender.outer_id,\n" +
            "       tender.tender_id,\n" +
            "       tender.status,\n" +
            "       tender.procurement_method_type,\n" +
            "       tender.amount,\n" +
            "       tender.currency,\n" +
            "       tender.tv_tender_cpv,\n" +
            "       cpv.name,\n" +
            "       cpv2.cpv  cpv2,\n" +
            "       cpv2.name cpv2_name,\n" +
            "       tender.tv_procuring_entity,\n" +
            "       tender.procuring_entity_kind,\n" +
            "       procuring_entity.identifier_legal_name,\n" +
            "       indicators_queue_region.correct_name,\n" +
            "       (select count(*) > 0\n" +
            "        from (select * from award\n" +
            "                              join complaint c2 on award.id = c2.award_id where award.tender_id = tender.id and c2.complaint_type = 'complaint') a), \n" +
            "       tender.title,\n" +
            "       (SELECT COUNT(*) > 0 FROM complaint WHERE tender_id = tender.id AND complaint_type = 'complaint')\n" +
            "from tender\n" +
            "       left join procuring_entity on tender.procuring_entity_id = procuring_entity.id\n" +
            "       left join cpv_catalogue cpv on tender.tv_tender_cpv = cpv.cpv\n" +
            "       left join cpv_catalogue cpv2 on cpv.cpv2 = cpv2.cpv\n" +
            "       left join indicators_queue_region on procuring_entity.region = indicators_queue_region.original_name where tender.outer_id = ANY (SELECT regexp_split_to_table(?1, ','))", nativeQuery = true)
    List<Object[]> getTendersCommonInfo(String tenderIds);

    @Query(value = "SELECT t.*\n" +
            "FROM tender t\n" +
            "JOIN procuring_entity pe on t.procuring_entity_id = pe.id\n" +
            "WHERE t.status IN ('active.awarded', 'active.qualification', 'complete')\n" +
            "AND t.procurement_method_type IN ('aboveThresholdEU','aboveThresholdUA')\n" +
            "AND pe.kind IN ('general', 'special')\n" +
            "AND t.date_created > ?1\n" +
            "ORDER BY t.date_created ", nativeQuery = true)
    Page<Tender> getRisk1_8_2Tenders(ZonedDateTime date, Pageable pageable);
}