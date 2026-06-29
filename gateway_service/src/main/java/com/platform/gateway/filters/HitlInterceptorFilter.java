package com.platform.gateway.filters;

import com.platform.gateway.config.HitlConfig;
import com.platform.gateway.entities.QuarantinedPayload;
import com.platform.gateway.repositories.QuarantineRepository;
import com.platform.gateway.services.AgentAcknowledgmentService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * HITL interceptor — Sub-Story 3.1a & 3.1b.
 *
 * For every request whose URI appears in gateway.hitl.high-risk-endpoints:
 *   1. Read and cache the full request body.
 *   2. Park the payload in the quarantine table with status=Pending.
 *   3. Return 202 Accepted to the AI agent (request is NOT forwarded).
 *   4. If the agent disconnects before the 202 is delivered (ClientAbortException),
 *      flip the quarantine row to Delivery_Timeout_Warning so the compliance
 *      officer knows the agent may attempt to re-submit.
 *
 * Low-risk endpoints are never touched — the filter chain continues normally.
 */
@Component
@Order(1)
public class HitlInterceptorFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HitlInterceptorFilter.class);

    private final HitlConfig hitlConfig;
    private final QuarantineRepository quarantineRepository;
    private final AgentAcknowledgmentService acknowledgmentService;

    public HitlInterceptorFilter(HitlConfig hitlConfig,
                                 QuarantineRepository quarantineRepository,
                                 AgentAcknowledgmentService acknowledgmentService) {
        this.hitlConfig = hitlConfig;
        this.quarantineRepository = quarantineRepository;
        this.acknowledgmentService = acknowledgmentService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        if (!hitlConfig.getHighRiskEndpoints().contains(uri)) {
            chain.doFilter(request, response);
            return;
        }

        log.info("HITL: intercepting high-risk request uri={} agent_id={}",
                uri, request.getHeader("X-Agent-Id"));

        // Read the body exactly once — the InputStream cannot be replayed.
        byte[] bodyBytes = StreamUtils.copyToByteArray(request.getInputStream());
        String body = bodyBytes.length > 0
                ? new String(bodyBytes, StandardCharsets.UTF_8)
                : "{}";

        String agentId = request.getHeader("X-Agent-Id");

        QuarantinedPayload parked = new QuarantinedPayload();
        parked.setEndpoint(uri);
        parked.setPayload(body);
        parked.setAgentId(agentId);
        parked.setStatus(QuarantinedPayload.STATUS_PENDING);
        QuarantinedPayload saved = quarantineRepository.save(parked);

        log.info("HITL: parked quarantine_id={} endpoint={}", saved.getQuarantineId(), uri);

        try {
            acknowledgmentService.send202(response, saved.getQuarantineId());
        } catch (ClientAbortException e) {
            // Agent dropped the connection before the 202 was delivered.
            // Flag it so the compliance officer knows the agent may retry.
            log.warn("HITL: client disconnected before 202 delivery quarantine_id={}",
                    saved.getQuarantineId());
            saved.setStatus(QuarantinedPayload.STATUS_DELIVERY_TIMEOUT_WARNING);
            quarantineRepository.save(saved);
        }
        // Intentionally do NOT call chain.doFilter() — the request is quarantined.
    }
}
