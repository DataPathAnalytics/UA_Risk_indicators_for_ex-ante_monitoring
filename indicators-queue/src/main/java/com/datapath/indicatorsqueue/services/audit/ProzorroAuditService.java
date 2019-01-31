package com.datapath.indicatorsqueue.services.audit;

import com.datapath.indicatorsqueue.domain.audit.Monitoring;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProzorroAuditService {

    private static final String AUDIT_API_URL = "https://audit-api.prozorro.gov.ua/api/2.4/monitorings";

    private RestTemplate restTemplate;

    public ProzorroAuditService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<Monitoring> getMonitorings() throws IOException {
        log.info("Fetching monitorings by url {}", AUDIT_API_URL);
        List<Monitoring> monitorings = new ArrayList<>();
        ArrayList<Monitoring> pageMonitorings;
        String auditApiUrl = AUDIT_API_URL;
        do {
            pageMonitorings = new ArrayList<>();
            String rawMonitorings = restTemplate.getForObject(auditApiUrl, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(rawMonitorings);
            for (JsonNode node : jsonNode.get("data")) {
                String monitoringId = node.at("/id").asText();
                String rawMonitoring = restTemplate.getForObject(
                        auditApiUrl + "/" + monitoringId, String.class);

                JsonNode monitoringNode = mapper.readTree(rawMonitoring);

                Monitoring monitoring = new Monitoring();
                monitoring.setId(monitoringNode.at("/data/tender_id").asText());
                monitoring.setStatus(monitoringNode.at("/data/status").asText());

                pageMonitorings.add(monitoring);
            }

            monitorings.addAll(pageMonitorings);
            auditApiUrl = jsonNode.at("/next_page/uri").asText();

        } while (!pageMonitorings.isEmpty());

        log.info("Fetched {} monitorings", monitorings.size());

        List<Monitoring> activeMonitorings = monitorings.stream()
                .filter(monitoring -> monitoring.getStatus().equals("active"))
                .collect(Collectors.toList());

        log.info("Fetched {} active monitorings", activeMonitorings.size());

        return activeMonitorings;
    }

}
