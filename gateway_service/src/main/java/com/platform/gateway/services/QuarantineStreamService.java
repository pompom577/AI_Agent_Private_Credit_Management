package com.platform.gateway.services;

import com.platform.gateway.entities.QuarantinedPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Pushes quarantine queue changes (new payload parked, status flip) to every
 * connected compliance officer dashboard — TC-UI-01.
 *
 * There's no per-user routing here (unlike {@link NotificationBridge}): every
 * officer watching the queue needs to see every change, so emitters are kept
 * in one shared list rather than keyed by user id.
 */
@Service
public class QuarantineStreamService {

    private static final Logger log = LoggerFactory.getLogger(QuarantineStreamService.class);
    private static final long EMITTER_TIMEOUT_MS = 0L; // never time out; client reconnects on error

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emitters.add(emitter);

        Runnable cleanup = () -> emitters.remove(emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> emitters.remove(emitter));

        return emitter;
    }

    public void broadcast(QuarantinedPayload payload) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().data(payload));
            } catch (IOException e) {
                log.warn("Dead quarantine-stream emitter, removing");
                emitters.remove(emitter);
            }
        }
    }
}
