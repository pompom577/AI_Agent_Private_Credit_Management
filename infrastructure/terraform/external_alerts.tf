# Create an AWS SNS Topic for Compliance Alerts
resource "aws_sns_topic" "compliance_alerts" {
  name = "hitl-delivery-timeout-alerts"
}

# Bind an Email subscription to bypass the non-responsive Slack webhook timeout
resource "aws_sns_topic_subscription" "slack_webhook_subscription" {
  topic_arn = aws_sns_topic.compliance_alerts.arn
  protocol  = "email"
  endpoint  = "yaphueyshin04@gmail.com"
}

# Output the Topic ARN so your backend database service can call it
output "sns_topic_arn" {
  value       = aws_sns_topic.compliance_alerts.arn
  description = "Backend event listener hooks to publish alerts here"
}