package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.TenderContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TenderContractRepository extends JpaRepository<TenderContract, Long> {

    TenderContract findFirstByOuterId(String outerId);

    @Query(value = "" +
            "SELECT\n" +
            "  tenderid,\n" +
            "  lotid,\n" +
            "  (SELECT tender_contract.outer_id\n" +
            "   FROM tender_contract\n" +
            "   WHERE id = contractid) contractouterid,\n" +
            "  string_agg(document.format, ',')\n" +
            "\n" +
            "FROM (\n" +
            "       SELECT\n" +
            "         tenderid,\n" +
            "         lotid,\n" +
            "         unnest(contractsids) :::: BIGINT contractid\n" +
            "       FROM (\n" +
            "              SELECT\n" +
            "                tenderid,\n" +
            "                lotid,\n" +
            "                CASE WHEN tendercontarcs IS NULL\n" +
            "                  THEN '{null}'\n" +
            "                ELSE string_to_array(tendercontarcs, ',') END contractsids\n" +
            "              FROM (\n" +
            "                     SELECT\n" +
            "                       tenderid,\n" +
            "                       lotid,\n" +
            "                       string_agg(DISTINCT CASE WHEN tendercontractstatus = 'active'\n" +
            "                         THEN tendercontractid :::: TEXT END, ',') tendercontarcs\n" +
            "                     FROM (\n" +
            "                            SELECT\n" +
            "                              tender.outer_id        tenderid,\n" +
            "                              lot.outer_id           lotid,\n" +
            "                              award.outer_id         awardid,\n" +
            "                              tender_contract.id     tendercontractid,\n" +
            "                              tender_contract.status tendercontractstatus\n" +
            "                            FROM tender\n" +
            "                              JOIN lot ON tender.id = lot.tender_id\n" +
            "                              JOIN award ON lot.id = award.lot_id\n" +
            "                              LEFT JOIN tender_contract ON award.id = tender_contract.award_id\n" +
            "                            WHERE tender.outer_id = ANY (SELECT regexp_split_to_table(?1, ','))\n" +
            "                          ) a\n" +
            "                     GROUP BY tenderid, lotid\n" +
            "                   ) b) c) d\n" +
            "  LEFT JOIN document ON contractid = document.contract_id\n" +
            "GROUP BY tenderid, lotid, contractid", nativeQuery = true)
    List<Object> getNonCancelledContractsAndDocumentTypesByTenderId(String tenderIds);

    @Query(value = "" +
            "select tenderid,\n" +
            "       lotid,\n" +
            "       contractId,\n" +
            "       string_agg(distinct document.format, ',') tender_docs,\n" +
            "       string_agg(distinct case\n" +
            "                             when contract_document.document_of = 'contract' then contract_document.format end, ',') contract_docs\n" +
            "from (select tenderid, lotid, unnest(contract_ids) :::: BIGINT contractid\n" +
            "      from (select tenderid,\n" +
            "                   lotid,\n" +
            "                   case\n" +
            "                     when contract_ids is null then '{null}'\n" +
            "                     else string_to_array(contract_ids, ',') end contract_ids\n" +
            "            from (select tender.outer_id tenderid, lot.outer_id lotid, string_agg(\n" +
            "                                                                         case\n" +
            "                                                                           when tender_contract.status = 'active'\n" +
            "                                                                                   then tender_contract.id :::: text\n" +
            "                                                                           else null end,\n" +
            "                                                                         ',') contract_ids\n" +
            "                  from tender\n" +
            "                         join lot on tender.id = lot.tender_id\n" +
            "                         join award on lot.id = award.lot_id\n" +
            "                         join tender_contract on award.id = tender_contract.award_id\n" +
            "                  WHERE tender.outer_id = ANY (SELECT regexp_split_to_table(?1, ','))\n" +
            "                  group by tender.outer_id, lot.outer_id) a) b) c\n" +
            "       left join document on contractid = document.contract_id\n" +
            "       left join contract on contractid = contract.tender_contract_id\n" +
            "       left join contract_document on contract.id = contract_document.contract_id\n" +
            "group by tenderid, lotid, contractid", nativeQuery = true)
    List<Object[]> getTenderLotContractDocsByTenderId(String tenderIds);


    @Query(value = "select c.contract_id, " +
            "c.outer_id as contractId, " +
            "c.date_created, " +
            "t.tender_id, " +
            "t.outer_id as tenderId, " +
            "t.status, " +
            "t.procurement_method_type " +
            "from contract c " +
            "join tender_contract tc on c.tender_contract_id = tc.id " +
            "join tender t on tc.tender_id = t.id " +
            "where c.outer_id = ?1 ", nativeQuery = true)
    Object[] getContractDimensions(String contractId);

}
