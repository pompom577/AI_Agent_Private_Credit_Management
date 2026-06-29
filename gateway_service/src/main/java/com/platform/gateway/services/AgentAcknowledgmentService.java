package com.platform.gateway.services;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;

/**
 * Writes a 202 Accepted response to the AI agent confirming the payload is
 * parked for human review (Sub-Story 3.1a).
 *
 * Separated from the filter so it can be independently mocked in TC-GW-04
 * to simulate a ClientAbortException (agent disconnect mid-delivery).
 */
@Service
public class AgentAcknowledgmentService {

    public void send202(HttpServletResponse response, UUID quarantineId) throws IOException {
        response.setStatus(HttpServletResponse.SC_ACCEPTED);
        response.setContentType("application/json");
        response.getWriter().write(String.format(
                "{\"status\":\"Pending\",\"quarantine_id\":\"%s\"," +
                "\"message\":\"Your request has been intercepted and is pending human review.\"}",
                quarantineId));
        response.getWriter().flush();
    }
}
