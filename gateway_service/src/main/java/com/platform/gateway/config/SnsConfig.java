package com.platform.gateway.config;

import com.amazonaws.xray.interceptors.TracingInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * AWS SNS SDK v2 wiring for the compliance timeout alert (Sub-Story 3.1b / TC-SEC-02).
 * Credentials resolve via the default chain, matching {@link S3Config}.
 */
@Configuration
public class SnsConfig {

    @Value("${gateway.storage.region}")
    private String region;

    @Bean
    public SnsClient snsClient() {
        return SnsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .addExecutionInterceptor(new TracingInterceptor())
                        .build())
                .build();
    }
}
