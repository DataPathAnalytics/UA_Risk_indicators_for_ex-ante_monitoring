package com.datapath.integration.scheduling;

import com.datapath.persistence.entities.Couse;
import com.datapath.persistence.entities.MonitoringEntity;
import com.datapath.persistence.repositories.MonitoringRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLDecoder;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@Component
public class MonitoringUpdateScheduler {

    @Value("${prozorro.monitorings.url}")
    private String baseApiUrl;

    private RestTemplate restTemplate;
    private MonitoringRepository repository;

    public MonitoringUpdateScheduler(RestTemplate restTemplate, MonitoringRepository repository) {
        this.restTemplate = restTemplate;
        this.repository = repository;
    }

    @Scheduled(fixedDelay = 1_000 * 60 * 20)//20 min
    public void updateMonitorings() throws IOException {
        MonitoringEntity lastMonitoring = repository.findFirstByOrderByModifiedDateDesc();

        String url = isNull(lastMonitoring) ?
                baseApiUrl :
                baseApiUrl + "?offset=" + lastMonitoring.getModifiedDate();

        ArrayList<MonitoringEntity> pageMonitorings;

        ObjectMapper mapper = new ObjectMapper();
        do {
            pageMonitorings = new ArrayList<>();
            log.info("Fetching monitorings by url {}", url);
            String rawMonitorings = restTemplate.getForObject(URLDecoder.decode(url, "utf8"), String.class);
            JsonNode jsonNode = mapper.readTree(rawMonitorings);
            for (JsonNode node : jsonNode.get("data")) {
                String monitoringId = node.at("/id").asText();
                String rawMonitoring = restTemplate.getForObject(
                        baseApiUrl + "/" + monitoringId, String.class);

                JsonNode monitoringNode = mapper.readTree(rawMonitoring);

                String startDate = monitoringNode.at("/data/monitoringPeriod/startDate").asText();
                String endDate = monitoringNode.at("/data/monitoringPeriod/endDate").asText();
                String modifiedDate = monitoringNode.at("/data/dateModified").asText();

                MonitoringEntity monitoring = new MonitoringEntity();

                monitoring.setId(monitoringId);
                monitoring.setStatus(monitoringNode.at("/data/status").asText());

                for (JsonNode tempNode : monitoringNode.at("/data/reasons")) {
                    Couse couse = new Couse();
                    couse.setMonitoring(monitoring);
                    couse.setReason(tempNode.asText());
                    monitoring.getCauses().add(couse);
                }

                monitoring.setModifiedDate(modifiedDate);
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

                monitoring.setTenderId(monitoringNode.at("/data/tender_id").asText());
                monitoring.setOffice(getOfficeFromMonitoringNode(monitoringNode));

                String conclusionDate = monitoringNode.at("/data/conclusion/date").asText();
                monitoring.setConclusionDate(parseZonedDateTime(conclusionDate));

                repository.save(monitoring);
                pageMonitorings.add(monitoring);
            }

            url = jsonNode.at("/next_page/uri").asText();

        } while (pageMonitorings.size() > 10);

        log.info("Fetching monitorings finished");
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

    private ZonedDateTime parseZonedDateTime(String zonedDateTimeStr) {
        if (StringUtils.isEmpty(zonedDateTimeStr)) return null;
        return ZonedDateTime.parse(zonedDateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSSSSS]XXX"));
    }
}
