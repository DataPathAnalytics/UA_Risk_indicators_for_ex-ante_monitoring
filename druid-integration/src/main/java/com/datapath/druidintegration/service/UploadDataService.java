package com.datapath.druidintegration.service;

import com.datapath.druidintegration.model.DruidContractIndicator;
import com.datapath.druidintegration.model.DruidTenderIndicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class UploadDataService {

    private static final Logger LOG = LoggerFactory.getLogger(UploadDataService.class);
    private String druidTenderUrl;
    private String druidContractUrl;
    private String tendersIndex;
    private String contractIndex;
    private RestTemplate restTemplate;

    @Value("${tranquility.tenders.url}")
    public void setDruidTenderUrl(String url) {
        this.druidTenderUrl = url;
    }

    @Value("${tranquility.contracts.url}")
    public void setDruidContractUrl(String url) {
        this.druidContractUrl = url;
    }

    @Value("${druid.tenders.index}")
    public void setDruidTenderIndex(String index) {
        this.tendersIndex = index;
    }

    @Value("${druid.contracts.index}")
    public void setDruidContractIndex(String index) {
        this.contractIndex = index;
    }

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    public void uploadTenderIndicator(DruidTenderIndicator druidIndicator) {
        String url = druidTenderUrl + "/" + tendersIndex;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<DruidTenderIndicator> request = new HttpEntity<>(druidIndicator, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        LOG.info(response.toString() + druidIndicator.toString());
    }

    public void uploadTenderIndicator(List<DruidTenderIndicator> druidIndicators) {
        for (DruidTenderIndicator druidIndicator : druidIndicators) {
            uploadTenderIndicator(druidIndicator);
        }
    }

    public void uploadContractIndicator(DruidContractIndicator druidIndicator) {
        String url = druidContractUrl + "/" + contractIndex;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<DruidContractIndicator> request = new HttpEntity<>(druidIndicator, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        LOG.info(response.toString() + druidIndicator.toString());
    }

    public void uploadContractIndicator(List<DruidContractIndicator> druidIndicators) {
        for (DruidContractIndicator druidIndicator : druidIndicators) {
            uploadContractIndicator(druidIndicator);
        }
    }
}
