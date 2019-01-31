package com.datapath.persistence.repositories;

import com.datapath.persistence.entities.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface DocumentRepository extends PagingAndSortingRepository<Document, Long>, JpaRepository<Document, Long> {

    @Query("SELECT coalesce(max(d.id), 0) FROM Document d")
    Long getMaxId();

    @Query("SELECT coalesce(min(d.id), 0) FROM Document d")
    Long getMinId();

    @Query(value = "SELECT count(*) FROM Document d where tender_id is not null and award_id is not null", nativeQuery = true)
    Long countAllByTenderIdAndAwardIdIsNotNull();

    @Transactional
    @Modifying
    @Query(value =
            "WITH doc_info AS ( \n" +
                    "    SELECT \n" +
                    "      documents_info ->> 'id'     document_id, \n" +
                    "      documents_info ->> 'author' author \n " +
                    "    FROM ( \n" +
                    "           SELECT \n" +
                    "             json_array_elements(award -> 'documents') documents_info \n" +
                    "           FROM ( \n" +
                    "                  SELECT\n" +
                    "                    json_array_elements(cast(tender_data.data AS JSON) -> 'data' -> 'awards') award \n" +
                    "                  FROM tender_data \n" +
                    "                  WHERE tender_data.id >= ?1 AND tender_data.id <= ?2 " +
                    "                ) a) b ) \n" +
                    "UPDATE document \n" +
                    "SET author = doc_info.author FROM doc_info\n" +
                    "WHERE outer_id = doc_info.document_id ",
            nativeQuery = true)
    void updateAuthorFromTenderData(Long minId, Long maxId);

    @Transactional
    @Modifying
    @Query(value =
            "UPDATE\n" +
                    "    document\n" +
                    "set tender_id = null\n" +
                    "where id in\n" +
                    "      (select id\n" +
                    "       from document\n" +
                    "       where award_id is not null\n" +
                    "          or bid_id is not null\n" +
                    "          or contract_id is not null)\n" +
                    "  and id >= ?1\n" +
                    "  AND id <= ?2",
            nativeQuery = true)
    void removeTenderIdFromDocument(Long minId, Long maxId);
}