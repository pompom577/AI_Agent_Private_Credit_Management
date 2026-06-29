package com.platform.gateway.filters;

import com.platform.gateway.config.HitlConfig;
import com.platform.gateway.entities.QuarantinedPayload;
import com.platform.gateway.repositories.QuarantineRepository;
import com.platform.gateway.services.AgentAcknowledgmentService;
import jakarta.servlet.FilterChain;
import org.apache.catalina.connector.ClientAbortException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TC-GW-01: High-risk endpoint is intercepted and request is NOT forwarded.
 * TC-GW-02: Low-risk endpoint passes through the filter chain unmodified.
 * TC-GW-03: 202 Accepted is delivered with quarantine_id to the AI agent.
 * TC-GW-04: ClientAbortException flips quarantine status to Delivery_Timeout_Warning.
 */
@ExtendWith(MockitoExtension.class)
class HitlInterceptorFilterTest {

    @Mock private QuarantineRepository quarantineRepository;
    @Mock private AgentAcknowledgmentService acknowledgmentService;
    @Mock private FilterChain chain;

    private HitlInterceptorFilter filter;
    private static final String HIGH_RISK_URI = "/api/credit/approve";

    @BeforeEach
    void setUp() {
        HitlConfig config = new HitlConfig();
        config.setHighRiskEndpoints(List.of(HIGH_RISK_URI));
        filter = new HitlInterceptorFilter(config, quarantineRepository, acknowledgmentService);
    }

    @Test
    void TC_GW_01_highRiskRequestIsInterceptedAndNotForwarded() throws Exception {
        // A POST to a flagged endpoint must be blocked from reaching its destination.
        MockHttpServletRequest request = buildRequest("POST", HIGH_RISK_URI, "{\"amount\":50000}", "agent-007");
        MockHttpServletResponse response = new MockHttpServletResponse();

        UUID id = UUID.randomUUID();
        QuarantinedPayload saved = savedPayload(id);
        when(quarantineRepository.save(any())).thenReturn(saved);

        filter.doFilter(request, response, chain);

        // Must never reach the actual destination endpoint
        verify(chain, never()).doFilter(any(), any());

        // Payload must be persisted with Pending status
        ArgumentCaptor<QuarantinedPayload> captor = ArgumentCaptor.forClass(QuarantinedPayload.class);
        verify(quarantineRepository).save(captor.capture());
        QuarantinedPayload persisted = captor.getValue();
        assertThat(persisted.getStatus()).isEqualTo(QuarantinedPayload.STATUS_PENDING);
        assertThat(persisted.getEndpoint()).isEqualTo(HIGH_RISK_URI);
        assertThat(persisted.getPayload()).isEqualTo("{\"amount\":50000}");
        assertThat(persisted.getAgentId()).isEqualTo("agent-007");
    }

    @Test
    void TC_GW_02_lowRiskRequestPassesThroughWithoutQuarantine() throws Exception {
        // Requests to non-flagged endpoints must not be intercepted.
        MockHttpServletRequest request = buildRequest("GET", "/api/credit/status", "", null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(quarantineRepository, never()).save(any());
    }

    @Test
    void TC_GW_03_agentReceives202WithPendingStatusAndQuarantineId() throws Exception {
        // After interception the AI agent must receive 202 Accepted containing quarantine_id.
        MockHttpServletRequest request = buildRequest("POST", HIGH_RISK_URI, "{\"amount\":50000}", null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        UUID quarantineId = UUID.randomUUID();
        when(quarantineRepository.save(any())).thenReturn(savedPayload(quarantineId));

        filter.doFilter(request, response, chain);

        verify(acknowledgmentService).send202(eq(response), eq(quarantineId));
    }

    @Test
    void TC_GW_04_clientAbortFlipsStatusToDeliveryTimeoutWarning() throws Exception {
        // If the agent disconnects before receiving the 202, the parked payload
        // must be flagged so the officer knows the agent may retry the action.
        MockHttpServletRequest request = buildRequest("POST", HIGH_RISK_URI, "{\"amount\":50000}", "agent-007");
        MockHttpServletResponse response = new MockHttpServletResponse();

        UUID quarantineId = UUID.randomUUID();
        QuarantinedPayload saved = savedPayload(quarantineId);
        when(quarantineRepository.save(any())).thenReturn(saved);
        doThrow(new ClientAbortException()).when(acknowledgmentService).send202(any(), any());

        filter.doFilter(request, response, chain);

        // Two saves: initial Pending, then the Delivery_Timeout_Warning update
        ArgumentCaptor<QuarantinedPayload> captor = ArgumentCaptor.forClass(QuarantinedPayload.class);
        verify(quarantineRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(1).getStatus())
                .isEqualTo(QuarantinedPayload.STATUS_DELIVERY_TIMEOUT_WARNING);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static MockHttpServletRequest buildRequest(String method, String uri,
                                                       String body, String agentId) {
        MockHttpServletRequest req = new MockHttpServletRequest(method, uri);
        if (!body.isEmpty()) req.setContent(body.getBytes());
        if (agentId != null) req.addHeader("X-Agent-Id", agentId);
        return req;
    }

    private static QuarantinedPayload savedPayload(UUID id) {
        QuarantinedPayload p = new QuarantinedPayload();
        // Simulate what JPA would set after save
        try {
            java.lang.reflect.Field f = QuarantinedPayload.class.getDeclaredField("quarantineId");
            f.setAccessible(true);
            f.set(p, id);
        } catch (Exception ignored) {}
        p.setStatus(QuarantinedPayload.STATUS_PENDING);
        return p;
    }
}
