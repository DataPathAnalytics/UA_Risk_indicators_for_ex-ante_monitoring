package com.datapath.integration.parsers.impl;

import com.datapath.integration.domain.AgreementResponseEntity;
import com.datapath.integration.utils.JsonUtils;
import com.datapath.persistence.entities.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;

public class AgreementParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Agreement parse(AgreementResponseEntity response) throws IOException {
        Agreement agreement = new Agreement();

        JsonNode root = objectMapper.readTree(response.getData()).get("data");

        agreement.setOuterId(response.getId());
        agreement.setAgreementId(JsonUtils.getString(root, "/agreementID"));
        agreement.setStatus(JsonUtils.getString(root, "/status"));
        agreement.setNumber(JsonUtils.getString(root, "/agreementNumber"));
        agreement.setTenderOuterId(JsonUtils.getString(root, "/tender_id"));
        agreement.setOwner(JsonUtils.getString(root, "/owner"));

        agreement.setDateSigned(JsonUtils.getDate(root, "/dateSigned"));
        agreement.setDateModified(JsonUtils.getDate(root, "/dateModified"));
        agreement.setStartDate(JsonUtils.getDate(root, "/period/startDate"));
        agreement.setEndDate(JsonUtils.getDate(root, "/period/endDate"));

        agreement.setDocuments(parseDocuments(root.get("documents")));
        agreement.getDocuments().forEach(d -> d.setAgreement(agreement));
        agreement.setItems(parseItems(root.get("items")));
        agreement.getItems().forEach(i -> i.setAgreement(agreement));
        agreement.setContracts(parseContract(root.get("contracts"), agreement.getItems()));
        agreement.getContracts().forEach(c -> c.setAgreement(agreement));

        return agreement;
    }

    private static List<AgreementContract> parseContract(JsonNode contractNode, List<AgreementItem> items) {
        if (isNull(contractNode)) return emptyList();

        List<AgreementContract> contracts = new LinkedList<>();
        for (JsonNode node : contractNode) {
            AgreementContract contract = new AgreementContract();

            contract.setOuterId(JsonUtils.getString(node, "/id"));
            contract.setStatus(JsonUtils.getString(node, "/status"));
            contract.setDate(JsonUtils.getDate(node, "/date"));
            contract.setBidId(JsonUtils.getString(node, "/bidID"));
            contract.setAwardId(JsonUtils.getString(node, "/awardID"));

            contract.setUnitPrices(parseUnitPrice(node.get("unitPrices"), items));
            contract.getUnitPrices().forEach(u -> u.setContract(contract));

            contract.setSuppliers(parseSupplier(node.get("suppliers")));
            contract.getSuppliers().forEach(s -> s.setContracts(Arrays.asList(contract)));

            contracts.add(contract);
        }
        return contracts;
    }

    private static List<AgreementSupplier> parseSupplier(JsonNode supplierNode) {
        if (isNull(supplierNode)) return emptyList();

        List<AgreementSupplier> suppliers = new LinkedList<>();
        for (JsonNode node : supplierNode) {
            AgreementSupplier supplier = new AgreementSupplier();
            supplier.setTelephone(JsonUtils.getString(node, "/contactPoint/telephone"));
            supplier.setEmail(JsonUtils.getString(node, "/contactPoint/telephone"));
            supplier.setIdentifierId(JsonUtils.getString(node, "/identifier/id"));
            supplier.setIdentifierLegalName(JsonUtils.getString(node, "/identifier/legalName"));
            supplier.setIdentifierScheme(JsonUtils.getString(node, "/identifier/scheme"));
            suppliers.add(supplier);
        }
        return suppliers;
    }

    private static List<UnitPrice> parseUnitPrice(JsonNode unitPriceNode, List<AgreementItem> items) {
        if (isNull(unitPriceNode)) return emptyList();

        List<UnitPrice> unitPrices = new LinkedList<>();
        for (JsonNode node : unitPriceNode) {
            UnitPrice unitPrice = new UnitPrice();
            unitPrice.setAmount(JsonUtils.getDouble(node, "/value/amount"));
            unitPrice.setCurrency(JsonUtils.getString(node, "/value/currency"));
            unitPrice.setValueAddedTaxIncluded(node.at("/value/currency").booleanValue());

            String relatedItem = JsonUtils.getString(node, "/relatedItem");
            AgreementItem item = items.stream().filter(i -> Objects.equals(relatedItem, i.getOuterId())).findFirst().orElse(null);
            unitPrice.setItem(item);

            unitPrices.add(unitPrice);
        }
        return unitPrices;
    }

    private static List<AgreementItem> parseItems(JsonNode itemNode) {
        if (isNull(itemNode)) return emptyList();

        List<AgreementItem> items = new LinkedList<>();
        for (JsonNode node : itemNode) {
            AgreementItem item = new AgreementItem();
            item.setOuterId(JsonUtils.getString(node, "/id"));
            item.setQuantity(JsonUtils.getDouble(node, "/quantity"));
            item.setUnitCode(JsonUtils.getString(node, "/unit/code"));
            item.setUnitName(JsonUtils.getString(node, "/unit/name"));
            item.setDeliveryEndDate(JsonUtils.getDate(node, "/deliveryDate/endDate"));
            item.setClassificationId(JsonUtils.getString(node, "/classification/id"));
            item.setClassificationScheme(JsonUtils.getString(node, "/classification/scheme"));
            item.setClassificationDescription(JsonUtils.getString(node, "/classification/description"));
            item.setRelatedLotId(JsonUtils.getString(node, "/relatedLot"));
            items.add(item);
        }
        return items;
    }

    private static List<AgreementDocument> parseDocuments(JsonNode documentNode) {
        if (isNull(documentNode)) return emptyList();

        List<AgreementDocument> documents = new LinkedList<>();
        for (JsonNode node : documentNode) {
            AgreementDocument document = new AgreementDocument();
            document.setOuterId(JsonUtils.getString(node, "/id"));
            document.setFormat(JsonUtils.getString(node, "/format"));
            document.setTitle(JsonUtils.getString(node, "/title"));
            document.setDocumentOf(JsonUtils.getString(node, "/documentOf"));
            document.setDocumentType(JsonUtils.getString(node, "/documentType"));
            document.setDatePublished(JsonUtils.getDate(node, "/datePublished"));
            document.setDateModified(JsonUtils.getDate(node, "/dateModified"));
            documents.add(document);
        }
        return documents;
    }
}
