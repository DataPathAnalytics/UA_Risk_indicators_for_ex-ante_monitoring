package com.datapath.integration.parsers.impl;

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
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static org.springframework.util.CollectionUtils.isEmpty;


public class ContractParser {

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
        String contractNumber = JsonUtils.getString(dataNode, "/contractNumber");
        ZonedDateTime dateModified = DateUtils.parseZonedDateTime(dateModifiedStr);
        ZonedDateTime dateSigned = JsonUtils.getDate(dataNode, "/dateSigned");

        Double amount = dataNode.at("/value/amount").asDouble();
        String currency = JsonUtils.getString(dataNode, "/value/currency");
        Double amountNet = JsonUtils.getDouble(dataNode, "/value/amountNet");
        Boolean valueAddedTaxIncluded = dataNode.at("/value/valueAddedTaxIncluded").asBoolean();
        Double paidAmount = JsonUtils.getDouble(dataNode, "/amountPaid/amount");
        String paidCurrency = JsonUtils.getString(dataNode, "/amountPaid/currency");
        Double paidAmountNet = JsonUtils.getDouble(dataNode, "/amountPaid/amountNet");
        Boolean paidValueAddedTaxIncluded = dataNode.at("/amountPaid/valueAddedTaxIncluded").asBoolean();

        ZonedDateTime periodStartDate = JsonUtils.getDate(dataNode, "/period/startDate");
        ZonedDateTime periodEndDate = JsonUtils.getDate(dataNode, "/period/endDate");
        String terminationDetails = JsonUtils.getString(dataNode, "/terminationDetails");

        Contract contract = new Contract();
        contract.setOuterId(outerId);
        contract.setStatus(status);
        contract.setTenderOuterId(tenderId);
        contract.setContractId(contractId);
        contract.setDateModified(dateModified);
        contract.setDateSigned(dateSigned);
        contract.setContractNumber(contractNumber);

        contract.setAmount(amount);
        contract.setCurrency(currency);
        contract.setAmountNet(amountNet);
        contract.setValueAddedTaxIncluded(valueAddedTaxIncluded);

        contract.setPaidAmount(paidAmount);
        contract.setPaidCurrency(paidCurrency);
        contract.setPaidAmountNet(paidAmountNet);
        contract.setPaidValueAddedTaxIncluded(paidValueAddedTaxIncluded);

        contract.setPeriodStartDate(periodStartDate);
        contract.setPeriodEndDate(periodEndDate);
        contract.setTerminationDetails(terminationDetails);

        this.contract = contract;
    }

    private void parseContractChanges() {
        contractChanges = new ArrayList<>();
        for (JsonNode node : dataNode.at("/changes")) {
            String outerId = node.at("/id").asText();
            String status = node.at("/status").asText();
            ZonedDateTime dateSigned = JsonUtils.getDate(node, "/dateSigned");
            ZonedDateTime date = JsonUtils.getDate(node, "/date");
            List<String> rationaleTypes = new ArrayList<>();
            for (JsonNode rationaleType : node.at("/rationaleTypes")) {
                rationaleTypes.add(rationaleType.asText());
            }

            ContractChange contractChange = new ContractChange();
            contractChange.setOuterId(outerId);
            contractChange.setStatus(status);
            contractChange.setDate(date);
            contractChange.setDateSigned(dateSigned);
            contractChange.setRationaleTypes(rationaleTypes.toArray(new String[0]));
            contractChanges.add(contractChange);
        }
    }

    private void parseContractDocuments() {
        contractDocuments = new ArrayList<>();
        List<ContractDocument> documents = new ArrayList<>();
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

            String relatedItem = JsonUtils.getString(node, "/relatedItem");

            if (relatedItem != null) {
                connectDocumentWithChange(document, relatedItem);
            }

            documents.add(document);
        }

        Map<String, List<ContractDocument>> groupDocumentByOuterId = documents
                .stream()
                .collect(groupingBy(ContractDocument::getOuterId));

        for (Map.Entry<String, List<ContractDocument>> entry : groupDocumentByOuterId.entrySet()) {
            entry.getValue()
                    .stream()
                    .max(Comparator.comparing(ContractDocument::getDateModified))
                    .ifPresent(contractDocuments::add);
        }
    }

    private void connectDocumentWithChange(ContractDocument document, String relatedItem) {
        contractChanges.stream()
                .filter(change -> relatedItem.equalsIgnoreCase(change.getOuterId()))
                .findFirst()
                .ifPresent(change -> {
                    if (isEmpty(change.getDocuments())) {
                        change.setDocuments(new ArrayList<>());
                    }
                    document.setChange(change);
                    change.getDocuments().add(document);
                });
    }

}
