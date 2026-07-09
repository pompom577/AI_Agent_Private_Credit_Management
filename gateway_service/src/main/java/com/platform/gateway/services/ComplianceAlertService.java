package com.platform.gateway.services;

import com.platform.gateway.entities.QuarantinedPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * Pushes a Delivery_Timeout_Warning alert to the compliance Slack/Teams channel
 * via the SNS topic Person 4 provisions in {@code external_alerts.tf} — TC-SEC-02.
 */
@Service
public class ComplianceAlertService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceAlertService.class);

    private final SnsClient snsClient;
    private final String topicArn;

    public ComplianceAlertService(SnsClient snsClient,
                                  @Value("${gateway.hitl.alert-topic-arn}") String topicArn) {
        this.snsClient = snsClient;
        this.topicArn = topicArn;
    }

    public void alertDeliveryTimeout(QuarantinedPayload payload) {
        if (topicArn == null || topicArn.isBlank()) {
            log.warn("gateway.hitl.alert-topic-arn not configured — skipping SNS alert for quarantine_id={}",
                    payload.getQuarantineId());
            return;
        }

        String message = String.format(
                "Delivery_Timeout_Warning: agent_id=%s may retry a duplicate action. quarantine_id=%s endpoint=%s",
                payload.getAgentId(), payload.getQuarantineId(), payload.getEndpoint());

        snsClient.publish(PublishRequest.builder()
                .topicArn(topicArn)
                .subject("HITL: unstable agent — possible duplicate submission")
                .message(message)
                .build());
    }
}
