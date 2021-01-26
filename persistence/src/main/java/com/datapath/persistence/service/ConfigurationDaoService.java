package com.datapath.persistence.service;

import com.datapath.persistence.domain.ConfigurationDomain;
import com.datapath.persistence.entities.Configuration;
import com.datapath.persistence.repositories.ConfigurationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class ConfigurationDaoService {

    @Autowired
    private ConfigurationRepository repository;

    public ConfigurationDomain getConfiguration() {
        ConfigurationDomain conf = new ConfigurationDomain();

        conf.setConfigurations(
                repository.findAll()
                        .stream()
                        .collect(Collectors.toMap(Configuration::getKey, Configuration::getValue))
        );
        return conf;
    }

    public Configuration findByKey(String key) {
        return repository.findByKey(key);
    }

    public void save(Configuration configuration) {
        repository.save(configuration);
    }
}
