package com.platform.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SSE configuration for the real-time extraction failure bridge (Story 1.3, Person 3).
 * Named WebSocketConfig per the story task spec; implements SSE as the transport.
 */
@Configuration
public class WebSocketConfig {

    /** SSE emitter lifetime: 5 minutes. Clients reconnect automatically on expiry. */
    @Bean
    public Long sseTimeoutMs() {
        return 300_000L;
    }
}
