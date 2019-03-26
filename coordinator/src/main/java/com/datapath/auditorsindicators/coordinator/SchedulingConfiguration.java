package com.datapath.auditorsindicators.coordinator;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "com.datapath.scheduling", name = "enabled", havingValue = "true")
public class SchedulingConfiguration {
}
