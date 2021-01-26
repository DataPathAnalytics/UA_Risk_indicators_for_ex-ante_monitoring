package com.datapath.indicatorsresolver.request;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Data
@Slf4j
@Component
public class RequestsServiceImpl implements RequestsService {

    @Value("${prozorro.requests.url}")
    private String apiUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public List<RequestDTO> getRequests() {

        List<RequestDTO> requests = new ArrayList<>();
        List<RequestDTO> requestBatch;
        String url = apiUrl;

        do {
            log.info("Load tenders from [{}]", url);
            RequestsResponse response = restTemplate.getForObject(url, RequestsResponse.class);
            requestBatch = new ArrayList<>();


            if (response != null && response.getData() != null) {
                for (RequestDTO rawRequest : response.getData()) {
                    log.info("Get data for request with id [{}]", rawRequest.getId());
                    RequestDataDTO request = restTemplate.getForObject(apiUrl + "/" + rawRequest.getId(), RequestDataDTO.class);
                    requestBatch.add(request.getData());
                }
                url = response.getNextPage().getUri();
                requests.addAll(requestBatch);
            }

        } while (requestBatch.size() > 2);

        return requests;
    }
}
