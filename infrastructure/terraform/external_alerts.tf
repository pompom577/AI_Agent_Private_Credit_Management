# Create an AWS SNS Topic for Compliance Alerts
resource "aws_sns_topic" "compliance_alerts" {
  name = "hitl-delivery-timeout-alerts"
}

# Bind an HTTPS Webhook to automatically push alerts straight into the Slack Channel
resource "aws_sns_topic_subscription" "slack_webhook_subscription" {
  topic_arn              = aws_sns_topic.compliance_alerts.arn
  protocol               = "https"
  endpoint_auto_confirms = true
  
  # Replace with your actual team Slack / Teams inbound webhook URL endpoint
  endpoint = "https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX"
}

# Output the Topic ARN so your backend database service can call it when generating 
# a 'Delivery_Timeout_Warning' execution event (TC-SEC-02)
output "sns_topic_arn" {
  value       = aws_sns_topic.compliance_alerts.arn
  description = "Backend event listener hooks to publish alerts here"
}