package com.platform.gateway.config;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.jakarta.servlet.AWSXRayServletFilter;
import com.amazonaws.xray.strategy.sampling.LocalizedSamplingStrategy;
import jakarta.servlet.Filter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URL;

/**
 * Task #2 Part 3 — every inbound request gets an X-Ray segment named
 * "gateway-service" (matches AWS_XRAY_DAEMON_ADDRESS in docker-compose.yml).
 * Downstream S3/SNS calls are instrumented separately in S3Config/SnsConfig
 * so they show up as their own nodes on the X-Ray service map.
 */
@Configuration
public class XRayConfig {

    public XRayConfig() {
        URL samplingRules = XRayConfig.class.getResource("/xray-sampling-rules.json");
        AWSXRayRecorderBuilder builder = AWSXRayRecorderBuilder.standard()
                .withSamplingStrategy(new LocalizedSamplingStrategy(samplingRules));
        AWSXRay.setGlobalRecorder(builder.build());
    }

    @Bean
    public Filter tracingFilter() {
        return new AWSXRayServletFilter("gateway-service");
    }
}
