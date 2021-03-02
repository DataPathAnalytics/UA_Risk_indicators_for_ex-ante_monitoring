package com.datapath.integration.validation;

import com.datapath.integration.utils.EntitySource;
import com.datapath.integration.utils.ValidationUtils;
import com.datapath.persistence.entities.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class TenderDataValidator {

    private static final String TEST = "test";
    private static final String TESTING = "ТЕСТУВАННЯ";
    private static final String PRICE_QUOTATION = "priceQuotation";

    @Value("${prozorro.tenders.skip-test}")
    private boolean skipTestTenders;

    public boolean isProcessable(Tender tender) {
        if (TEST.equalsIgnoreCase(tender.getMode()) && skipTestTenders) {
            log.info("Tender [{}] has test mode", tender.getOuterId());
            return false;
        }

        if (tender.getTitle().contains(TESTING)) {
            log.info("Tender has invalid part in title");
            return false;
        }

        if (tender.getProcurementMethodType().equalsIgnoreCase(PRICE_QUOTATION)) {
            return false;
        }

        if (tender.getSource().equalsIgnoreCase(EntitySource.TENDERING.toString())) {
            return tender.getDate() != null; //&& tender.getDate().isAfter(DateUtils.yearEarlierFromNow());
        } else {
            return true;
        }
    }

    public boolean isValidTender(Tender tender) {

        boolean procuringEntityIsValid = validateProcuringEntity(tender.getProcuringEntity());

        List<Supplier> suppliers = new ArrayList<>();
        tender.getAwards().forEach(award -> suppliers.add(award.getSupplier()));
        tender.getBids().forEach(bid -> suppliers.add(bid.getSupplier()));
        tender.getTenderContracts().forEach(tenderContract -> suppliers.add(tenderContract.getSupplier()));

        boolean suppliersIsValid = validateSuppliers(suppliers);
        boolean awardsIsValid = validateAwards(tender.getAwards());
        boolean bidsIsValid = validateBids(tender.getBids());

        return Stream.of(
                new Boolean[]{
                        procuringEntityIsValid,
                        suppliersIsValid,
                        awardsIsValid,
                        bidsIsValid
                }).filter(val -> !val).collect(Collectors.toList()).isEmpty();
    }

    private boolean validateProcuringEntity(ProcuringEntity procuringEntity) {
        return null == procuringEntity
                || ValidationUtils.validateIdentifierId(procuringEntity.getIdentifierId())
                && ValidationUtils.validateIdentifierScheme(procuringEntity.getIdentifierScheme())
                && ValidationUtils.validateIdentifierLegalName(procuringEntity.getIdentifierLegalName());
    }

    private boolean validateSuppliers(List<Supplier> suppliers) {
        return null == suppliers
                || suppliers.isEmpty()
                || suppliers.stream()
                .filter(Objects::nonNull)
                .filter(supplier -> !ValidationUtils.validateIdentifierId(supplier.getIdentifierId())
                        || !ValidationUtils.validateIdentifierScheme(supplier.getIdentifierScheme())
                        || !ValidationUtils.validateIdentifierLegalName(supplier.getIdentifierLegalName())
                        || !ValidationUtils.validateTelephone(supplier.getTelephone())
                        || !ValidationUtils.validateEmail(supplier.getEmail()))
                .collect(Collectors.toList())
                .isEmpty();
    }

    private boolean validateAwards(List<Award> awards) {
        return null == awards
                || awards.isEmpty()
                || awards.stream()
                .filter(Objects::nonNull)
                .filter(award -> !ValidationUtils.validateIdentifierId(award.getSupplierIdentifierId())
                        || !ValidationUtils.validateIdentifierScheme(award.getSupplierIdentifierScheme())
                        || !ValidationUtils.validateIdentifierLegalName(award.getSupplierIdentifierScheme())
                        || !ValidationUtils.validateEmail(award.getSupplierEmail()))
                .collect(Collectors.toList())
                .isEmpty();
    }

    private boolean validateBids(List<Bid> bids) {
        return null == bids
                || bids.isEmpty()
                || bids.stream()
                .filter(Objects::nonNull)
                .filter(award -> !ValidationUtils.validateIdentifierId(award.getSupplierIdentifierId())
                        || !ValidationUtils.validateIdentifierScheme(award.getSupplierIdentifierScheme())
                        || !ValidationUtils.validateEmail(award.getSupplierEmail()))
                .collect(Collectors.toList())
                .isEmpty();
    }

    public boolean isTenderFromFinanceCategory(Tender tender) {
        if (tender.getTvTenderCPV().startsWith("6611")) {
            log.trace("Tender {} from category 'Банківські послуги' skipped.", tender.getId());
            return true;
        }
        return false;
    }
}
