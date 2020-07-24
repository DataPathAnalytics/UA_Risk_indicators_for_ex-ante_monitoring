package com.datapath.integration.parsers.impl;

import com.datapath.integration.parsers.EntityParser;
import com.datapath.integration.parsers.exceptions.TenderDateNotFoundException;
import com.datapath.integration.parsers.exceptions.TenderValidationException;
import com.datapath.integration.utils.DateUtils;
import com.datapath.integration.utils.JsonUtils;
import com.datapath.persistence.entities.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class TenderParser implements EntityParser {

    private static final String AUTOCREATED_LOT = "autocreated";
    private static final String MIXED_LOT_CPV2 = "mixed";
    private static final String DASH = "-";

    private String rawData;
    private JsonNode dataNode;
    private Boolean hasAutocreatedLot;
    private ProcuringEntity procuringEntity;
    private Tender tender;
    private TenderData tenderData;
    private List<Document> tenderDocuments;
    private List<EligibilityDocument> tenderEligibilityDocuments;
    private List<FinancialDocument> tenderFinancialDocuments;
    private List<Question> tenderQuestions;
    private List<TenderItem> tenderItems;
    private List<Award> tenderAwards;
    private List<TenderContract> tenderContracts;
    private List<Lot> tenderLots;
    private List<Bid> tenderBids;
    private List<Qualification> tenderQualifications;

    // Transaction variables
    private String[] tvTenderCPVList;

    private boolean skipExpiredTenders;
    private boolean skipTestTenders;

    public TenderParser() {
        skipTestTenders = true;
        skipExpiredTenders = true;
    }

    public void setRawData(String rawData) {
        this.rawData = rawData;
    }

    public void setSkipExpiredTenders(boolean skipExpiredTenders) {
        this.skipExpiredTenders = skipExpiredTenders;
    }

    public void setSkipTestTenders(boolean skipTestTenders) {
        this.skipTestTenders = skipTestTenders;
    }

    public void parseRawData() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        this.dataNode = objectMapper.readTree(rawData).get("data");

        defineAutocreatedLot();
        parseProcuringEntity();
        parseTender();
        parseTenderData();
        parseTenderComplaints();
        parseTenderDocuments();
        parseTenderEligibilityDocuments();
        parseTenderFinancialDocuments();
        parseTenderItems();
        parseTenderQuestions();
        parseTenderAwards();
        parseTenderContracts();
        parseTenderLots();
        parseTenderBids();
        parseTvTenderCpvList();
        parseTenderQualification();
    }

    public Tender buildTender() {
        tenderData.setTender(tender);
        tenderAwards.forEach(award -> award.setTender(tender));
        tenderItems.forEach(items -> items.setTender(tender));
        tenderLots.forEach(lot -> lot.setTender(tender));
        tenderBids.forEach(bid -> bid.setTender(tender));
        tenderQuestions.forEach(question -> question.setTender(tender));
        tenderQualifications.forEach(qualification -> qualification.setTender(tender));

        tender.setProcuringEntity(procuringEntity);
        tender.setDocuments(tenderDocuments);
        tender.setEligibilityDocuments(tenderEligibilityDocuments);
        tender.setFinancialDocuments(tenderFinancialDocuments);
        tender.setAwards(tenderAwards);
        tender.setData(tenderData);
        tender.setItems(tenderItems);
        tender.setTenderContracts(tenderContracts);
        tender.setBids(tenderBids);
        tender.setLots(tenderLots);
        tender.setQuestions(tenderQuestions);
        tender.setTvTenderCPVList(tvTenderCPVList);
        tender.setQualifications(tenderQualifications);

        tenderContracts.forEach(contract -> {
            contract.setTender(tender);
            tender.getAwards().forEach(award -> {
                if (award.getOuterId().equals(contract.getAwardId())) {
                    award.setTenderContract(contract);
                    contract.setAward(award);
                    if (contract.getSupplier() != null) {
                        if (contract.getSupplier().equals(award.getSupplier())) {
                            contract.setSupplier(award.getSupplier());
                        }
                    }
                }
            });
        });

        tenderDocuments.forEach(document -> {
            tender.getAwards().forEach(award -> {
                if (award.getOuterId().equals(document.getAwardOuterId())) {
                    document.setAward(award);
                }
            });
            tender.getTenderContracts().forEach(tenderContract -> {
                if (tenderContract.getOuterId().equals(document.getTenderContractOuterId())) {
                    document.setTenderContract(tenderContract);
                }
            });
            tender.getBids().forEach(bid -> {
                if (bid.getOuterId().equals(document.getBidOuterId())) {
                    document.setBid(bid);
                }
            });
            if (document.getType().equals("tender")) {
                document.setTender(tender);
            }
        });

        tenderEligibilityDocuments.forEach(eligibilityDocument -> {
            eligibilityDocument.setTender(tender);
            tender.getBids().forEach(bid -> {
                if (bid.getOuterId().equals(eligibilityDocument.getBidOuterId())) {
                    eligibilityDocument.setBid(bid);
                }
            });
        });

        tenderFinancialDocuments.forEach(financialDocument -> {
            financialDocument.setTender(tender);
            tender.getBids().forEach(bid -> {
                if (bid.getOuterId().equals(financialDocument.getBidOuterId())) {
                    financialDocument.setBid(bid);
                }
            });
        });

        Lot autocreatedLot = new Lot();
        autocreatedLot.setOuterId(AUTOCREATED_LOT);
        autocreatedLot.setTender(tender);
        autocreatedLot.setStatus(tender.getStatus());
        autocreatedLot.setAmount(tender.getAmount());
        autocreatedLot.setCurrency(tender.getCurrency());
        autocreatedLot.setGuaranteeAmount(tender.getGuaranteeAmount());
        autocreatedLot.setGuaranteeCurrency(tender.getGuaranteeCurrency());
        autocreatedLot.setItems(new ArrayList<>());
        autocreatedLot.setAwards(new ArrayList<>());
        autocreatedLot.setQualifications(new ArrayList<>());

        if (hasAutocreatedLot) {
            tenderItems.forEach(tenderItem -> {
                autocreatedLot.getItems().add(tenderItem);
                tenderItem.setLot(autocreatedLot);
            });
            tender.getLots().add(autocreatedLot);
        } else {
            tenderItems.forEach(tenderItem ->
                    tenderLots.forEach(tenderLot -> {
                        String relatedLotId = tenderItem.getRelatedLotId();
                        if (tenderLot.getOuterId().equals(relatedLotId)) {
                            tenderLot.getItems().add(tenderItem);
                            tenderItem.setLot(tenderLot);
                        }
                    })
            );
        }

        Map<String, Supplier> allSuppliersMap = new HashMap<>();

        tender.getBids().forEach(bid -> {
            Supplier supplier = bid.getSupplier();
            if (supplier != null) {
                String id = supplier.getIdentifierId() + supplier.getIdentifierScheme();
                allSuppliersMap.put(id, bid.getSupplier());
            }
        });

        tender.getAwards().forEach(award -> {
            Supplier supplier = award.getSupplier();
            if (supplier != null) {
                String id = supplier.getIdentifierId() + supplier.getIdentifierScheme();
                allSuppliersMap.put(id, award.getSupplier());
            }
        });

        tender.getTenderContracts().forEach(contract -> {
            Supplier supplier = contract.getSupplier();
            if (supplier != null) {
                String id = supplier.getIdentifierId() + supplier.getIdentifierScheme();
                allSuppliersMap.put(id, contract.getSupplier());
            }
        });

        //// Set unique suppliers
        tender.getBids().forEach(bid -> {
            Supplier supplier = bid.getSupplier();
            if (supplier != null) {
                String id = supplier.getIdentifierId() + supplier.getIdentifierScheme();
                bid.setSupplier(allSuppliersMap.get(id));
            }
        });

        tender.getAwards().forEach(award -> {
            Supplier supplier = award.getSupplier();
            if (supplier != null) {
                String id = supplier.getIdentifierId() + supplier.getIdentifierScheme();
                award.setSupplier(allSuppliersMap.get(id));
            }
        });

        tender.getTenderContracts().forEach(contract -> {
            Supplier supplier = contract.getSupplier();
            if (supplier != null) {
                String id = supplier.getIdentifierId() + supplier.getIdentifierScheme();
                contract.setSupplier(allSuppliersMap.get(id));
            }
        });


        tender.getBids().forEach(bid -> {
            bid.getRelatedLots().forEach(relatedLot -> {
                if (relatedLot != null && relatedLot.equals(AUTOCREATED_LOT)) {
                    bid.getLots().add(autocreatedLot);
                } else {
                    tenderLots.forEach(tenderLot -> {
                        if (tenderLot.getOuterId().equals(relatedLot)) {
                            bid.getLots().add(tenderLot);
                        }
                    });
                }
            });
        });

        tender.getQualifications().forEach(qualification -> {
            if (qualification.getLotId().equals(AUTOCREATED_LOT)) {
                qualification.setLot(autocreatedLot);
                autocreatedLot.getQualifications().add(qualification);
            } else {
                tenderLots.forEach(tenderLot -> {
                    if (tenderLot.getOuterId().equals(qualification.getLotId())) {
                        qualification.setLot(tenderLot);
                        tenderLot.getQualifications().add(qualification);
                    }
                });
            }
        });

        tenderLots.forEach(lot -> {
            Set<String> lotItemsCpvs = lot.getItems().stream()
                    .map(TenderItem::getClassificationId)
                    .collect(Collectors.toSet());

            List<String> uniqueCpvs = lotItemsCpvs.stream().map(cpv -> {
                if (cpv == null || cpv.isEmpty() || cpv.length() < 2) {
                    return cpv;
                }
                return cpv.substring(0, 2);
            }).distinct().collect(Collectors.toList());

            if (uniqueCpvs.size() == 1) {
                lot.setCpv2(uniqueCpvs.get(0));
            } else {
                lot.setCpv2(MIXED_LOT_CPV2);
            }

            List<Award> lotAwards = tenderAwards.stream()
                    .filter(award -> award.getLotId().equals(lot.getOuterId()))
                    .collect(Collectors.toList());

            lotAwards.forEach(lotAward -> lotAward.setLot(lot));
            lot.getAwards().addAll(lotAwards);
        });

        String tvProcuringEntity = String.join(DASH,
                procuringEntity.getIdentifierScheme(), procuringEntity.getIdentifierId());

        tender.setDateCreated(DateUtils.now());
        tender.setTvProcuringEntity(tvProcuringEntity);

        tender.getBids().forEach(bid -> {
            List<Award> bidAwards = tender.getAwards().stream()
                    .filter(award -> award.getBidId().equals(bid.getOuterId()))
                    .peek(award -> award.setBid(bid))
                    .collect(Collectors.toList());

            bid.setAwards(bidAwards);
        });

        return tender;
    }

    private void defineAutocreatedLot() {
        int size = dataNode.at("/lots").size();
        hasAutocreatedLot = size == 0;
    }

    private void parseProcuringEntity() {
        JsonNode node = dataNode.at("/procuringEntity");
        String scheme = node.at("/identifier/scheme").asText();
        String legalName = node.at("/identifier/legalName").asText();
        String id = node.at("/identifier/id").asText();
        String kind = node.at("/kind").asText();
        String region = JsonUtils.getString(node, "/address/region");

        procuringEntity = new ProcuringEntity();
        procuringEntity.setIdentifierId(id);
        procuringEntity.setIdentifierLegalName(legalName);
        procuringEntity.setIdentifierScheme(scheme);
        procuringEntity.setKind(kind);
        procuringEntity.setRegion(region);
    }

    private void parseTender() {
        String outerId = dataNode.at("/id").asText();
        String tenderId = dataNode.at("/tenderID").asText();
        String status = dataNode.at("/status").asText();
        String title = dataNode.at("/title").asText();
        String procurementMethodType = dataNode.at("/procurementMethodType").asText();
        String procurementMethod = dataNode.at("/procurementMethod").asText();
        String cause = JsonUtils.getString(dataNode, "/cause");
        Double amount = JsonUtils.getDouble(dataNode, "/value/amount");
        String currency = JsonUtils.getString(dataNode, "/value/currency");
        Double guaranteeAmount = JsonUtils.getDouble(dataNode, "/guarantee/amount");
        String guaranteeCurrency = JsonUtils.getString(dataNode, "/guarantee/currency");
        String procuringEntityKind = JsonUtils.getString(dataNode, "/procuringEntity/kind");
        ZonedDateTime dateModified = JsonUtils.getDate(dataNode, "/dateModified");
        ZonedDateTime date = JsonUtils.getDate(dataNode, "/date");
        ZonedDateTime startDate = JsonUtils.getDate(dataNode, "/tenderPeriod/startDate");
        ZonedDateTime endDate = JsonUtils.getDate(dataNode, "/tenderPeriod/endDate");
        ZonedDateTime enquiryStartDate = JsonUtils.getDate(dataNode, "/enquiryPeriod/startDate");
        ZonedDateTime enquiryEndDate = JsonUtils.getDate(dataNode, "/enquiryPeriod/endDate");
        ZonedDateTime awardStartDate = JsonUtils.getDate(dataNode, "/awardPeriod/startDate");
        ZonedDateTime awardEndDate = JsonUtils.getDate(dataNode, "/awardPeriod/endDate");

        validateTender(date);
        String procurementMethodRationale = procurementMethodType.equals("reporting") ?
                dataNode.at("/procurementMethodRationale").asText() :
                null;

        tender = new Tender();
        tender.setOuterId(outerId);
        tender.setTenderId(tenderId);
        tender.setStatus(status);
        tender.setTitle(title);
        tender.setDateModified(dateModified);
        tender.setDate(date);
        tender.setProcurementMethodType(procurementMethodType);
        tender.setProcurementMethod(procurementMethod);
        tender.setProcurementMethodRationale(procurementMethodRationale);
        tender.setCause(cause);
        tender.setAmount(amount);
        tender.setCurrency(currency);
        tender.setGuaranteeAmount(guaranteeAmount);
        tender.setGuaranteeCurrency(guaranteeCurrency);
        tender.setStartDate(startDate);
        tender.setEndDate(endDate);
        tender.setEnquiryStartDate(enquiryStartDate);
        tender.setEnquiryEndDate(enquiryEndDate);
        tender.setAwardStartDate(awardStartDate);
        tender.setAwardEndDate(awardEndDate);
        tender.setProcuringEntityKind(procuringEntityKind);
    }

    private void parseTenderData() {
        tenderData = new TenderData();
        tenderData.setData(rawData);
    }

    private void parseTenderComplaints() {
        List<Complaint> complaints = parseComplaints(dataNode);
        complaints.forEach(complaint -> complaint.setTender(tender));
        tender.setComplaints(complaints);
    }

    private void parseTenderDocuments() {
        tenderDocuments = new ArrayList<>();
        Map<String, Document> documents = new HashMap<>();
        for (JsonNode documentNode : dataNode.at("/documents")) {
            String outerId = documentNode.at("/id").asText();
            String format = documentNode.at("/format").asText();
            String author = JsonUtils.getString(documentNode, "/author");
            String documentOf = JsonUtils.getString(documentNode, "/documentOf");
            String relatedItem = JsonUtils.getString(documentNode, "/relatedItem");
            ZonedDateTime dateModified = JsonUtils.getDate(documentNode, "/dateModified");
            ZonedDateTime datePublished = JsonUtils.getDate(documentNode, "/datePublished");

            Document document = new Document();
            document.setOuterId(outerId);
            document.setFormat(format);
            document.setType("tender");
            document.setDateModified(dateModified);
            document.setAuthor(author);
            document.setDocumentOf(documentOf);
            document.setDatePublished(datePublished);
            document.setRelatedItem(relatedItem);

            Document existingDocument = documents.get(document.getAwardOuterId());
            if (existingDocument != null) {
                if (document.getDateModified().isAfter(existingDocument.getDateModified())) {
                    documents.put(document.getOuterId(), document);
                }
            } else {
                documents.put(document.getOuterId(), document);
            }
        }

        Map<String, Document> awardDocuments = new HashMap<>();
        for (JsonNode awardNode : dataNode.at("/awards")) {
            String awardOuterId = awardNode.at("/id").asText();
            for (JsonNode documentNode : awardNode.at("/documents")) {
                String outerId = documentNode.at("/id").asText();
                String format = documentNode.at("/format").asText();
                String author = JsonUtils.getString(documentNode, "/author");
                String documentOf = JsonUtils.getString(documentNode, "/documentOf");
                String relatedItem = JsonUtils.getString(documentNode, "/relatedItem");
                ZonedDateTime dateModified = JsonUtils.getDate(documentNode, "/dateModified");
                ZonedDateTime datePublished = JsonUtils.getDate(documentNode, "/datePublished");

                Document document = new Document();
                document.setOuterId(outerId);
                document.setFormat(format);
                document.setType("award");
                document.setAwardOuterId(awardOuterId);
                document.setDateModified(dateModified);
                document.setDatePublished(datePublished);
                document.setAuthor(author);
                document.setDocumentOf(documentOf);
                document.setRelatedItem(relatedItem);

                Document existingDocument = awardDocuments.get(document.getAwardOuterId());
                if (existingDocument != null) {
                    if (document.getDateModified().isAfter(existingDocument.getDateModified())) {
                        awardDocuments.put(document.getOuterId(), document);
                    }
                } else {
                    awardDocuments.put(document.getOuterId(), document);
                }
            }
        }

        Map<String, Document> contractDocuments = new HashMap<>();
        for (JsonNode contractNode : dataNode.at("/contracts")) {
            String tenderContractOuterId = contractNode.at("/id").asText();
            for (JsonNode documentNode : contractNode.at("/documents")) {
                String outerId = documentNode.at("/id").asText();
                String format = documentNode.at("/format").asText();
                String author = JsonUtils.getString(documentNode, "/author");
                String documentOf = JsonUtils.getString(documentNode, "/documentOf");
                String relatedItem = JsonUtils.getString(documentNode, "/relatedItem");
                ZonedDateTime dateModified = JsonUtils.getDate(documentNode, "/dateModified");
                ZonedDateTime datePublished = JsonUtils.getDate(documentNode, "/datePublished");

                Document document = new Document();
                document.setOuterId(outerId);
                document.setFormat(format);
                document.setType("contract");
                document.setTenderContractOuterId(tenderContractOuterId);
                document.setDateModified(dateModified);
                document.setDatePublished(datePublished);
                document.setAuthor(author);
                document.setDocumentOf(documentOf);
                document.setRelatedItem(relatedItem);

                Document existingDocument = contractDocuments.get(document.getTenderContractOuterId());
                if (existingDocument != null) {
                    if (document.getDateModified().isAfter(existingDocument.getDateModified())) {
                        contractDocuments.put(document.getOuterId(), document);
                    }
                } else {
                    contractDocuments.put(document.getOuterId(), document);
                }
            }
        }

        Map<String, Document> bidDocuments = new HashMap<>();
        for (JsonNode bidNode : dataNode.at("/bids")) {
            String bidOuterId = bidNode.at("/id").asText();
            for (JsonNode documentNode : bidNode.at("/documents")) {
                String outerId = documentNode.at("/id").asText();
                String format = documentNode.at("/format").asText();
                String author = JsonUtils.getString(documentNode, "/author");
                String documentOf = JsonUtils.getString(documentNode, "/documentOf");
                String relatedItem = JsonUtils.getString(documentNode, "/relatedItem");
                ZonedDateTime dateModified = JsonUtils.getDate(documentNode, "/dateModified");
                ZonedDateTime datePublished = JsonUtils.getDate(documentNode, "/datePublished");

                Document document = new Document();
                document.setOuterId(outerId);
                document.setFormat(format);
                document.setType("bid");
                document.setBidOuterId(bidOuterId);
                document.setDateModified(dateModified);
                document.setDatePublished(datePublished);
                document.setAuthor(author);
                document.setDocumentOf(documentOf);
                document.setRelatedItem(relatedItem);

                Document existingDocument = bidDocuments.get(document.getBidOuterId());
                if (existingDocument != null) {
                    if (document.getDateModified().isAfter(existingDocument.getDateModified())) {
                        bidDocuments.put(document.getOuterId(), document);
                    }
                } else {
                    bidDocuments.put(document.getOuterId(), document);
                }
            }
        }

        tenderDocuments.addAll(documents.values());
        tenderDocuments.addAll(awardDocuments.values());
        tenderDocuments.addAll(contractDocuments.values());
        tenderDocuments.addAll(bidDocuments.values());
    }

    private void parseTenderEligibilityDocuments() {
        tenderEligibilityDocuments = new ArrayList<>();
        Map<String, EligibilityDocument> documents = new HashMap<>();
        for (JsonNode bidNode : dataNode.at("/bids")) {
            String bidOuterId = bidNode.at("/id").asText();
            for (JsonNode documentNode : bidNode.at("/eligibilityDocuments")) {
                String outerId = documentNode.at("/id").asText();
                String format = documentNode.at("/format").asText();
                String author = JsonUtils.getString(documentNode, "/author");
                String documentOf = JsonUtils.getString(documentNode, "/documentOf");
                String relatedItem = JsonUtils.getString(documentNode, "/relatedItem");
                ZonedDateTime dateModified = JsonUtils.getDate(documentNode, "/dateModified");
                ZonedDateTime datePublished = JsonUtils.getDate(documentNode, "/datePublished");

                EligibilityDocument document = new EligibilityDocument();
                document.setOuterId(outerId);
                document.setFormat(format);
                document.setType("bid");
                document.setBidOuterId(bidOuterId);
                document.setDateModified(dateModified);
                document.setDatePublished(datePublished);
                document.setAuthor(author);
                document.setDocumentOf(documentOf);
                document.setRelatedItem(relatedItem);

                EligibilityDocument existingDocument = documents.get(document.getBidOuterId());
                if (existingDocument != null) {
                    if (document.getDateModified().isAfter(existingDocument.getDateModified())) {
                        documents.put(document.getOuterId(), document);
                    }
                } else {
                    documents.put(document.getOuterId(), document);
                }
            }
        }

        tenderEligibilityDocuments.addAll(documents.values());
    }

    private void parseTenderFinancialDocuments() {
        tenderFinancialDocuments = new ArrayList<>();
        Map<String, FinancialDocument> documents = new HashMap<>();
        for (JsonNode bidNode : dataNode.at("/bids")) {
            String bidOuterId = bidNode.at("/id").asText();
            for (JsonNode documentNode : bidNode.at("/financialDocuments")) {
                String outerId = documentNode.at("/id").asText();
                String format = documentNode.at("/format").asText();
                String author = JsonUtils.getString(documentNode, "/author");
                String documentOf = JsonUtils.getString(documentNode, "/documentOf");
                String relatedItem = JsonUtils.getString(documentNode, "/relatedItem");
                ZonedDateTime dateModified = JsonUtils.getDate(documentNode, "/dateModified");
                ZonedDateTime datePublished = JsonUtils.getDate(documentNode, "/datePublished");

                FinancialDocument document = new FinancialDocument();
                document.setOuterId(outerId);
                document.setFormat(format);
                document.setType("bid");
                document.setBidOuterId(bidOuterId);
                document.setDateModified(dateModified);
                document.setDatePublished(datePublished);
                document.setAuthor(author);
                document.setDocumentOf(documentOf);
                document.setRelatedItem(relatedItem);

                FinancialDocument existingDocument = documents.get(document.getBidOuterId());
                if (existingDocument != null) {
                    if (document.getDateModified().isAfter(existingDocument.getDateModified())) {
                        documents.put(document.getOuterId(), document);
                    }
                } else {
                    documents.put(document.getOuterId(), document);
                }
            }
        }

        tenderFinancialDocuments.addAll(documents.values());
    }

    private void parseTenderItems() {
        tenderItems = new ArrayList<>();
        JsonNode node = dataNode.at("/items");
        for (JsonNode itemNode : node) {
            String id = itemNode.at("/classification/id").asText();
            String outerId = itemNode.at("/id").asText();
            String relatedLotId = JsonUtils.getString(itemNode, "/relatedLot");
            Double quantity = JsonUtils.getDouble(itemNode, "/quantity");

            if (relatedLotId == null && hasAutocreatedLot) {
                relatedLotId = AUTOCREATED_LOT;
            }

            TenderItem tenderItem = new TenderItem();
            tenderItem.setClassificationId(id);
            tenderItem.setOuterId(outerId);
            tenderItem.setRelatedLotId(relatedLotId);
            tenderItem.setQuantity(quantity);
            tenderItems.add(tenderItem);
        }
    }

    private void parseTenderQuestions() {
        tenderQuestions = new ArrayList<>();
        JsonNode node = dataNode.at("/questions");
        for (JsonNode questionNode : node) {
            ZonedDateTime date = JsonUtils.getDate(questionNode, "/date");
            ZonedDateTime dateAnswered = JsonUtils.getDate(questionNode, "/dateAnswered");
            String answer = JsonUtils.getString(questionNode, "/answer");

            Question question = new Question();
            question.setDate(date);
            question.setDateAnswered(dateAnswered);
            question.setAnswer(answer);
            tenderQuestions.add(question);
        }
    }

    private void parseTenderAwards() {
        tenderAwards = new ArrayList<>();
        JsonNode node = dataNode.at("/awards");
        List<Supplier> suppliers = new ArrayList<>();
        for (JsonNode itemNode : node) {

            String outerId = itemNode.at("/id").asText();
            String status = itemNode.at("/status").asText();
            String currency = itemNode.at("/value/currency").asText();
            Double amount = itemNode.at("/value/amount").asDouble();
            ZonedDateTime date = JsonUtils.getDate(itemNode, "/date");
            String lotId = JsonUtils.getString(itemNode, "/lotID");
            String bidId = JsonUtils.getString(itemNode, "/bid_id");

            if (lotId == null) {
                lotId = AUTOCREATED_LOT;
            }

            Supplier supplier = parseSupplier(itemNode);
            if (suppliers.contains(supplier)) {
                int supplierIndex = suppliers.indexOf(supplier);
                supplier = suppliers.get(supplierIndex);
            } else {
                suppliers.add(supplier);
            }

            List<Complaint> complaints = parseComplaints(itemNode);

            Award award = new Award();
            award.setOuterId(outerId);
            award.setStatus(status);
            award.setSupplier(supplier);
            award.setDate(date);
            award.setAmount(amount);
            award.setCurrency(currency);
            award.setLotId(lotId);
            award.setComplaints(complaints);
            award.setBidId(bidId);

            if (supplier != null) {
                award.setSupplierIdentifierId(supplier.getIdentifierId());
                award.setSupplierIdentifierScheme(supplier.getIdentifierScheme());
                award.setSupplierIdentifierLegalName(supplier.getIdentifierLegalName());
                award.setSupplierEmail(supplier.getEmail());
                award.setSupplierTelephone(supplier.getTelephone());
            }

            complaints.forEach(complaint -> complaint.setAward(award));

            tenderAwards.add(award);
        }
    }

    private void parseTenderContracts() {
        tenderContracts = new ArrayList<>();
        JsonNode node = dataNode.at("/contracts");
        for (JsonNode itemNode : node) {
            String contractId = JsonUtils.getString(itemNode, "/contractID");
            String outerId = itemNode.at("/id").asText();
            String awardId = itemNode.at("/awardID").asText();
            String status = itemNode.at("/status").asText();
            Double amount = JsonUtils.getDouble(itemNode, "/value/amount");
            String currency = JsonUtils.getString(itemNode, "/value/currency");
            ZonedDateTime date = JsonUtils.getDate(itemNode, "/date");
            ZonedDateTime dateSigned = JsonUtils.getDate(itemNode, "/dateSigned");

            Supplier supplier = parseSupplier(itemNode);

            TenderContract tenderContract = new TenderContract();
            tenderContract.setOuterId(outerId);
            tenderContract.setContractId(contractId);
            tenderContract.setStatus(status);
            tenderContract.setAwardId(awardId);
            tenderContract.setSupplier(supplier);
            tenderContract.setAmount(amount);
            tenderContract.setCurrency(currency);
            tenderContract.setDate(date);
            tenderContract.setDateSigned(dateSigned);

            if (supplier != null) {
                tenderContract.setSupplierIdentifierId(supplier.getIdentifierId());
                tenderContract.setSupplierIdentifierScheme(supplier.getIdentifierScheme());
                tenderContract.setSupplierEmail(supplier.getEmail());
                tenderContract.setSupplierTelephone(supplier.getTelephone());
            }

            Set<String> cpvs = new HashSet<>();
            for (JsonNode contractItemNode : itemNode.at("/items")) {
                String cpv = JsonUtils.getString(contractItemNode, "/classification/id");
                if (cpv != null) {
                    cpvs.add(cpv);
                }
            }
            tenderContract.setContractCpvList(
                    cpvs.toArray(new String[cpvs.size()]));

            tenderContracts.add(tenderContract);
        }
    }

    private void parseTenderLots() {
        tenderLots = new ArrayList<>();
        for (JsonNode lotNode : dataNode.at("/lots")) {
            String id = lotNode.at("/id").asText();
            String status = lotNode.at("/status").asText();
            Double amount = lotNode.at("/value/amount").asDouble();
            String currency = lotNode.at("/value/currency").asText();
            Double guaranteeAmount = JsonUtils.getDouble(lotNode, "/guarantee/amount");
            String guaranteeCurrency = JsonUtils.getString(lotNode, "/guarantee/currency");

            Lot lot = new Lot();
            lot.setOuterId(id);
            lot.setStatus(status);
            lot.setAmount(amount);
            lot.setCurrency(currency);
            lot.setGuaranteeAmount(guaranteeAmount);
            lot.setGuaranteeCurrency(guaranteeCurrency);
            lot.setItems(new ArrayList<>());
            lot.setAwards(new ArrayList<>());
            lot.setQualifications(new ArrayList<>());

            tenderLots.add(lot);
        }
    }

    private void parseTenderBids() {
        tenderBids = new ArrayList<>();
        JsonNode node = dataNode.at("/bids");
        for (JsonNode bidNode : node) {
            String outerId = bidNode.at("/id").asText();
            String status = bidNode.at("/status").asText();

            List<String> relatedLots = new ArrayList<>();
            if (hasAutocreatedLot) {
                relatedLots.add(AUTOCREATED_LOT);
            } else {
                for (JsonNode lotValue : bidNode.at("/lotValues")) {
                    String relatedLot = JsonUtils.getString(lotValue, "/relatedLot");
                    if (relatedLot != null) {
                        relatedLots.add(relatedLot);
                    }
                }
            }

            Bid bid = new Bid();
            for (JsonNode supplierNode : bidNode.at("/tenderers")) {
                String identifierId = supplierNode.at("/identifier/id").asText();
                String identifierScheme = supplierNode.at("/identifier/scheme").asText();
                String identifierLegalName = supplierNode.at("/identifier/legalName").asText();
                String telephone = JsonUtils.getString(supplierNode, "/contactPoint/telephone");
                String email = JsonUtils.getString(supplierNode, "/contactPoint/email");

                Supplier supplier = new Supplier();
                supplier.setIdentifierId(identifierId);
                supplier.setIdentifierScheme(identifierScheme);
                supplier.setIdentifierLegalName(identifierLegalName);
                supplier.setTelephone(telephone);
                supplier.setEmail(email);

                bid.setSupplier(supplier);
                bid.setSupplierIdentifierId(supplier.getIdentifierId());
                bid.setSupplierIdentifierScheme(supplier.getIdentifierScheme());
                bid.setSupplierIdentifierLegalName(supplier.getIdentifierLegalName());
                bid.setSupplierEmail(supplier.getEmail());
                bid.setSupplierTelephone(supplier.getTelephone());
            }

            bid.setOuterId(outerId);
            bid.setStatus(status);
            bid.setRelatedLots(relatedLots);
            bid.setLots(new ArrayList<>());

            tenderBids.add(bid);
        }
    }

    private void parseTenderQualification() {
        tenderQualifications = new ArrayList<>();
        JsonNode node = dataNode.at("/qualifications");
        for (JsonNode qualificationNode : node) {
            String outerId = qualificationNode.at("/id").asText();
            String status = qualificationNode.at("/status").asText();
            Boolean eligible = qualificationNode.at("/eligible").asBoolean();
            Boolean qualified = qualificationNode.at("/qualified").asBoolean();
            ZonedDateTime date = JsonUtils.getDate(qualificationNode, "/date");

            Qualification qualification = new Qualification();
            qualification.setOuterId(outerId);
            qualification.setStatus(status);
            qualification.setEligible(eligible);
            qualification.setQualified(qualified);
            qualification.setDate(date);

            String lotId = JsonUtils.getString(qualificationNode, "/lotID");
            if (lotId != null) {
                qualification.setLotId(lotId);
            } else {
                if (hasAutocreatedLot) {
                    qualification.setLotId(AUTOCREATED_LOT);
                }
            }
            tenderQualifications.add(qualification);
        }
    }

    private void parseTvTenderCpvList() {
        if (tenderItems.isEmpty()) {
            return;
        }

        Set<String> rawTvTenderCPVList = tenderItems.stream()
                .map(TenderItem::getClassificationId)
                .collect(Collectors.toSet());

        tvTenderCPVList = rawTvTenderCPVList.toArray(new String[rawTvTenderCPVList.size()]);
    }

    private Supplier parseSupplier(JsonNode node) {
        Supplier supplier = null;
        for (JsonNode supplierNode : node.at("/suppliers")) {
            String id = supplierNode.at("/identifier/id").asText();
            String legalName = supplierNode.at("/identifier/legalName").asText();
            String scheme = supplierNode.at("/identifier/scheme").asText();
            String telephone = JsonUtils.getString(supplierNode, "/contactPoint/telephone");
            String email = JsonUtils.getString(supplierNode, "/contactPoint/email");

            supplier = new Supplier();
            supplier.setIdentifierId(id);
            supplier.setIdentifierLegalName(legalName);
            supplier.setIdentifierScheme(scheme);
            supplier.setTelephone(telephone);
            supplier.setEmail(email);
        }
        return supplier;
    }

    private List<Complaint> parseComplaints(JsonNode node) {
        List<Complaint> complaints = new ArrayList<>();
        for (JsonNode complaintNode : node.at("/complaints")) {
            String outerId = complaintNode.at("/id").asText();
            String complaintId = complaintNode.at("/complaintID").asText();
            String status = complaintNode.at("/status").asText();
            String type = complaintNode.at("/type").asText();
            ZonedDateTime date = JsonUtils.getDate(complaintNode, "/date");

            Complaint complaint = new Complaint();
            complaint.setOuterId(outerId);
            complaint.setComplaintId(complaintId);
            complaint.setStatus(status);
            complaint.setComplaintType(type);
            complaint.setDate(date);
            complaints.add(complaint);
        }
        return complaints;
    }

    private void validateTender(ZonedDateTime date) {

        if (skipExpiredTenders && date == null) {
            throw new TenderDateNotFoundException();
        }

        if (skipExpiredTenders && date.isBefore(DateUtils.yearEarlierFromNow())) {
            throw new TenderValidationException("Tender expired");
        }

        if (skipTestTenders && dataNode.at("/mode").asText().equals("test")) {
            throw new TenderValidationException("Test tender");
        }
    }
}
