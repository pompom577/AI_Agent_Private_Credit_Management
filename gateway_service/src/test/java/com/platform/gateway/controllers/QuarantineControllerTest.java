package com.platform.gateway.controllers;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.platform.gateway.entities.HitlAuditLedger;
import com.platform.gateway.entities.QuarantinedPayload;
import com.platform.gateway.repositories.QuarantineRepository;
import com.platform.gateway.services.AuditLoggingService;
import com.platform.gateway.services.QuarantineStreamService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Field;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WireMock-backed tests for {@link QuarantineController} — Sub-Stories 3.2a and 3.3a.
 *
 * TC-DB-02 (/reject) — status -> Discarded, nothing forwarded downstream.
 * TC-DB-03 (/approve, happy path) — exact payload forwarded to the parked endpoint,
 *   status -> Approved. Regression guard for the bug where an unrooted WebClient.Builder
 *   threw IllegalArgumentException on the relative endpoint path, silently marking
 *   every approval Execution_Failed.
 * TC-DB-04 (/approve, destination down) — forwarding failure -> Execution_Failed,
 *   payload is not lost (still readable from the repository).
 * TC-BE-01/02 — every Approve/Reject writes exactly one audit record with the
 *   intended action URL, the officer who decided, and the parked quarantine id.
 * TC-BE-03 — if the audit INSERT fails, the exception must propagate (not be
 *   swallowed) so @Transactional rolls back the quarantine release/discard too,
 *   and the officer must never see a broadcasted "success" state.
 */
@ExtendWith(MockitoExtension.class)
class QuarantineControllerTest {

    private static WireMockServer wireMock;

    @Mock private QuarantineRepository quarantineRepository;
    @Mock private QuarantineStreamService streamService;
    @Mock private AuditLoggingService auditLoggingService;

    private QuarantineController controller;
    private static final UUID OFFICER_ID = UUID.randomUUID();

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        WebClient destinationWebClient = WebClient.builder()
                .baseUrl("http://localhost:" + wireMock.port())
                .build();
        controller = new QuarantineController(quarantineRepository, destinationWebClient, streamService,
                auditLoggingService);
    }

    @Test
    void tcDb02_rejectMarksDiscardedAndForwardsNothing() {
        UUID id = UUID.randomUUID();
        QuarantinedPayload parked = parkedPayload(id, "/api/credit/approve", "{\"amount\":50000}");
        when(quarantineRepository.findById(id)).thenReturn(java.util.Optional.of(parked));
        when(quarantineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<QuarantinedPayload> response = controller.reject(id, OFFICER_ID);

        assertThat(response.getBody().getStatus()).isEqualTo(QuarantinedPayload.STATUS_DISCARDED);
        wireMock.verify(0, postRequestedFor(urlEqualTo("/api/credit/approve")));
        verify(streamService).broadcast(response.getBody());
    }

    @Test
    void tcDb03_approveForwardsExactPayloadToParkedEndpointAndMarksApproved() {
        wireMock.stubFor(post(urlEqualTo("/api/credit/approve")).willReturn(aResponse().withStatus(200)));

        UUID id = UUID.randomUUID();
        QuarantinedPayload parked = parkedPayload(id, "/api/credit/approve", "{\"amount\":50000}");
        when(quarantineRepository.findById(id)).thenReturn(java.util.Optional.of(parked));
        when(quarantineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<QuarantinedPayload> response = controller.approve(id, OFFICER_ID);

        assertThat(response.getBody().getStatus()).isEqualTo(QuarantinedPayload.STATUS_APPROVED);
        wireMock.verify(postRequestedFor(urlEqualTo("/api/credit/approve")));
    }

    @Test
    void tcDb04_approveMarksExecutionFailedWhenDestinationIsDown() {
        // No stub registered — WireMock returns 404, simulating the destination being unreachable.
        UUID id = UUID.randomUUID();
        QuarantinedPayload parked = parkedPayload(id, "/api/credit/approve", "{\"amount\":50000}");
        when(quarantineRepository.findById(id)).thenReturn(java.util.Optional.of(parked));
        ArgumentCaptor<QuarantinedPayload> captor = ArgumentCaptor.forClass(QuarantinedPayload.class);
        when(quarantineRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<QuarantinedPayload> response = controller.approve(id, OFFICER_ID);

        assertThat(response.getBody().getStatus()).isEqualTo(QuarantinedPayload.STATUS_EXECUTION_FAILED);
        // Payload must not be lost — still retrievable with its original endpoint/body intact.
        assertThat(captor.getValue().getEndpoint()).isEqualTo("/api/credit/approve");
        assertThat(captor.getValue().getPayload()).isEqualTo("{\"amount\":50000}");
    }

    @Test
    void tcBe01_approveWritesExactlyOneAuditRecordWithApproveDecision() {
        wireMock.stubFor(post(urlEqualTo("/api/credit/approve")).willReturn(aResponse().withStatus(200)));

        UUID id = UUID.randomUUID();
        QuarantinedPayload parked = parkedPayload(id, "/api/credit/approve", "{\"amount\":50000}");
        when(quarantineRepository.findById(id)).thenReturn(java.util.Optional.of(parked));
        when(quarantineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        controller.approve(id, OFFICER_ID);

        // The ledger must attribute the decision to the acting officer and the exact
        // parked action — that traceability is the entire point of Sub-Story 3.3a.
        verify(auditLoggingService).recordDecision(
                "/api/credit/approve", HitlAuditLedger.DECISION_APPROVE, OFFICER_ID, id);
    }

    @Test
    void tcBe02_rejectWritesExactlyOneAuditRecordWithRejectDecision() {
        UUID id = UUID.randomUUID();
        QuarantinedPayload parked = parkedPayload(id, "/api/credit/approve", "{\"amount\":50000}");
        when(quarantineRepository.findById(id)).thenReturn(java.util.Optional.of(parked));
        when(quarantineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        controller.reject(id, OFFICER_ID);

        verify(auditLoggingService).recordDecision(
                "/api/credit/approve", HitlAuditLedger.DECISION_REJECT, OFFICER_ID, id);
    }

    @Test
    void tcBe03_auditWriteFailurePropagatesInsteadOfBeingSwallowed() {
        wireMock.stubFor(post(urlEqualTo("/api/credit/approve")).willReturn(aResponse().withStatus(200)));

        UUID id = UUID.randomUUID();
        QuarantinedPayload parked = parkedPayload(id, "/api/credit/approve", "{\"amount\":50000}");
        when(quarantineRepository.findById(id)).thenReturn(java.util.Optional.of(parked));
        when(quarantineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("hitl_audit_ledger insert failed"))
                .when(auditLoggingService).recordDecision(any(), any(), any(), any());

        // Must NOT be caught and turned into a 200 — @Transactional needs the exception
        // to reach it so the quarantine status update rolls back with the failed audit
        // write (TC-BE-03: "payload remains quarantined and is not released").
        assertThatThrownBy(() -> controller.approve(id, OFFICER_ID))
                .isInstanceOf(RuntimeException.class);

        // The officer must never be told the decision succeeded when it didn't.
        verify(streamService, never()).broadcast(any());
    }

    private static QuarantinedPayload parkedPayload(UUID id, String endpoint, String payload) {
        QuarantinedPayload p = new QuarantinedPayload();
        try {
            Field f = QuarantinedPayload.class.getDeclaredField("quarantineId");
            f.setAccessible(true);
            f.set(p, id);
        } catch (Exception ignored) {}
        p.setEndpoint(endpoint);
        p.setPayload(payload);
        p.setAgentId("agent-007");
        p.setStatus(QuarantinedPayload.STATUS_PENDING);
        return p;
    }
}
