package com.platform.gateway.messaging;

import com.platform.gateway.dto.ExtractionFailureEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * TC-GW-04: Session isolation — push to User A's deal must reach only User A's SSE emitter;
 * User B's emitter must remain silent even when both are connected simultaneously.
 */
class NotificationBridgeTest {

    private NotificationBridge bridge;
    private ExtractionFailureEvent event;

    @BeforeEach
    void setUp() {
        bridge = new NotificationBridge();

        event = new ExtractionFailureEvent();
        event.setDocId(99L);
        event.setDealId("deal-user-a");
        event.setFilename("InvestCo_Report.pdf");
        event.setStatus("Extraction_Failed_Requires_OCR");
    }

    /**
     * TC-GW-04: Two analysts connected simultaneously.
     * Failure event tied to User A's deal_id must reach only User A — User B receives nothing.
     */
    @Test
    void tcGw04_pushToUserA_doesNotReachUserB() throws IOException {
        SseEmitter emitterA = mock(SseEmitter.class);
        SseEmitter emitterB = mock(SseEmitter.class);

        bridge.register("user-a", emitterA);
        bridge.register("user-b", emitterB);

        bridge.push("user-a", event);

        verify(emitterA).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitterB, never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    /**
     * TC-GW-04 (complement): User B pushing does not bleed into User A's stream.
     */
    @Test
    void tcGw04_pushToUserB_doesNotReachUserA() throws IOException {
        SseEmitter emitterA = mock(SseEmitter.class);
        SseEmitter emitterB = mock(SseEmitter.class);

        bridge.register("user-a", emitterA);
        bridge.register("user-b", emitterB);

        bridge.push("user-b", event);

        verify(emitterB).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitterA, never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    /**
     * Dead-emitter cleanup: if an emitter throws IOException on send, it is removed
     * from the registry and does not poison subsequent pushes.
     */
    @Test
    void deadEmitter_isRemovedAfterIOException() throws IOException {
        SseEmitter deadEmitter = mock(SseEmitter.class);
        SseEmitter liveEmitter = mock(SseEmitter.class);

        org.mockito.Mockito.doThrow(new IOException("broken pipe"))
                .when(deadEmitter).send(any(SseEmitter.SseEventBuilder.class));

        bridge.register("user-a", deadEmitter);
        bridge.register("user-a", liveEmitter);

        bridge.push("user-a", event);  // deadEmitter throws, liveEmitter succeeds
        bridge.push("user-a", event);  // second push: only liveEmitter should be called

        verify(deadEmitter).send(any(SseEmitter.SseEventBuilder.class));       // called once, then removed
        verify(liveEmitter, org.mockito.Mockito.times(2))
                .send(any(SseEmitter.SseEventBuilder.class));                   // called both times
    }
}
