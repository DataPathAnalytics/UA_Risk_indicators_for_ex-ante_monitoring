package com.datapath.integration.services.impl;

import com.datapath.integration.domain.AgreementResponseEntity;
import com.datapath.integration.domain.AgreementUpdateInfo;
import com.datapath.integration.domain.AgreementsPageResponse;
import com.datapath.integration.services.AgreementLoaderService;
import com.datapath.integration.services.TenderLoaderService;
import com.datapath.integration.utils.DateUtils;
import com.datapath.integration.utils.EntitySource;
import com.datapath.integration.utils.ProzorroRequestUrlCreator;
import com.datapath.persistence.entities.Agreement;
import com.datapath.persistence.entities.AgreementSupplier;
import com.datapath.persistence.entities.Tender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@Service
public class ProzorroAgreementLoaderService implements AgreementLoaderService {

    @Value("${prozorro.agreements.url}")
    private String apiUrl;

    private final RestTemplate template;
    private final AgreementService agreementService;
    private final SupplierService supplierService;
    private final TenderLoaderService tenderLoaderService;

    public ProzorroAgreementLoaderService(RestTemplate template,
                                          AgreementService agreementService,
                                          SupplierService supplierService,
                                          TenderLoaderService tenderLoaderService) {
        this.template = template;
        this.agreementService = agreementService;
        this.supplierService = supplierService;
        this.tenderLoaderService = tenderLoaderService;
    }

    @Override
    public AgreementResponseEntity loadAgreement(AgreementUpdateInfo info) {
        String url = ProzorroRequestUrlCreator.createAgreementUrl(apiUrl, info.getId());
        log.info("Agreement loading: {}", url);
        String responseData = template.getForObject(url, String.class);
        AgreementResponseEntity responseEntity = new AgreementResponseEntity();
        responseEntity.setId(info.getId());
        responseEntity.setData(responseData);
        return responseEntity;
    }

    @Override
    public ZonedDateTime resolveDateOffset() {
        ZonedDateTime lastDateModified = agreementService.getLastDateModified();
        return nonNull(lastDateModified) ?
                lastDateModified.withZoneSameInstant(ZoneId.of("Europe/Kiev")) :
                DateUtils.yearEarlierFromNow().withZoneSameInstant(ZoneId.of("Europe/Kiev"));
    }

    @Override
    public AgreementsPageResponse loadAgreementPage(String url) {
        return template.getForObject(url, AgreementsPageResponse.class);
    }

    @Transactional
    @Override
    public synchronized void saveAgreement(Agreement agreement) throws Exception {
        Tender tender = tenderLoaderService.getTenderByOuterId(agreement.getTenderOuterId());

        if (isNull(tender)) {
            throw new Exception(String.format("Agreement tender not found %s", agreement.getTenderOuterId()));
        }

        Optional<Agreement> existedAgreementOpt = tender.getAgreements()
                .stream()
                .filter(a -> agreement.getOuterId().equals(a.getOuterId()))
                .findFirst();

        if (existedAgreementOpt.isPresent()) {
            Agreement existedAgreement = existedAgreementOpt.get();
            if (agreement.getDateModified().isAfter(existedAgreement.getDateModified())) {
                agreement.setId(existedAgreement.getId());
            } else {
                log.info("Last version of agreement already saved {}", existedAgreement.getOuterId());
                existedAgreement.setSource(EntitySource.AGREEMENT.toString());
                return;
            }
        }

        AgreementTenderJoinService.joinInnerElements(agreement, tender);

        Map<String, AgreementSupplier> existingAgreementSuppliers = agreement.getContracts()
                .stream()
                .flatMap(c -> c.getSuppliers().stream())
                .collect(Collectors.toSet())
                .stream()
                .map(s -> supplierService.findAgreementSupplierByIdentifierIdAndScheme(s.getIdentifierId(), s.getIdentifierScheme()))
                .filter(Objects::nonNull)
                .collect(
                        Collectors.toMap(
                                s -> s.getIdentifierScheme() + s.getIdentifierId(),
                                Function.identity()
                        )
                );

        agreement.getContracts().forEach(c -> {
            List<AgreementSupplier> suppliers = new LinkedList<>();

            c.getSuppliers().forEach(s -> {
                if (existingAgreementSuppliers.containsKey(s.getIdentifierScheme() + s.getIdentifierId())) {
                    existingAgreementSuppliers.get(s.getIdentifierScheme() + s.getIdentifierId()).getContracts().add(c);
                    suppliers.add(existingAgreementSuppliers.get(s.getIdentifierScheme() + s.getIdentifierId()));
                } else {
                    suppliers.add(s);
                }
            });

            c.setSuppliers(suppliers);
        });

        agreementService.save(agreement);
    }
}
