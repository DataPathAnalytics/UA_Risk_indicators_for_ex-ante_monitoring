package com.datapath.integration.validation;

import com.datapath.integration.utils.ValidationUtils;
import com.datapath.persistence.entities.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.contains;

@Slf4j
@Component
public class TenderDataValidator {

    private static final String TEST_TITLE = "ТЕСТУВАННЯ";
    private static final String PRICE_QUOTATION = "priceQuotation";

    public boolean isValidTender(Tender tender) {

        if (contains(tender.getTitle(), TEST_TITLE)) return false;
        if (PRICE_QUOTATION.equalsIgnoreCase(tender.getProcurementMethodType())) return false;

        boolean procuringEntityIsValid = validateProcuringEntity(tender.getProcuringEntity());

        List<Supplier> suppliers = new ArrayList<>();
        tender.getAwards().forEach(award -> suppliers.add(award.getSupplier()));
        tender.getBids().forEach(bid -> suppliers.add(bid.getSupplier()));
        tender.getTenderContracts().forEach(tenderContract -> suppliers.add(tenderContract.getSupplier()));

        boolean suppliersIsValid = validateSuppliers(suppliers);
        boolean awardsIsValid = validateAwards(tender.getAwards());
        boolean bidsIsValid = validateBids(tender.getBids());

        return Stream.of(
                procuringEntityIsValid,
                suppliersIsValid,
                awardsIsValid,
                bidsIsValid
        ).filter(val -> !val).collect(toList()).isEmpty();
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
                .collect(toList())
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
                .collect(toList())
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
                .collect(toList())
                .isEmpty();
    }
}
