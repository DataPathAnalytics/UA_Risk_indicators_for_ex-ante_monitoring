package com.datapath.auditorsindicators.analytictables.providers;

import com.datapath.persistence.entities.derivatives.MutualParticipation;
import com.datapath.persistence.entities.derivatives.SupplierLot;
import com.datapath.persistence.repositories.derivatives.AnalyticTableRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class MutualParticipationProvider {

    private AnalyticTableRepository analyticTableRepository;

    public List<MutualParticipation> provide() {
        List<MutualParticipation> result = new ArrayList<>();

        List<SupplierLot> supplierLotDAOS = analyticTableRepository.getSupplierLots();
        Map<String, Set<Long>> supplierLots = new HashMap<>();

        supplierLotDAOS.forEach(supplierLot -> {
            supplierLots.putIfAbsent(supplierLot.getSupplier(), new HashSet<>());
            supplierLots.get(supplierLot.getSupplier()).add(supplierLot.getLotId());
        });

        List<String> suppliers = new ArrayList<>(supplierLots.keySet());
        for (int i = 0; i < suppliers.size(); i++) {
            String firstSupplier = suppliers.get(i);
            Set<Long> firstSupplierLots = supplierLots.get(firstSupplier);
            for (int j = i + 1; j < suppliers.size(); j++) {
                String secondSupplier = suppliers.get(j);
                if (!firstSupplier.equals(secondSupplier)) {

                    Set<Long> secondSupplierLots = supplierLots.get(secondSupplier);
                    Set<Long> intersect = intersection(firstSupplierLots, secondSupplierLots);

                    if (!intersect.isEmpty()) {
                        MutualParticipation participation = new MutualParticipation();
                        participation.setId(result.size() + 1);
                        participation.setSupplier1(firstSupplier);
                        participation.setSupplier2(secondSupplier);
                        participation.setParticipation((double) intersect.size() / (firstSupplierLots.size() + secondSupplierLots.size() - intersect.size()));
                        result.add(participation);
                    }
                }
            }
        }
        return result;
    }

    private Set<Long> intersection(Set<Long> first, Set<Long> second) {
        return first.stream().filter(second::contains).collect(Collectors.toSet());
    }

}
