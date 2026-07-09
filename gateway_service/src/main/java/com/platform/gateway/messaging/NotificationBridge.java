package com.platform.gateway.messaging;

import com.platform.gateway.dto.ExtractionFailureEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per-user SSE emitters and routes extraction failure events
 * only to the analyst who owns the deal (Story 1.3, Person 3 — TC-GW-03/04).
 */
@Component
public class NotificationBridge {

    private static final Logger log = LoggerFactory.getLogger(NotificationBridge.class);

    private final ConcurrentHashMap<String, List<SseEmitter>> emittersByUserId = new ConcurrentHashMap<>();

    public void register(String userId, SseEmitter emitter) {
        emittersByUserId.computeIfAbsent(userId, k -> new ArrayList<>()).add(emitter);

        Runnable cleanup = () -> removeEmitter(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> removeEmitter(userId, emitter));

        log.info("SSE emitter registered: userId={}", userId);
    }

    public void push(String userId, ExtractionFailureEvent event) {
        List<SseEmitter> emitters = emittersByUserId.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            log.warn("No active SSE emitters for userId={}", userId);
            return;
        }

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("extraction-failure")
                        .data(event));
            } catch (IOException e) {
                log.warn("Dead SSE emitter for userId={}, removing", userId);
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }

    private void removeEmitter(String userId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByUserId.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
        }
    }
}
