package com.platform.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Binds gateway.hitl.high-risk-endpoints from application.yml.
 * Add any endpoint path the HITL filter should intercept and quarantine.
 */
@Configuration
@ConfigurationProperties(prefix = "gateway.hitl")
public class HitlConfig {

    private List<String> highRiskEndpoints = new ArrayList<>();

    public List<String> getHighRiskEndpoints() { return highRiskEndpoints; }
    public void setHighRiskEndpoints(List<String> highRiskEndpoints) {
        this.highRiskEndpoints = highRiskEndpoints;
    }
}
