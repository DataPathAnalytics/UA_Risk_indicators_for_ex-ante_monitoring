package com.datapath.web.services.impl;

import com.datapath.persistence.entities.validation.TenderValidationHistory;
import com.datapath.persistence.repositories.validation.TenderValidationHistoryRepository;
import com.datapath.web.domain.validation.TenderValidation;
import com.datapath.web.domain.validation.TenderValidationHistoryItem;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class TenderValidationService {

    private TenderValidationHistoryRepository tenderValidationHistoryRepository;

    public TenderValidationService(TenderValidationHistoryRepository tenderValidationHistoryRepository) {
        this.tenderValidationHistoryRepository = tenderValidationHistoryRepository;
    }

    public TenderValidation getTenderValidation() {
        List<TenderValidationHistory> allByOrderByDateDesc = tenderValidationHistoryRepository
                .findAllByOrderByDateDesc();

        if (allByOrderByDateDesc.isEmpty()) {
            return new TenderValidation();
        }

        TenderValidationHistory lastHistoricalValidation = allByOrderByDateDesc.get(0);
        TenderValidation validation = new TenderValidation();
        validation.setDate(lastHistoricalValidation.getDate());
        validation.setExistingTendersCount(lastHistoricalValidation.getExistingTendersCount());
        validation.setMissingTendersCount(lastHistoricalValidation.getMissingTendersCount());
        validation.setTestOrExpiredTendersCount(lastHistoricalValidation.getTestOrExpiredTendersCount());
        validation.setMissingTenders(Arrays.asList(lastHistoricalValidation.getMissingTenders()));

        List<TenderValidationHistoryItem> tenderValidationHistory = new ArrayList<>();
        for (TenderValidationHistory historyItem : allByOrderByDateDesc) {
            TenderValidationHistoryItem item = new TenderValidationHistoryItem();
            item.setDate(historyItem.getDate());
            item.setExistingTendersCount(historyItem.getExistingTendersCount());
            item.setMissingTendersCount(historyItem.getMissingTendersCount());
            item.setTestOrExpiredTendersCount(historyItem.getTestOrExpiredTendersCount());
            item.setMissingTenders(Arrays.asList(lastHistoricalValidation.getMissingTenders()));
            tenderValidationHistory.add(item);
        }

        validation.setHistory(tenderValidationHistory);

        return validation;
    }
}
