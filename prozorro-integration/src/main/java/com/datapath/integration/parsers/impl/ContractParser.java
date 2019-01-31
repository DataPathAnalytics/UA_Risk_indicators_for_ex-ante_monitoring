package com.datapath.integration.parsers.impl;

import com.datapath.integration.parsers.EntityParser;
import com.datapath.integration.utils.DateUtils;
import com.datapath.integration.utils.JsonUtils;
import com.datapath.persistence.entities.Contract;
import com.datapath.persistence.entities.ContractChange;
import com.datapath.persistence.entities.ContractDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;


public class ContractParser implements EntityParser {

    private JsonNode dataNode;
    private Contract contract;
    private List<ContractChange> contractChanges;
    private List<ContractDocument> contractDocuments;

    private ContractParser(String rawData) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        this.dataNode = objectMapper.readTree(rawData).get("data");
    }

    public static ContractParser create(String rawData) throws IOException {
        final ContractParser contractParser = new ContractParser(rawData);
        contractParser.parseContract();
        contractParser.parseContractChanges();
        contractParser.parseContractDocuments();

        return contractParser;
    }

    public Contract buildContractEntity() {
        contract.setChanges(contractChanges);
        contractChanges.forEach(change -> change.setContract(contract));
        contract.setDocuments(contractDocuments);
        contractDocuments.forEach(document -> document.setContract(contract));
        contract.setDateCreated(DateUtils.now());

        return contract;
    }

    private void parseContract() {
        String outerId = dataNode.at("/id").asText();
        String dateModifiedStr = dataNode.at("/dateModified").asText();
        String status = dataNode.at("/status").asText();
        String tenderId = dataNode.at("/tender_id").asText();
        String contractId = dataNode.at("/contractID").asText();
        Double amount = dataNode.at("/value/amount").asDouble();
        ZonedDateTime dateModified = DateUtils.parseZonedDateTime(dateModifiedStr);
        ZonedDateTime dateSigned = JsonUtils.getDate(dataNode, "/dateSigned");

        Contract contract = new Contract();
        contract.setOuterId(outerId);
        contract.setStatus(status);
        contract.setTenderOuterId(tenderId);
        contract.setContractId(contractId);
        contract.setDateModified(dateModified);
        contract.setAmount(amount);
        contract.setDateSigned(dateSigned);

        this.contract = contract;
    }

    private void parseContractChanges() {
        contractChanges = new ArrayList<>();
        for (JsonNode node : dataNode.at("/changes")) {
            String outerId = node.at("/id").asText();
            String status = node.at("/status").asText();
            ZonedDateTime dateSigned = JsonUtils.getDate(node, "/dateSigned");
            List<String> rationaleTypes = new ArrayList<>();
            for (JsonNode rationaleType : node.at("/rationaleTypes")) {
                rationaleTypes.add(rationaleType.asText());
            }

            ContractChange contractChange = new ContractChange();
            contractChange.setOuterId(outerId);
            contractChange.setStatus(status);
            contractChange.setDateSigned(dateSigned);
            contractChange.setRationaleTypes(rationaleTypes.toArray(new String[0]));
            contractChanges.add(contractChange);
        }
    }

    private void parseContractDocuments() {
        contractDocuments = new ArrayList<>();
        for (JsonNode node : dataNode.at("/documents")) {
            String outerId = node.at("/id").asText();
            String format = JsonUtils.getString(node, "/format");
            String title = JsonUtils.getString(node, "/title");
            String documentOf = JsonUtils.getString(node, "/documentOf");
            String documentType = JsonUtils.getString(node, "/documentType");
            ZonedDateTime datePublished = JsonUtils.getDate(node, "/datePublished");
            ZonedDateTime dateModified = JsonUtils.getDate(node, "/dateModified");

            ContractDocument document = new ContractDocument();
            document.setOuterId(outerId);
            document.setFormat(format);
            document.setTitle(title);
            document.setDocumentOf(documentOf);
            document.setDocumentType(documentType);
            document.setDatePublished(datePublished);
            document.setDateModified(dateModified);
            contractDocuments.add(document);
        }
    }

}
