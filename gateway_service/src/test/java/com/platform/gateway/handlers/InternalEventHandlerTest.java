package com.platform.gateway.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.gateway.config.WebSocketConfig;
import com.platform.gateway.dto.ExtractionFailureEvent;
import com.platform.gateway.entities.DealState;
import com.platform.gateway.exceptions.GlobalExceptionHandler;
import com.platform.gateway.messaging.NotificationBridge;
import com.platform.gateway.repositories.DealStateRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TC-GW-03: POST /webhooks/extraction-failed routes the event to the correct analyst's SSE stream.
 * TC-GW-03 (SSE): GET /sse/extraction-updates establishes a text/event-stream connection.
 */
@WebMvcTest(InternalEventHandler.class)
@Import({WebSocketConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class InternalEventHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    NotificationBridge notificationBridge;

    @MockBean
    DealStateRepository dealStateRepository;

    /**
     * TC-GW-03: Extraction failure payload from FastAPI must be forwarded only to the
     * analyst who owns the deal — identified by uploaded_by_user_id on the DealState.
     */
    @Test
    void tcGw03_extractionFailedWebhook_pushesToDealOwner() throws Exception {
        DealState deal = new DealState();
        deal.setDealId("deal-abc");
        deal.setUploadedByUserId("analyst-1");
        deal.setBucketUrl("s3://bucket/deal-abc");

        when(dealStateRepository.findById("deal-abc")).thenReturn(Optional.of(deal));

        String payload = """
                {
                  "doc_id": 1,
                  "deal_id": "deal-abc",
                  "filename": "InvestCo_Report.pdf",
                  "status": "Extraction_Failed_Requires_OCR"
                }
                """;

        mockMvc.perform(post("/webhooks/extraction-failed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        ArgumentCaptor<ExtractionFailureEvent> eventCaptor =
                ArgumentCaptor.forClass(ExtractionFailureEvent.class);
        verify(notificationBridge).push(eq("analyst-1"), eventCaptor.capture());

        ExtractionFailureEvent captured = eventCaptor.getValue();
        assertThat(captured.getDocId()).isEqualTo(1L);
        assertThat(captured.getDealId()).isEqualTo("deal-abc");
        assertThat(captured.getFilename()).isEqualTo("InvestCo_Report.pdf");
        assertThat(captured.getStatus()).isEqualTo("Extraction_Failed_Requires_OCR");
    }

    /**
     * TC-GW-03 (SSE endpoint): GET /sse/extraction-updates must return 200 and register the
     * caller's emitter in NotificationBridge so subsequent pushes can reach this client.
     * (MockMvc does not populate Content-Type for streaming SseEmitter responses; that header
     * is verified at the integration level against a live server.)
     */
    @Test
    void tcGw03_sseSubscribe_registersEmitterAndReturns200() throws Exception {
        mockMvc.perform(get("/sse/extraction-updates").param("userId", "analyst-1"))
                .andExpect(status().isOk());

        verify(notificationBridge).register(eq("analyst-1"), any());
    }
}
