package com.datapath.elasticsearchintegration.configuration;

import com.datapath.elasticsearchintegration.constants.RiskedProcedure;
import com.datapath.elasticsearchintegration.constants.TenderScoreRank;
import com.datapath.elasticsearchintegration.converters.RiskedProcedureEnumConverter;
import com.datapath.elasticsearchintegration.converters.TenderScoreRankEnumConverter;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;

import javax.annotation.PreDestroy;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Service
public class ElasticSearchClientConfiguration implements InitializingBean {

    @Value("${elastic.host}")
    private String host;

    @Value("${elastic.port}")
    private String port;

    private TransportClient transportClient;

    @Bean
    public TransportClient transportClient() {
        return transportClient;
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(RiskedProcedure.class, new RiskedProcedureEnumConverter());
        dataBinder.registerCustomEditor(TenderScoreRank.class, new TenderScoreRankEnumConverter());
    }

    @Override
    public void afterPropertiesSet() throws UnknownHostException {
        transportClient = new PreBuiltTransportClient(Settings.builder()
                .put("client.transport.sniff", false)
                .put("cluster.name", "docker-cluster").build())
                .addTransportAddress(new TransportAddress(InetAddress.getByName(host),
                        Integer.parseInt(port)));
    }

    @PreDestroy
    public void closeConnection() {
        transportClient.close();
    }
}
