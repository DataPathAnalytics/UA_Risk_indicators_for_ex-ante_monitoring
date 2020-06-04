package com.datapath.indicatorsqueue.services.audit;

import com.datapath.indicatorsqueue.domain.audit.Monitoring;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLDecoder;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@Slf4j
@Service
public class ProzorroAuditService {

    @Value("${prozorro.monitorings.url}")
    private String baseApiUrl;

    private RestTemplate restTemplate;

    public ProzorroAuditService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private ZonedDateTime parseZonedDateTime(String zonedDateTimeStr) {
        if (StringUtils.isEmpty(zonedDateTimeStr)) return null;
        return ZonedDateTime.parse(zonedDateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSSSSS]XXX"));
    }

    public List<Monitoring> getMonitorings() throws IOException {
        log.info("Start fetching monitoring");
        List<Monitoring> monitorings = new ArrayList<>();
        ArrayList<Monitoring> pageMonitorings;
        String auditApiUrl = baseApiUrl;
        ObjectMapper mapper = new ObjectMapper();
        do {
            pageMonitorings = new ArrayList<>();
            log.info("Fetching monitoring by url {}", auditApiUrl);
            String rawMonitorings = restTemplate.getForObject(URLDecoder.decode(auditApiUrl, "utf8"), String.class);
            JsonNode jsonNode = mapper.readTree(rawMonitorings);
            for (JsonNode node : jsonNode.get("data")) {
                String monitoringId = node.at("/id").asText();
                String rawMonitoring = restTemplate.getForObject(baseApiUrl + "/" + monitoringId, String.class);

                JsonNode monitoringNode = mapper.readTree(rawMonitoring);

                String startDate = monitoringNode.at("/data/monitoringPeriod/startDate").asText();
                String endDate = monitoringNode.at("/data/monitoringPeriod/endDate").asText();

                Monitoring monitoring = new Monitoring();
                monitoring.setId(monitoringNode.at("/data/tender_id").asText());
                monitoring.setStatus(monitoringNode.at("/data/status").asText());
                monitoring.setCauses(new ArrayList<>());
                for (JsonNode tempNode : monitoringNode.at("/data/reasons")) {
                    monitoring.getCauses().add(tempNode.asText());
                }
                monitoring.setStartDate(parseZonedDateTime(startDate));
                monitoring.setEndDate(nonNull(endDate)
                        ? parseZonedDateTime(endDate)
                        : parseZonedDateTime(startDate).plusDays(15)
                );
                String appealDocUrl = baseApiUrl + "/" + monitoringId + "/appeal";
                String appealDoc = restTemplate.getForObject(
                        appealDocUrl, String.class);

                JsonNode monitoringAppealNode = mapper.readTree(appealDoc);

                try {
                    if (monitoringAppealNode.at("/data/documents").size() > 0) {
                        monitoring.setAppeal(true);
                    }
                } catch (Exception e) {
                    log.warn("Appeal not found");
                }

                monitoring.setMonitoringId(monitoringId);
                monitoring.setOffice(getOfficeFromMonitoringNode(monitoringNode));

                pageMonitorings.add(monitoring);
            }

            monitorings.addAll(pageMonitorings);
            auditApiUrl = jsonNode.at("/next_page/uri").asText();

        } while (pageMonitorings.size() > 10);

        log.info("Fetched {} monitorings", monitorings.size());
        return monitorings;
    }

    private String getOfficeFromMonitoringNode(JsonNode monitoringNode) {
        Iterator<JsonNode> parties = monitoringNode.at("/data/parties").elements();
        String monitoringOffice = null;
        while (parties.hasNext()) {
            JsonNode party = parties.next();
            Iterator<JsonNode> roles = party.at("/roles").elements();
            while (roles.hasNext()) {
                if (roles.next().asText().equals("sas")) {
                    monitoringOffice = party.at("/name").asText();
                    break;
                }
            }
            if (monitoringOffice != null) {
                break;
            }
        }
        return monitoringOffice;
    }


    public List<Monitoring> getActiveMonitorings() throws IOException {
        List<Monitoring> monitorings = getMonitorings();

        List<Monitoring> activeMonitorings = monitorings.stream()
                .filter(monitoring -> monitoring.getStatus().equals("active"))
                .collect(Collectors.toList());

        log.info("Fetched {} active monitorings", activeMonitorings.size());

        return activeMonitorings;
    }

}
