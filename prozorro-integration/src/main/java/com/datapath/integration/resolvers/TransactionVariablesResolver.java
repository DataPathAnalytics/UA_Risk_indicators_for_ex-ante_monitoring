package com.datapath.integration.resolvers;

import com.datapath.integration.services.CpvCatalogueService;
import com.datapath.integration.utils.Gsw;
import com.datapath.persistence.entities.CpvCatalogue;
import com.datapath.persistence.entities.Tender;
import com.datapath.persistence.entities.TenderItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Component
public class TransactionVariablesResolver {

    private CpvCatalogueService cpvCatalogueService;

    public TransactionVariablesResolver(CpvCatalogueService cpvCatalogueService) {
        this.cpvCatalogueService = cpvCatalogueService;
    }

    public String getSubjectOfProcurement(Tender tender) {
        List<String> cpvs = tender.getItems().stream().map(TenderItem::getClassificationId).collect(Collectors.toList());
        List<CpvCatalogue> catalogues = cpvCatalogueService.getCatalogue(cpvs);

        TransactionVariablesResolver.CpvLevel parentCpvLevel = getParentCpvLevel(catalogues);
        CpvCatalogue catalogue = cpvCatalogueService.findByCpv(parentCpvLevel.getCpv());
        //TODO need to discuss what to do when common parent doesn't exist in cpv canalogue
        if (catalogue == null) return parentCpvLevel.getCpv();
        Gsw gsw = Gsw.find(parentCpvLevel.getCpv());

        if (gsw.equals(Gsw.WORKS)) {
            return parentCpvLevel.getLevel() > 5 ? catalogue.getCpv5() : catalogue.getCpv();
        } else {
            List<String> prefixes = Arrays.asList("3361", "3362", "3363", "3364", "3365", "3366", "3367", "33691", "33692");

            if (prefixes.contains(catalogue.getCpv().substring(0, 4))) {
                return parentCpvLevel.getLevel() > 3 ? catalogue.getCpv3() : catalogue.getCpv();
            } else {
                return parentCpvLevel.getLevel() > 4 ? catalogue.getCpv4() : catalogue.getCpv();
            }
        }
    }

    public String getTenderCPV(Tender tender) {
        List<String> cpvs = tender.getItems().stream().map(TenderItem::getClassificationId).collect(Collectors.toList());
        List<CpvCatalogue> catalogues = cpvCatalogueService.getCatalogue(cpvs);

        TransactionVariablesResolver.CpvLevel parentCpvLevel = getParentCpvLevel(catalogues);
        return parentCpvLevel.cpv;
    }

    private TransactionVariablesResolver.CpvLevel getParentCpvLevel(List<CpvCatalogue> catalogue) {

        Set<String> cpvSet = catalogue.stream().collect(groupingBy(CpvCatalogue::getCpv8)).keySet();
        if (cpvSet.size() == 1 && !cpvSet.contains("undefined")) {
            return new TransactionVariablesResolver.CpvLevel(cpvSet.iterator().next(), 8);
        }

        cpvSet = catalogue.stream().collect(groupingBy(CpvCatalogue::getCpv7)).keySet();
        if (cpvSet.size() == 1 && !cpvSet.contains("undefined")) return new TransactionVariablesResolver.CpvLevel(cpvSet.iterator().next(), 7);

        cpvSet = catalogue.stream().collect(groupingBy(CpvCatalogue::getCpv6)).keySet();
        if (cpvSet.size() == 1 && !cpvSet.contains("undefined")) return new TransactionVariablesResolver.CpvLevel(cpvSet.iterator().next(), 6);

        cpvSet = catalogue.stream().collect(groupingBy(CpvCatalogue::getCpv5)).keySet();
        if (cpvSet.size() == 1 && !cpvSet.contains("undefined")) return new TransactionVariablesResolver.CpvLevel(cpvSet.iterator().next(), 5);

        cpvSet = catalogue.stream().collect(groupingBy(CpvCatalogue::getCpv4)).keySet();
        if (cpvSet.size() == 1 && !cpvSet.contains("undefined")) return new TransactionVariablesResolver.CpvLevel(cpvSet.iterator().next(), 4);

        cpvSet = catalogue.stream().collect(groupingBy(CpvCatalogue::getCpv3)).keySet();
        if (cpvSet.size() == 1 && !cpvSet.contains("undefined")) return new TransactionVariablesResolver.CpvLevel(cpvSet.iterator().next(), 3);

        cpvSet = catalogue.stream().collect(groupingBy(CpvCatalogue::getCpv2)).keySet();
        if (cpvSet.size() == 1 && !cpvSet.contains("undefined")) return new TransactionVariablesResolver.CpvLevel(cpvSet.iterator().next(), 2);

        throw new RuntimeException("Can't find parent code for items in tender");
    }

    @Data
    @AllArgsConstructor
    private static class CpvLevel {
        private String cpv;
        private int level;
    }
}
