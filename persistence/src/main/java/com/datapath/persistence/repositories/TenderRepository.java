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

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface TenderRepository extends PagingAndSortingRepository<Tender, Long>, JpaRepository<Tender, Long> {

    Tender findFirstBySourceAndDateModifiedIsNotNullOrderByDateModifiedDesc(String source);

    Tender findFirstByOuterId(String outerId);

    Long countAllByTitleIsNull();

    Long countAllByProcuringEntityKindIsNull();

    @Query(value = "select max(t.dateModified) from Tender t")
    ZonedDateTime findMaxDateModified();

    @Query(value = "select max(t.dateCreated) from Tender t")
    ZonedDateTime findMaxDateCreated();

    void deleteAllByDateBefore(ZonedDateTime date);

    @Modifying
    @Transactional
    long deleteAllByOuterId(String outerId);

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

    @Query(value = "SELECT t.outer_id, amount, tender_id FROM tender t " +
            "WHERE t.status NOT IN ?1 " +
            "AND t.outer_id = ANY (SELECT regexp_split_to_table(?2, ',')) " +
            "AND amount IS NOT NULL ORDER BY amount", nativeQuery = true)
    List<Object> findTendersWithAmountByTendersExcludingStatus(List<String> statuses, String tenderIds);

    @Query(value = "" +
            "SELECT tenderid,\n" +
            "       lotid,\n" +
            "       award_date,\n" +
            "       CASE\n" +
            "         WHEN procurement_method_type = 'aboveThresholdUA' THEN min_bid_doc_date\n" +
            "         ELSE least(min_bid_doc_date, min_eligibility_doc_date, min_financial_doc_date) END min_date,\n" +
            "       docs\n" +
            "FROM (SELECT tenderid,\n" +
            "             lotid,\n" +
            "             procurement_method_type,\n" +
            "             award.date                               award_date,\n" +
            "             CASE\n" +
            "               WHEN procurement_method_type = 'aboveThresholdUA'\n" +
            "                       THEN count(DISTINCT CASE\n" +
            "                                             WHEN document.document_of <> 'lot' OR document.related_item = lotid\n" +
            "                                                     THEN document.id END)\n" +
            "               ELSE greatest(count(DISTINCT CASE\n" +
            "                                               WHEN document.document_of <> 'lot' OR document.related_item = lotid\n" +
            "                                                       THEN document.id END),\n" +
            "                              count(DISTINCT CASE\n" +
            "                                               WHEN eligibility_document.document_of <> 'lot' OR\n" +
            "                                                    eligibility_document.related_item = lotid\n" +
            "                                                       THEN eligibility_document.id END),\n" +
            "                              count(DISTINCT CASE\n" +
            "                                               WHEN financial_document.document_of <> 'lot' OR\n" +
            "                                                    financial_document.related_item = lotid\n" +
            "                                                       THEN financial_document.id END))\n" +
            "                 END                                  docs,\n" +
            "             min(document.date_published)             min_bid_doc_date,\n" +
            "             min(eligibility_document.date_published) min_eligibility_doc_date,\n" +
            "             min(financial_document.date_published)   min_financial_doc_date\n" +
            "\n" +
            "      FROM (SELECT tenderid, lotid, procurement_method_type, unnest(active_award_id) :::: BIGINT award_id\n" +
            "            FROM (SELECT tenderid,\n" +
            "                         procurement_method_type,\n" +
            "                         lotid,\n" +
            "                         CASE\n" +
            "                           WHEN active_award_id IS NULL\n" +
            "                                   THEN '{null}'\n" +
            "                           ELSE string_to_array(active_award_id, ',') END active_award_id\n" +
            "                  FROM (SELECT tender.outer_id                                                                      tenderid,\n" +
            "                               tender.procurement_method_type,\n" +
            "                               lot.outer_id                                                                         lotid,\n" +
            "                               string_agg(\n" +
            "                                 CASE WHEN award.status = 'active' THEN award.id :::: TEXT END,\n" +
            "                                 ',')                                                                               active_award_id\n" +
            "                        FROM tender\n" +
            "                               JOIN lot ON tender.id = lot.tender_id\n" +
            "                               JOIN award ON lot.id = award.lot_id\n" +
            "                                WHERE tender.outer_id = ANY (SELECT regexp_split_to_table(?1, ','))\n" +
            "                        GROUP BY tender.outer_id,\n" +
            "                                 procurement_method_type,\n" +
            "                                 lot.outer_id) a)b)c\n" +
            "             LEFT JOIN award ON award.id = award_id\n" +
            "             LEFT JOIN bid ON award.bid_id = bid.id\n" +
            "             LEFT JOIN document ON bid.id = document.bid_id\n" +
            "             LEFT JOIN eligibility_document ON bid.id = eligibility_document.bid_id\n" +
            "             LEFT JOIN financial_document ON bid.id = financial_document.bid_id\n" +
            "      GROUP BY tenderid, lotid, award.date, procurement_method_type) a\n", nativeQuery = true)
    List<Object[]> getTenderLotWithActiveAwardDateMinDocumentPublishedAndDocsCount(String tenderIds);


    @Query(value = "SELECT tenderid,\n" +
            "       lotid,\n" +
            "       unsuccessful_awards,\n" +
            "       bid.supplier_identifier_scheme,\n" +
            "       bid.supplier_identifier_id,\n" +
            "       (SELECT cpv_count\n" +
            "        FROM supplier_for_pe_with_3cpv\n" +
            "        WHERE supplier_for_pe_with_3cpv.supplier = concat(bid.supplier_identifier_scheme, '-', bid.supplier_identifier_id)\n" +
            "          AND procuring_entity = a.procuring_entity)\n" +
            "FROM (SELECT tender.outer_id                                                           tenderid,\n" +
            "             tender.tv_procuring_entity                                                procuring_entity,\n" +
            "             lot.outer_id                                                              lotid,\n" +
            "             count(DISTINCT CASE WHEN award.status = 'unsuccessful' THEN award.id END) unsuccessful_awards,\n" +
            "             string_agg(DISTINCT CASE WHEN award.status = 'active' THEN award.id :::: TEXT END,\n" +
            "                                 ',')                                                  active_awards\n" +
            "      FROM tender\n" +
            "             JOIN lot ON tender.id = lot.tender_id\n" +
            "             JOIN award ON lot.id = award.lot_id\n" +
            "             JOIN bid ON award.bid_id = bid.id\n" +
            "      WHERE tender.outer_id = ANY (SELECT regexp_split_to_table(?1, ','))\n" +
            "      GROUP BY tender.outer_id, lot.outer_id, tender.tv_procuring_entity) a\n" +
            "       LEFT JOIN award ON active_awards :::: BIGINT = award.id\n" +
            "       LEFT JOIN bid ON award.bid_id = bid.id", nativeQuery = true)
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
            "ORDER BY tender.date_created LIMIT 100", nativeQuery = true)
    List<Object[]> findTendersWithUnuniqueTenderersCount(ZonedDateTime date,
                                                         List<String> procedureStatus,
                                                         List<String> procedureType,
                                                         List<String> procuringEntityKind);


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
            "ORDER BY tender.date_created LIMIT 100", nativeQuery = true)
    List<Object[]> findTendersWithCPVAndPendingContractsCount(ZonedDateTime date,
                                                              List<String> procedureStatus,
                                                              List<String> procedureType,
                                                              List<String> procuringEntityKind);

    @Query(value = "SELECT tender.outer_id, tender.date_created, string_agg(document.format, ',') FROM tender " +
            "LEFT JOIN document ON tender.id = document.tender_id " +
            "WHERE tender.date_created > ?1 AND tender.date > now() - INTERVAL '1 year' " +
            "AND tender.procurement_method_type IN ?2 " +
            "AND tender.procuring_entity_kind IN ?3  AND tender.status <> 'cancelled' " +
            "GROUP BY tender.outer_id, tender.date_created " +
            "ORDER BY tender.date_created LIMIT 100", nativeQuery = true)
    List<Object[]> getTendersWithDocumentTypes(ZonedDateTime datetime,
                                               List<String> procedureType,
                                               List<String> procuringEntityKind);

    @Query(value = "SELECT t.outer_id FROM tender t " +
            "WHERE t.date_created > ?1 AND t.date > now() - INTERVAL '1 year' " +
            "AND t.status IN ?2 " +
            "AND t.procurement_method_type IN ?3 " +
            "AND t.procuring_entity_kind IN ?4 " +
            "ORDER BY t.date_created LIMIT 100", nativeQuery = true)
    List<String> findTenderIdByProcedureStatusAndProcedureType(ZonedDateTime date,
                                                               List<String> procedureStatus,
                                                               List<String> procedureType,
                                                               List<String> procuringEntityKind);

    @Query(value = "SELECT t.outer_id FROM tender t " +
            "WHERE t.procurement_method_type IN ?3 " +
            "AND t.status IN ?2 AND t.date_created > ?1 AND t.date > now() - INTERVAL '1 year' " +
            "AND t.procuring_entity_kind IN ?4 " +
            "AND t.tv_subject_of_procurement NOT LIKE '45%'" +
            "ORDER BY t.date_created LIMIT 100", nativeQuery = true)
    List<String> findGoodsServicesTenderIdByProcedureStatusAndProcedureType(ZonedDateTime date,
                                                                            List<String> procedureStatus,
                                                                            List<String> procedureType,
                                                                            List<String> procuringEntityKind);

    @Query(value = "SELECT t.outer_id FROM tender t " +
            "WHERE t.procurement_method_type IN ?3 " +
            "AND t.status IN ?2 AND t.date_created > ?1 AND t.date > now() - INTERVAL '1 year' " +
            "AND t.procuring_entity_kind IN ?4 " +
            "AND t.main_procurement_category IN ('goods','services') " +
            "AND t.tv_subject_of_procurement NOT LIKE '45%'" +
            "ORDER BY t.date_created LIMIT 100", nativeQuery = true)
    List<String> getIndicator2_10TenderData(ZonedDateTime date,
                                            List<String> procedureStatus,
                                            List<String> procedureType,
                                            List<String> procuringEntityKind);


    @Query(value = "SELECT t.outer_id FROM tender t " +
            "WHERE t.procurement_method_type IN ?3 " +
            "AND t.status IN ?2 " +
            "AND t.date_created > ?1 " +
            "AND t.date > now() - INTERVAL '1 year' " +
            "AND t.procuring_entity_kind IN ?4 " +
            "AND t.tv_subject_of_procurement NOT LIKE '45%'" +
            "AND t.main_procurement_category IN ('goods', 'services')" +
            "ORDER BY t.date_created LIMIT 100", nativeQuery = true)
    List<String> getIndicator2_4TenderData(ZonedDateTime date,
                                           List<String> procedureStatus,
                                           List<String> procedureType,
                                           List<String> procuringEntityKind);

    @Query(value = "SELECT t.outer_id FROM tender t " +
            "WHERE t.procurement_method_type IN ?3 " +
            "AND t.status IN ?2 AND t.date_created > ?1 AND t.date > now() - INTERVAL '1 year' " +
            "AND t.procuring_entity_kind IN ?4 " +
            "AND t.tv_subject_of_procurement NOT LIKE '45%'" +
            "AND t.main_procurement_category IN('goods','services') " +
            "ORDER BY t.date_created LIMIT 100", nativeQuery = true)
    List<String> getIndicator1_8TenderData(ZonedDateTime date,
                                           List<String> procedureStatus,
                                           List<String> procedureType,
                                           List<String> procuringEntityKind);

    @Query(value = "SELECT t.outer_id FROM tender t " +
            "WHERE t.procurement_method_type IN ?3 " +
            "AND t.status IN ?2 " +
            "AND t.date_created > ?1 AND t.date > now() - INTERVAL '1 year' " +
            "AND t.procuring_entity_kind IN ?4 " +
            "AND t.tv_subject_of_procurement LIKE '45%' " +
            "ORDER BY t.date_created LIMIT 100", nativeQuery = true)
    List<String> findWorksTenderIdByProcedureStatusAndProcedureType(ZonedDateTime date,
                                                                    List<String> procedureStatus,
                                                                    List<String> procedureType,
                                                                    List<String> procuringEntityKind);

    @Query(value = "SELECT t.outer_id FROM tender t " +
            "WHERE t.procurement_method_type IN ?3 " +
            "AND t.status IN ?2 " +
            "AND t.date_created > ?1 AND t.date > now() - INTERVAL '1 year' " +
            "AND t.procuring_entity_kind IN ?4 " +
            "AND t.tv_subject_of_procurement LIKE '45%' " +
            "AND t.main_procurement_category IN ('works') " +
            "ORDER BY t.date_created LIMIT 100", nativeQuery = true)
    List<String> getIndicator_2_10_1TenderData(ZonedDateTime date,
                                               List<String> procedureStatus,
                                               List<String> procedureType,
                                               List<String> procuringEntityKind);


    @Query(value = "SELECT t.outer_id FROM tender t " +
            "WHERE t.procurement_method_type IN ?3 " +
            "AND t.status IN ?2 " +
            "AND t.date_created > ?1 " +
            "AND t.date > now() - INTERVAL '1 year' " +
            "AND t.procuring_entity_kind IN ?4 " +
            "AND t.tv_subject_of_procurement LIKE '45%' " +
            "AND t.main_procurement_category IN ('works') " +
            "ORDER BY t.date_created LIMIT 100", nativeQuery = true)
    List<String> getIndicator2_4_1TenderData(ZonedDateTime date,
                                             List<String> procedureStatus,
                                             List<String> procedureType,
                                             List<String> procuringEntityKind);


    @Query(value = "SELECT t.outer_id FROM tender t " +
            "WHERE t.procurement_method_type IN ?3 " +
            "AND t.status IN ?2 " +
            "AND t.date_created > ?1 AND t.date > now() - INTERVAL '1 year' " +
            "AND t.procuring_entity_kind IN ?4 " +
            "AND t.tv_subject_of_procurement LIKE '45%' " +
            "AND t.main_procurement_category IN('works') " +
            "ORDER BY t.date_created LIMIT 100", nativeQuery = true)
    List<String> getIndicator1_8_1TenderData(ZonedDateTime date,
                                             List<String> procedureStatus,
                                             List<String> procedureType,
                                             List<String> procuringEntityKind);


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
            "            WHEN procuring_entity_kind IN ('general','authority','central','social') AND tv_tender_cpv NOT LIKE '45%'\n" +
            "              THEN 200000\n" +
            "            WHEN procuring_entity_kind IN ('general','authority','central','social') AND tv_tender_cpv LIKE '45%'\n" +
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
            "  CASE WHEN cause = 'noCompetition' THEN TRUE ELSE FALSE END noCompetition\n" +
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
            "ORDER BY date_created LIMIT 100", nativeQuery = true)
    List<Object[]> findTenderIdsWithCPVListWithPendingContractsAndNotMonopolySupplier(ZonedDateTime date,
                                                                                      List<String> procedureStatus,
                                                                                      List<String> procedureType,
                                                                                      List<String> procuringEntityKind);

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
            "                   sum(CASE WHEN awardstatus = 'unsuccessful' THEN 1 ELSE 0 END) unsuccessfulaward,\n" +
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

    @Query(value = "SELECT outer_id, start_date, procuring_entity_kind, currency, amount, date\n" +
            "FROM tender t\n" +
            "WHERE tv_tender_cpv NOT LIKE '45%' AND \n" +
            "      tv_tender_cpv NOT LIKE  '6611%' AND \n" +
            "      title NOT LIKE  '%кредит%' AND \n" +
            "      title NOT LIKE '%гарант%' AND \n" +
            "      title NOT LIKE '%лізинг%' AND \n" +
            "      t.date_created > ?1 AND t.date > now() - INTERVAL '1 year' AND \n" +
            "      procurement_method_type IN ?3 AND\n" +
            "      t.procuring_entity_kind IN ?4 AND status IN ?2 " +
            "ORDER BY t.date_created LIMIT 100", nativeQuery = true)
    List<Object[]> findGoodsServicesTenderIdPAKindAndAmountExceptFinances(ZonedDateTime date,
                                                                          List<String> procedureStatus,
                                                                          List<String> procedureType,
                                                                          List<String> procuringEntityKind);

    @Query(value = "SELECT outer_id, start_date, procuring_entity_kind, currency, amount, date\n" +
            "FROM tender t\n" +
            "WHERE tv_tender_cpv NOT LIKE '45%' AND \n" +
            "      tv_tender_cpv NOT LIKE  '6611%' AND \n" +
            "      title NOT LIKE  '%кредит%' AND \n" +
            "      title NOT LIKE '%гарант%' AND \n" +
            "      title NOT LIKE '%лізинг%' AND \n" +
            "      t.date_created > ?1 AND t.date > now() - INTERVAL '1 year' AND \n" +
            "      procurement_method_type IN ?3 AND\n" +
            "      t.procuring_entity_kind IN ?4 AND status IN ?2 AND" +
            "      t.main_procurement_category IN('services','goods') " +
            "ORDER BY t.date_created LIMIT 100", nativeQuery = true)
    List<Object[]> getIndicator1_9TenderData(ZonedDateTime date,
                                             List<String> procedureStatus,
                                             List<String> procedureType,
                                             List<String> procuringEntityKind);


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
            "-- " +
            "      AND title NOT LIKE  '%кредит%'\n" +
            "-- " +
            "      AND title NOT LIKE '%гарант%'\n" +
            "-- " +
            "      AND title NOT LIKE '%лізинг%'\n" +
            "      AND title NOT LIKE ANY ('{\"%кредит%\", \"%гарант%\", \"%лізинг%\"}')\n" +
            "      AND t.date_created > ?1\n" +
            "      AND t.date > now() - INTERVAL '1 year'\n" +
            "      AND procurement_method_type IN ?3\n" +
            "      AND t.procuring_entity_kind IN ?4\n" +
            "      AND status IN ?2\n " +
            "ORDER BY t.date_created LIMIT 100", nativeQuery = true)
    List<Object[]> findGoodsServicesProcuringEntityKindAmount(ZonedDateTime date,
                                                              List<String> procedureStatus,
                                                              List<String> procedureType,
                                                              List<String> procuringEntityKind);

    @Query(value = "SELECT outer_id, tv_procuring_entity, procuring_entity_kind,  \n" +
            "  CASE WHEN currency = 'UAH'\n" +
            "    THEN amount\n" +
            "  ELSE\n" +
            "    amount *\n" +
            "    (SELECT rate\n" +
            "     FROM exchange_rate\n" +
            "     WHERE currency = exchange_rate.code AND\n" +
            "           concat(substr(to_char(coalesce(t.start_date, t.date), 'YYYY-MM-DD HH:mm:ss.zzzzzz'), 0, 11),' 00:00:00.000000')\n" +
            "           :::: DATE = exchange_rate.date)\n" +
            "    END amount, tv_subject_of_procurement \n" +
            "FROM tender t\n" +
            "WHERE tv_tender_cpv NOT LIKE '45%'\n" +
            "      AND tv_tender_cpv NOT LIKE  '6611%'\n" +
            "      AND title NOT LIKE ANY ('{\"%кредит%\", \"%гарант%\", \"%лізинг%\"}')\n" +
            "      AND t.date_created > ?1\n" +
            "      AND t.date > now() - INTERVAL '1 year'\n" +
            "      AND procurement_method_type IN ?3\n" +
            "      AND t.procuring_entity_kind IN ?4\n" +
            "      AND status IN ?2\n " +
            "       AND t.main_procurement_category IN ('goods','services') \n" +
            "ORDER BY t.date_created LIMIT 100", nativeQuery = true)
    List<Object[]> getIndicator2_5_1TenderData(ZonedDateTime date,
                                               List<String> procedureStatus,
                                               List<String> procedureType,
                                               List<String> procuringEntityKind);

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
            "ORDER BY date_created LIMIT 100", nativeQuery = true)
    List<Object[]> findGoodsServicesProcuringEntityKindAndSupplierAmount(ZonedDateTime date,
                                                                         List<String> procedureStatus,
                                                                         List<String> procedureType,
                                                                         List<String> procuringEntityKind);

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
            "ORDER BY date_created LIMIT 100", nativeQuery = true)
    List<Object[]> findWorksProcuringEntityKindAndSupplierAmount(ZonedDateTime date,
                                                                 List<String> procedureStatus,
                                                                 List<String> procedureType,
                                                                 List<String> procuringEntityKind);

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
            "ORDER BY date_created LIMIT 100", nativeQuery = true)
    List<Object[]> findWorksPendingContractsCountProcuringEntityKindAndSupplierAmount(ZonedDateTime date,
                                                                                      List<String> procedureStatus,
                                                                                      List<String> procedureType,
                                                                                      List<String> procuringEntityKind);

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
            "ORDER BY date_created LIMIT 100", nativeQuery = true)
    List<Object[]> findWorksProcuringEntityKindAmount(ZonedDateTime date,
                                                      List<String> procedureStatus,
                                                      List<String> procedureType,
                                                      List<String> procuringEntityKind);


    @Query(value = "" +
            "SELECT tender.outer_id tender_id, contract.outer_id contract_id, contract.date_signed," +
            " min(CASE WHEN contract_change.status = 'active' THEN contract_change.date_signed ELSE NULL END ), " +
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
            "ORDER BY contract.date_created LIMIT 100", nativeQuery = true)
    List<Object[]> findTenderWithContractDateSignedAndMinChangeDateSigned(ZonedDateTime date,
                                                                          List<String> procedureStatus,
                                                                          List<String> procedureType,
                                                                          List<String> procuringEntityKind);

    @Query(value = "SELECT outer_id, start_date, procuring_entity_kind, currency, amount\n" +
            "FROM tender t\n " +
            "WHERE tv_tender_cpv LIKE '45%' AND \n" +
            "      t.date_created > ?1 AND t.date > now() - INTERVAL '1 year' AND \n" +
            "      procurement_method_type IN ?3 AND\n" +
            "      t.procuring_entity_kind IN ?4 AND status IN ?2 " +
            "ORDER BY t.date_created LIMIT 100", nativeQuery = true)
    List<Object[]> findWorkTenderIdPAKindAndAmount(ZonedDateTime date,
                                                   List<String> procedureStatus,
                                                   List<String> procedureType,
                                                   List<String> procuringEntityKind);

    @Query(value = "SELECT outer_id, start_date, procuring_entity_kind, currency, amount, date\n" +
            "FROM tender t\n " +
            "WHERE tv_tender_cpv LIKE '45%' AND \n" +
            "      t.date_created > ?1 AND t.date > now() - INTERVAL '1 year' AND \n" +
            "      procurement_method_type IN ?3 AND\n" +
            "      t.main_procurement_category IN ('works') AND " +
            "      t.procuring_entity_kind IN ?4 AND status IN ?2 " +
            "ORDER BY t.date_created LIMIT 100", nativeQuery = true)
    List<Object[]> getIndicator1_9_1TenderData(ZonedDateTime date,
                                               List<String> procedureStatus,
                                               List<String> procedureType,
                                               List<String> procuringEntityKind);


    @Transactional
    @Query(value = "SELECT t.outer_id FROM tender t  " +
            "WHERE t.status IN ?2 " +
            "   AND t.date_created > ?1 AND t.date > now() - INTERVAL '1 year' " +
            "   AND t.procuring_entity_kind IN ?3 LIMIT 100", nativeQuery = true)
    List<String> findTenderIdByProcedureStatus(ZonedDateTime date,
                                               List<String> procedureStatus,
                                               List<String> procuringEntityKind);

    @Transactional
    @Query(value = "SELECT t.outer_id FROM tender t " +
            "WHERE t.procurement_method_type IN ?2 " +
            "AND t.date_created >  ?1 AND t.date > now() - INTERVAL '1 year' " +
            "AND t.procuring_entity_kind IN ?3 LIMIT 100", nativeQuery = true)
    List<String> findTenderIdByProcedureType(ZonedDateTime date,
                                             List<String> procedureType,
                                             List<String> procuringEntityKind);

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
            "       count(DISTINCT CASE WHEN tender_contract.status = 'pending' THEN tender_contract.id END)\n" +
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
            "ORDER BY tender.date_created LIMIT 100", nativeQuery = true)
    List<Object[]> findTenderIdPWithPendingContractAndTwiceUnsuccessful(ZonedDateTime date,
                                                                        List<String> procedureStatus,
                                                                        List<String> procedureType,
                                                                        List<String> procuringEntityKind);

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
            "       count(DISTINCT  (CASE WHEN award.status = 'active' THEN lot.id ELSE NULL END ))  lot_count,\n" +
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
            "ORDER BY t.date_created LIMIT 100", nativeQuery = true)
    List<Object[]> findWorkTenderIdCurrencyAmountByProcedureStatusAndProcedureType(ZonedDateTime date,
                                                                                   List<String> procedureStatus,
                                                                                   List<String> procedureType,
                                                                                   List<String> procuringEntityKind);


    @Query(value = "SELECT t.outer_id, " +
            "t.currency, " +
            "t.amount, " +
            "CASE WHEN t.start_date IS NOT NULL THEN t.start_date ELSE t.date END,  " +
            "t.date_created " +
            "FROM tender t " +
            "WHERE t.date_created > ?1 AND t.date > now() - INTERVAL '1 year' " +
            "AND t.status IN ?2 " +
            "AND t.procurement_method_type IN ?3 " +
            "AND t.procuring_entity_kind IN ?4 " +
            "AND t.main_procurement_category = 'works' " +
            "AND t.tv_subject_of_procurement LIKE '45%'" +
            "AND (t.start_date <= current_date OR t.enquiry_start_date <= current_date) " +
            "ORDER BY t.date_created LIMIT 100", nativeQuery = true)
    List<Object[]> getIndicator1_4_1TenderData(ZonedDateTime date,
                                               List<String> procedureStatus,
                                               List<String> procedureType,
                                               List<String> procuringEntityKind);


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
            "AND (t.start_date::::TIMESTAMP::::date <= CURRENT_DATE OR t.enquiry_start_date::::TIMESTAMP::::date <= CURRENT_DATE) " +
            "ORDER BY t.date_created LIMIT 100", nativeQuery = true)
    List<Object[]> findGoodsServicesTenderIdCurrencyAmountByProcedureStatusAndProcedureType(ZonedDateTime date,
                                                                                            List<String> procedureStatus,
                                                                                            List<String> procedureType,
                                                                                            List<String> procuringEntityKind);

    @Query(value = "SELECT t.outer_id, " +
            "t.currency, " +
            "t.amount, " +
            "CASE WHEN t.start_date IS NOT NULL THEN t.start_date ELSE t.date END, " +
            "t.date_created " +
            "FROM tender t " +
            "WHERE t.date_created > ?1 AND t.date > now() - INTERVAL '1 year' " +
            "AND t.status IN ?2 " +
            "AND t.procurement_method_type IN ?3 " +
            "AND t.procuring_entity_kind IN ?4 " +
            "AND t.tv_subject_of_procurement NOT LIKE '45%' " +
            "AND t.main_procurement_category IN ('goods','services') " +
            "AND (t.start_date::::TIMESTAMP::::date <= CURRENT_DATE OR t.enquiry_start_date::::TIMESTAMP::::date <= CURRENT_DATE) " +
            "ORDER BY t.date_created LIMIT 100", nativeQuery = true)
    List<Object[]> getIndicator1_4TenderData(ZonedDateTime date,
                                             List<String> procedureStatus,
                                             List<String> procedureType,
                                             List<String> procuringEntityKind);

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
            "                        WHEN (document.format <> 'application/pkcs7-signature' OR document.format IS NULL)\n" +
            "                                THEN document.id END)          tender_docs,\n" +
            "       count(DISTINCT CASE\n" +
            "                        WHEN contract_document.document_of = 'contract' AND\n" +
            "                             (contract_document.format <> 'application/pkcs7-signature' OR document.format IS NULL)\n" +
            "                                THEN contract_document.id END) contract_docs,\n" +
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
            "       LEFT JOIN contract ON tender_contract.id = contract.tender_contract_id\n" +
            "       LEFT JOIN contract_document ON contract.id = contract_document.contract_id\n" +
            "GROUP BY tenderouterid,\n" +
            "         lot_outer_id,\n" +
            "         complaints_count, active_award_date, tender_contract.status, lotstatus", nativeQuery = true)
    List<Object[]> getTenderLotsAndComplaintsCountAndActiveAwardDateAndContractStatus(String tenderIds);

    @Query(value = "SELECT\n" +
            "  tenderid,\n" +
            "  lotid,\n" +
            "  awardid,\n" +
            "  award.date,\n" +
            "  sum(CASE WHEN (document.format <> 'application/pkcs7-signature' OR document.format IS NULL) AND\n" +
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
            "                CASE WHEN aword_date IS NOT NULL  AND  min_date_modified IS NOT NULL " +
            " THEN abs(date_part('day', aword_date::::TIMESTAMP - min_date_modified::::TIMESTAMP)) ELSE NULL END DAYS\n" +
            "              FROM (\n" +
            "                     SELECT\n" +
            "                       tender.outer_id                    tender_id,\n" +
            "                       lot.outer_id                       lot_id,\n" +
            "                       tender_contract.status             tender_contract_status,\n" +
            "                       CASE WHEN tender_contract.status = 'active'\n" +
            "                         THEN (award.date::::TIMESTAMP WITH TIME ZONE AT TIME ZONE 'Europe/Kiev')::::date\n" +
            "                       ELSE NULL END                      aword_date,\n" +
            "                       (MIN(CASE WHEN (DOCUMENT.format <> 'application/pkcs7-signature' OR DOCUMENT.format IS NULL)\n" +
            "                         THEN DOCUMENT.date_modified END)::::TIMESTAMP WITH TIME ZONE AT TIME ZONE 'Europe/Kiev')::::date min_date_modified\n" +
            "                     FROM tender\n" +
            "                       JOIN lot ON tender.id = lot.tender_id\n" +
            "                       JOIN award ON lot.id = award.lot_id\n" +
            "                       JOIN tender_contract ON award.id = tender_contract.award_id\n" +
            "                       LEFT JOIN DOCUMENT ON tender_contract.id = DOCUMENT.contract_id\n" +
            "                     WHERE tender.outer_id = ANY (SELECT regexp_split_to_table(?1, ','))\n" +
            "                     GROUP BY tender.outer_id, lot.outer_id, tender_contract.status, award.date\n" +
            "                   ) a) b\n" +
            "       GROUP BY tender_id, lot_id) C", nativeQuery = true)
    List<Object[]> getTenderWithLotAndDaysBetweenAwardDateAndDocumentDateModified(String tenderIds);

    @Query(value = "SELECT\n" +
            "  tender.outer_id,\n" +
            "  min(CASE WHEN award.status = 'active' THEN award.date ELSE NULL END) min_active_award_date,\n" +
            "  min(CASE WHEN (document.author IS NULL OR document.author <> 'auction') " +
            " AND (document.format IS NULL OR document.format <>'application/pkcs7-signature')" +
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
            "  min(CASE WHEN ( document.author IS NULL OR document.author <> 'auction') " +
            " AND (document.author IS NULL OR document.format <> 'application/pkcs7-signature') \n" +
            "    THEN document.date_published\n" +
            "      ELSE NULL END)\n" +
            "FROM tender\n" +
            " LEFT JOIN document ON tender.id = document.tender_id\n" +
            " WHERE tender.outer_id =  ANY (SELECT regexp_split_to_table(?1, ',')) " +
            "GROUP BY tender.outer_id, tender.award_start_date", nativeQuery = true)
    List<Object[]> getTenderWithStartAwardDateAndMinDocDatePublished(String tenderIds);

    @Query(value = "SELECT\n" +
            "    tender.outer_id,\n" +
            "    tender.end_date,\n" +
            "    min(CASE WHEN ( document.author IS NULL OR document.author <> 'auction')\n" +
            "        AND (document.author IS NULL OR document.format <> 'application/pkcs7-signature')\n" +
            "                 THEN document.date_published\n" +
            "        END)\n" +
            "FROM tender\n" +
            "         LEFT JOIN document ON tender.id = document.tender_id\n" +
            "WHERE tender.outer_id =  ANY (SELECT regexp_split_to_table(?1, ','))\n" +
            "GROUP BY tender.outer_id, tender.end_date", nativeQuery = true)
    List<Object[]> getTenderWithEndDateAndMinDocDatePublished(String tenderIds);

    @Query(value = "SELECT\n" +
            "  tender.outer_id tender_outer_id, lot.outer_id, sum( CASE  WHEN qualification.status ='unsuccessful' THEN 1 ELSE 0 END )\n" +
            "FROM lot\n" +
            "  LEFT JOIN qualification ON lot.id = qualification.lot_id\n" +
            "  JOIN tender ON lot.tender_id = tender.id\n" +
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
            "     WHERE tender.currency = exchange_rate.code AND\n" +
            "           concat(substr(to_char(coalesce(tender.start_date, tender.date), 'YYYY-MM-DD HH:mm:ss.zzzzzz'), 0, 11), ' 00:00:00.000000') :::: DATE =\n" +
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
            "tender.procuring_entity_kind,\n" +
            "tender.date", nativeQuery = true)
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

    @Query(value = "SELECT outer_id, status FROM Tender WHERE tender.outer_id IN ?1", nativeQuery = true)
    List<Object[]> findTenderOuterIdAndStatusByTendersOuterIdIn(List<String> outerIds);


    @Query(value = "SELECT tender.outer_id,\n" +
            "       tender.tender_id,\n" +
            "       tender.status,\n" +
            "       tender.procurement_method_type,\n" +
            "       tender.amount,\n" +
            "       tender.currency,\n" +
            "       tender.tv_tender_cpv,\n" +
            "       cpv.name,\n" +
            "       cpv2.cpv                                                                                             cpv2,\n" +
            "       cpv2.name                                                                                            cpv2_name,\n" +
            "       tender.tv_procuring_entity,\n" +
            "       tender.procuring_entity_kind,\n" +
            "       procuring_entity.identifier_legal_name,\n" +
            "       indicators_queue_region.correct_name,\n" +
            "       (SELECT count(*) > 0\n" +
            "        FROM (SELECT *\n" +
            "              FROM award\n" +
            "                       JOIN complaint c2 ON award.id = c2.award_id\n" +
            "              WHERE award.tender_id = tender.id\n" +
            "                AND c2.complaint_type = 'complaint'\n" +
            "                AND c2.status IN ('accepted', 'declined', 'satisfied', 'stopping')) a),\n" +
            "       tender.title,\n" +
            "       (SELECT COUNT(*) > 0\n" +
            "        FROM complaint\n" +
            "        WHERE tender_id = tender.id\n" +
            "          AND complaint_type = 'complaint'\n" +
            "          AND status IN ('accepted', 'declined', 'satisfied', 'stopping'))                                  has_tender_complaints,\n" +
            "       region_indicators_queue_item.materiality_score,\n" +
            "       tender.procurement_method_rationale,\n" +
            "       (SELECT COUNT(d.id) FROM document d WHERE d.tender_id = tender.id)                                   doc_count,\n" +
            "       (SELECT COUNT(l.id) FROM lot l WHERE l.tender_id = tender.id and l.status in ('active', 'complete')) lot_count,\n" +
            "       (SELECT COUNT(b.id) FROM bid b WHERE b.tender_id = tender.id and b.status = 'active')                bid_count,\n" +
            "       tender.main_procurement_category,\n" +
            "       (EXISTS (SELECT * FROM feedback_monitoring_info WHERE tender_outer_id = tender.outer_id)) exists_monitoring_info,\n" +
            "       (EXISTS (SELECT * FROM feedback_result WHERE tender_outer_id = tender.outer_id)) exists_result,\n" +
            "       (EXISTS (SELECT * FROM feedback_summary WHERE tender_outer_id = tender.outer_id)) exists_summary,\n" +
            "       (EXISTS (SELECT * FROM feedback_violation WHERE tender_outer_id = tender.outer_id)) exists_violation,\n" +
            "       (EXISTS (SELECT * FROM feedback_indicator WHERE tender_outer_id = tender.outer_id)) exists_indicators\n" +
            "FROM tender\n" +
            "         LEFT JOIN procuring_entity ON tender.procuring_entity_id = procuring_entity.id\n" +
            "         LEFT JOIN cpv_catalogue cpv ON tender.tv_tender_cpv = cpv.cpv\n" +
            "         LEFT JOIN cpv_catalogue cpv2 ON cpv.cpv2 = cpv2.cpv\n" +
            "         LEFT JOIN region_indicators_queue_item ON tender.outer_id = region_indicators_queue_item.tender_outer_id\n" +
            "         LEFT JOIN indicators_queue_region ON procuring_entity.region = indicators_queue_region.original_name\n" +
            "WHERE tender.outer_id = ANY (SELECT regexp_split_to_table(?1, ','))", nativeQuery = true)
    List<Object[]> getTendersCommonInfo(String tenderIds);

    @Query(value = "SELECT t.*\n" +
            "FROM tender t\n" +
            "JOIN procuring_entity pe ON t.procuring_entity_id = pe.id\n" +
            "WHERE t.status IN ('active.awarded', 'active.qualification', 'complete')\n" +
            "AND t.procurement_method_type IN ('aboveThresholdEU','aboveThresholdUA')\n" +
            "AND pe.kind IN ('general', 'authority', 'central', 'social', 'special')\n" +
            "AND t.date_created > ?1\n" +
            "ORDER BY t.date_created ", nativeQuery = true)
    Page<Tender> getRisk1_8_2Tenders(ZonedDateTime date, Pageable pageable);

    @Query(value = "SELECT\n" +
            "  outer_id,\n" +
            "  tv_procuring_entity,\n" +
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
            "                AND concat(substr(to_char(coalesce(t.start_date, t.date), 'YYYY-MM-DD HH:mm:ss.zzzzzz'), 0, 11),\n" +
            "                           ' 00:00:00.000000') :::: DATE = exchange_rate.date) rate \n" +
            "       FROM tender t \n" +
            "       WHERE main_procurement_category IN ?5\n" +
            "                  AND t.date_created > ?1\n" +
            "                  AND t.date > now() - INTERVAL '1 year'\n" +
            "                  AND procurement_method_type IN ?3\n" +
            "                  AND t.procuring_entity_kind IN ?4\n" +
            "                  AND status IN ?2\n" +
            "     ) a\n" +
            "ORDER BY date_created LIMIT 100", nativeQuery = true)
    List<Object[]> findWorksProcuringEntityKindAmountByMainProcurementCategory(ZonedDateTime date,
                                                                               List<String> procedureStatus,
                                                                               List<String> procedureType,
                                                                               List<String> procuringEntityKind,
                                                                               List<String> categories);

    @Query(value = "SELECT t.outer_id FROM tender t " +
            "WHERE t.procurement_method_type IN ?3 " +
            "AND t.status IN ?2 AND t.date_created > ?1 AND t.date > now() - INTERVAL '1 year' " +
            "AND t.procuring_entity_kind IN ?4 " +
            "AND t.main_procurement_category IN ?5 " +
            "ORDER BY t.date_created LIMIT 100", nativeQuery = true)
    List<String> findGoodsServicesTenderIdByProcedureStatusAndProcedureTypeByMainProcurementCategory(ZonedDateTime date,
                                                                                                     List<String> procedureStatus,
                                                                                                     List<String> procedureType,
                                                                                                     List<String> procuringEntityKind,
                                                                                                     List<String> categories);

    @Query(value = "" +
            "SELECT\n" +
            "  outer_id,\n" +
            "  pending_contracts_count, " +
            "  tv_procuring_entity, " +
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
            "                concat(substr(to_char(coalesce(tender.start_date, tender.date), 'YYYY-MM-DD HH:mm:ss.zzzzzz'), 0, 11), ' 00:00:00.000000')\n" +
            "                :::: DATE = exchange_rate.date) ELSE NULL END rate,\n" +
            "         tender.date_created\n" +
            "\n" +
            "       FROM tender\n" +
            "         LEFT JOIN tender_contract ON tender.id = tender_contract.tender_id\n" +
            "         LEFT JOIN award ON tender.id = award.tender_id\n" +
            "       WHERE  tender.main_procurement_category IN ?5 \n" +
            "             AND tender.date_created > ?1\n" +
            "             AND tender.date > now() - INTERVAL '1 year'\n" +
            "             AND procurement_method_type IN ?3\n" +
            "             AND tender.procuring_entity_kind IN ?4\n" +
            "             AND tender.status IN ?2\n" +
            "       GROUP BY tender.outer_id, tender.date_created, tender.currency, tender.amount, tender.start_date, tv_procuring_entity, procuring_entity_kind, tender.date \n" +
            "     ) a\n" +
            "ORDER BY date_created LIMIT 100", nativeQuery = true)
    List<Object[]> findWorksPendingContractsCountProcuringEntityKindAndSupplierAmountByMainProcurementCategory(ZonedDateTime date,
                                                                                                               List<String> procedureStatus,
                                                                                                               List<String> procedureType,
                                                                                                               List<String> procuringEntityKind,
                                                                                                               List<String> categories);

    @Query(nativeQuery = true, value = "SELECT * FROM tender " +
            "WHERE date_created > ?1\n" +
            "AND date > now() - INTERVAL '1 year'\n" +
            "AND procurement_method_type IN ?3\n" +
            "AND procuring_entity_kind IN ?4\n" +
            "AND status IN ?2\n" +
            "ORDER BY date_created LIMIT 100")
    List<Tender> findTenders(ZonedDateTime date,
                             List<String> procedureStatus,
                             List<String> procedureType,
                             List<String> procuringEntityKind);

    @Query(nativeQuery = true, value = "SELECT id FROM tender " +
            "WHERE date_created > ?1\n" +
            "AND date > now() - INTERVAL '1 year'\n" +
            "AND procurement_method_type IN ?3\n" +
            "AND procuring_entity_kind IN ?4\n" +
            "AND status IN ?2\n" +
            "ORDER BY date_created LIMIT 100")
    List<Long> findTenderIds(ZonedDateTime date,
                             List<String> procedureStatus,
                             List<String> procedureType,
                             List<String> procuringEntityKind);


    @Query(nativeQuery = true, value = "SELECT id FROM tender " +
            "WHERE date_created > ?1\n" +
            "AND date > now() - INTERVAL '1 year'\n" +
            "AND procurement_method_type IN ?3\n" +
            "AND procuring_entity_kind IN ?4\n" +
            "AND main_procurement_category IN ?5\n" +
            "AND status IN ?2\n" +
            "ORDER BY date_created LIMIT 100")
    List<Long> findTenderIds(ZonedDateTime date,
                             List<String> procedureStatus,
                             List<String> procedureType,
                             List<String> procuringEntityKind,
                             List<String> categories);

    @Query(value = "select * from tender where id in ?1", nativeQuery = true)
    List<Tender> findByIdIn(List<Long> ids);

    @Query(nativeQuery = true, value = "SELECT * FROM tender " +
            "WHERE date_created > ?1\n" +
            "AND date > now() - INTERVAL '1 year'\n" +
            "AND procurement_method_type IN ?3\n" +
            "AND procuring_entity_kind IN ?4\n" +
            "AND main_procurement_category IN ?5\n" +
            "AND status IN ?2\n" +
            "ORDER BY date_created LIMIT 100")
    List<Tender> findTendersByMainProcurementCategory(ZonedDateTime date,
                                                      List<String> procedureStatus,
                                                      List<String> procedureType,
                                                      List<String> procuringEntityKind,
                                                      List<String> categories);

    @Query(nativeQuery = true, value = "select * from tender " +
            "where date_created > ?1\n" +
            "AND date > now() - INTERVAL '1 year'\n" +
            "AND procuring_entity_kind IN ?2\n" +
            "ORDER BY date_created LIMIT 100")
    List<Tender> findTendersForContract(ZonedDateTime date, List<String> procuringEntityKind);

    @Query(value = "SELECT outer_id FROM tender WHERE date_modified > ?1 ORDER BY date_modified", nativeQuery = true)
    Page<String> findAllAfterDateModified(ZonedDateTime since, Pageable pageRequest);
}
