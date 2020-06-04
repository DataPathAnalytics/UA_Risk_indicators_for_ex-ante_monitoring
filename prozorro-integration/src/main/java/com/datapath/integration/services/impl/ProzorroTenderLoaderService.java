package com.datapath.integration.services.impl;

import com.datapath.integration.domain.TenderResponseEntity;
import com.datapath.integration.domain.TenderUpdateInfo;
import com.datapath.integration.domain.TendersPageResponseEntity;
import com.datapath.integration.services.TenderLoaderService;
import com.datapath.integration.utils.DateUtils;
import com.datapath.integration.utils.ProzorroRequestUrlCreator;
import com.datapath.persistence.entities.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.transaction.Transactional;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProzorroTenderLoaderService implements TenderLoaderService {

    @Value("${prozorro.tenders.url}")
    private String apiUrl;

    private RestTemplate restTemplate;
    private TenderService tenderService;
    private ProcuringEntityService procuringEntityService;
    private SupplierService supplierService;

    public ProzorroTenderLoaderService(RestTemplate restTemplate,
                                       TenderService integrationTenderService,
                                       ProcuringEntityService procuringEntityService,
                                       SupplierService supplierService) {
        this.restTemplate = restTemplate;
        this.tenderService = integrationTenderService;
        this.procuringEntityService = procuringEntityService;
        this.supplierService = supplierService;
    }

    @Override
    public TendersPageResponseEntity loadTendersPage(String url) {
        return restTemplate.getForObject(url, TendersPageResponseEntity.class);
    }

    @Override
    public TenderResponseEntity loadTender(TenderUpdateInfo tenderUpdateInfo) {
        final String tenderUrl = ProzorroRequestUrlCreator.createTenderUrl(
                apiUrl, tenderUpdateInfo.getId());

        final String responseData = restTemplate.getForObject(tenderUrl, String.class);
        TenderResponseEntity tender = new TenderResponseEntity();
        tender.setData(responseData);
        tender.setId(tenderUpdateInfo.getId());
        tender.setDateModified(tenderUpdateInfo.getDateModified());
        return tender;
    }

    @Override
    public ZonedDateTime resolveDateOffset() {
        ZonedDateTime lastModifiedDate = getLastModifiedDate();
        return lastModifiedDate != null ? lastModifiedDate : getYearEarlierDate();
    }

    @Override
    public ZonedDateTime getYearEarlierDate() {
        return DateUtils.yearEarlierFromNow();
    }

    @Override
    public ZonedDateTime getLastModifiedDate() {
        Tender lastModifiedTender = tenderService.findLastModifiedEntry();
        return lastModifiedTender != null && lastModifiedTender.getDateModified() != null ?
                lastModifiedTender.getDateModified().plusNanos(1000) : null;
    }

    @Override
    @Transactional
    public synchronized Tender saveTender(Tender tender) {
        ProcuringEntity procuringEntity = procuringEntityService.findByIdentifierIdAndScheme(
                tender.getProcuringEntity().getIdentifierId(), tender.getProcuringEntity().getIdentifierScheme());

        Tender existingTender = tenderService.findByOuterId(tender.getOuterId());
        if (existingTender != null && tender.getDateModified().withZoneSameInstant(ZoneId.of("UTC")).equals(existingTender.getDateModified())) {
            log.info("Last version of tender exists: id = {}, dateCreated = {}", existingTender.getId(), existingTender.getDateModified());
            return existingTender;
        }
        if (existingTender != null) {
            log.info("Tender exists: id = {}, dateCreated = {}", existingTender.getId(), existingTender.getDateCreated());
            tender.setId(existingTender.getId());

            Map<String, Contract> existingContractsMap = existingTender.getTenderContracts()
                    .stream().map(TenderContract::getContract)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(Contract::getOuterId, contract -> contract));

            tender.getTenderContracts().forEach(tenderContract -> {
                Contract contract = tenderContract.getContract();
                if (contract != null) {
                    Contract existingContract = existingContractsMap.get(contract.getOuterId());
                    if (existingContract != null) {
                        contract.setId(existingContract.getId());
                    }
                }
            });
        }

        if (procuringEntity != null) {
            tender.setProcuringEntity(procuringEntity);
        }

        List<Supplier> existingSuppliers = new ArrayList<>();
        tender.getAwards().forEach(award -> {
            final Supplier supplier = award.getSupplier();
            if (supplier != null) {
                final Supplier existingSupplier = supplierService.findByIdentifierIdAndScheme(
                        supplier.getIdentifierId(), supplier.getIdentifierScheme());
                if (existingSupplier == null) {
                    existingSuppliers.add(supplierService.save(supplier));
                } else {
                    existingSuppliers.add(existingSupplier);
                }
            }
        });
        tender.getTenderContracts().forEach(tenderContract -> {
            final Supplier supplier = tenderContract.getSupplier();
            if (supplier != null) {
                final Supplier existingSupplier = supplierService.findByIdentifierIdAndScheme(
                        supplier.getIdentifierId(), supplier.getIdentifierScheme());
                if (existingSupplier == null) {
                    existingSuppliers.add(supplierService.save(supplier));
                } else {
                    existingSuppliers.add(existingSupplier);
                }
            }
        });
        tender.getBids().forEach(bid -> {
            final Supplier supplier = bid.getSupplier();
            if (supplier != null) {
                final Supplier existingSupplier = supplierService.findByIdentifierIdAndScheme(
                        supplier.getIdentifierId(), supplier.getIdentifierScheme());
                if (existingSupplier == null) {
                    existingSuppliers.add(supplierService.save(supplier));
                } else {
                    existingSuppliers.add(existingSupplier);
                }
            }
        });

        tender.getTenderContracts().forEach(tenderContract -> {
            Supplier newSupplier = tenderContract.getSupplier();
            if (newSupplier != null) {
                Supplier existingSupplier = existingSuppliers.stream().filter(sup -> {
                    String id = sup.getIdentifierId() + sup.getIdentifierScheme();
                    String identifierScheme = newSupplier.getIdentifierScheme();
                    String identifierId = newSupplier.getIdentifierId();
                    return (identifierId + identifierScheme).equals(id);
                }).findFirst().get();

                existingSupplier.setEmail(newSupplier.getEmail());
                existingSupplier.setTelephone(newSupplier.getTelephone());
                existingSupplier.setIdentifierLegalName(newSupplier.getIdentifierLegalName());

                tenderContract.setSupplier(existingSupplier);
            }
        });

        tender.getAwards().forEach(award -> {
            Supplier newSupplier = award.getSupplier();
            if (newSupplier != null) {
                Supplier existingSupplier = existingSuppliers.stream().filter(sup -> {
                    String id = sup.getIdentifierId() + sup.getIdentifierScheme();
                    String identifierScheme = newSupplier.getIdentifierScheme();
                    String identifierId = newSupplier.getIdentifierId();
                    return (identifierId + identifierScheme).equals(id);
                }).findFirst().get();

                existingSupplier.setEmail(newSupplier.getEmail());
                existingSupplier.setTelephone(newSupplier.getTelephone());
                existingSupplier.setIdentifierLegalName(newSupplier.getIdentifierLegalName());

                award.setSupplier(existingSupplier);
            }
        });

        tender.getBids().forEach(bid -> {
            Supplier newSupplier = bid.getSupplier();
            if (newSupplier != null) {
                Supplier existingSupplier = existingSuppliers.stream().filter(sup -> {
                    String id = sup.getIdentifierId() + sup.getIdentifierScheme();
                    String identifierScheme = newSupplier.getIdentifierScheme();
                    String identifierId = newSupplier.getIdentifierId();
                    return (identifierId + identifierScheme).equals(id);
                }).findFirst().get();

                existingSupplier.setEmail(newSupplier.getEmail());
                existingSupplier.setTelephone(newSupplier.getTelephone());
                existingSupplier.setIdentifierLegalName(newSupplier.getIdentifierLegalName());

                bid.setSupplier(existingSupplier);
            }
        });

        return tenderService.save(tender);
    }

    @Override
    public boolean newestTenderVersionExists(String outerId, ZonedDateTime dateModified) {
        return tenderService.newestVersionExists(outerId, dateModified);
    }

    @Override
    @Transactional
    public void removeTendersByDate(ZonedDateTime date) {
        tenderService.removeByDate(date);
    }

    @Override
    public long removeTenderByOuterId(String outerId) {
        return tenderService.removeByOuterId(outerId);
    }
}
