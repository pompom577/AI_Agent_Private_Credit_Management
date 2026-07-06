package com.platform.gateway.config;

import com.amazonaws.xray.interceptors.TracingInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * AWS S3 SDK v2 wiring for Sub-Story 1.1a (secure bucket persistence).
 * Credentials resolve via the default chain (env vars, profile file, container/instance role) —
 * Person 4 provisions the IAM role granting {@code s3:PutObject} on the target bucket.
 */
@Configuration
public class S3Config {

    @Value("${gateway.storage.bucket}")
    private String bucket;

    @Value("${gateway.storage.region}")
    private String region;

    public String getBucket() {
        return bucket;
    }

    public String getRegion() {
        return region;
    }

    /**
     * Build the SDK v2 client. The {@code AWS_ENDPOINT_URL} env var (or the
     * {@code gateway.storage.endpoint-override} property) lets dev / CI redirect
     * the SDK at a local S3-compatible server (MinIO, LocalStack, s3mock, ...).
     * When unset the SDK talks to real AWS S3 in {@link #region}.
     */
    @Bean
    public S3Client s3Client(
            @Value("${gateway.storage.endpoint-override:${AWS_ENDPOINT_URL:}}") String endpointOverride) {
        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .addExecutionInterceptor(new TracingInterceptor())
                        .build());
        if (endpointOverride != null && !endpointOverride.isBlank()) {
            // MinIO / LocalStack require path-style addressing: http://host/<bucket>/<key>.
            builder.endpointOverride(URI.create(endpointOverride))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build());
        }
        return builder.build();
    }
}
