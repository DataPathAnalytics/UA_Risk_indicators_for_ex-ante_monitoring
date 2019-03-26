package com.datapath.web.api.rest.controller;

import com.datapath.web.api.version.ApiVersion;
import com.datapath.web.domain.validation.TenderValidation;
import com.datapath.web.services.impl.TenderValidationService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class TenderValidationController {

    private TenderValidationService tenderValidationService;

    public TenderValidationController(TenderValidationService tenderValidationService) {
        this.tenderValidationService = tenderValidationService;
    }

    @ApiVersion({0.1})
    @RequestMapping(value = "/tenders-validation", method = RequestMethod.GET)
    public TenderValidation getIndicatorsByTenderId() {
        return tenderValidationService.getTenderValidation();
    }

}

