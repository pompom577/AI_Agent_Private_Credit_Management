"""
Task #2 Part 2 — serverless entry point into the same compliance-alerts SNS
topic the Spring Boot gateway's ComplianceAlertService publishes to directly
(see gateway_service/.../services/ComplianceAlertService.java). Both paths
converge on the one topic/Slack-webhook subscription, so this Lambda extends
the existing server-based alerting architecture rather than duplicating it —
it just gives anything that can make an HTTP call (external monitoring, a
Postman smoke test, a future "send test alert" button) a way to dispatch a
HITL compliance alert without needing AWS SDK credentials of its own.

Invoked by API Gateway (HTTP API, payload format 2.0) — see serverless.tf.
"""
import json
import os

import boto3

sns = boto3.client("sns")
TOPIC_ARN = os.environ["COMPLIANCE_ALERTS_TOPIC_ARN"]


def handler(event, context):
    try:
        body = json.loads(event.get("body") or "{}")
    except json.JSONDecodeError:
        return _response(400, {"error": "body must be valid JSON"})

    agent_id = body.get("agent_id")
    quarantine_id = body.get("quarantine_id")
    message = body.get("message", "Delivery_Timeout_Warning alert")

    if not agent_id or not quarantine_id:
        return _response(400, {"error": "agent_id and quarantine_id are required"})

    sns.publish(
        TopicArn=TOPIC_ARN,
        Subject="HITL: unstable agent - possible duplicate submission",
        Message=f"{message} agent_id={agent_id} quarantine_id={quarantine_id}",
    )

    return _response(202, {"status": "published", "quarantine_id": quarantine_id})


def _response(status_code, body):
    return {
        "statusCode": status_code,
        "headers": {"Content-Type": "application/json"},
        "body": json.dumps(body),
    }
