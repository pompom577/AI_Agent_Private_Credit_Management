# =========================================================================
# Task #2 Part 3: monitor server, application, and cloud services'
# performance via CloudWatch. Alarms mirror the two spots that already
# have a "the pipeline is stuck" meaning elsewhere in this system:
#   - EC2 CPU: the single app host is overloaded
#   - Lambda errors: the serverless alert dispatcher (serverless.tf) is
#     failing, same "an alert silently didn't go out" risk as TC-OPS-01
#     for hitl_audit_ledger write failures.
# =========================================================================

resource "aws_sns_topic" "ops_alarms" {
  name = "private-credit-ops-alarms"
}

resource "aws_cloudwatch_metric_alarm" "ec2_cpu_high" {
  alarm_name          = "app-host-cpu-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods   = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = 300
  statistic           = "Average"
  threshold           = 80
  alarm_description   = "app_host EC2 CPU above 80% for 10 minutes"
  dimensions = {
    InstanceId = aws_instance.app_host.id
  }
  alarm_actions = [aws_sns_topic.ops_alarms.arn]
}

resource "aws_cloudwatch_metric_alarm" "ec2_memory_high" {
  alarm_name          = "app-host-memory-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods   = 2
  metric_name         = "mem_used_percent"
  namespace           = "PrivateCreditPipeline/EC2"
  period              = 300
  statistic           = "Average"
  threshold           = 85
  alarm_description   = "app_host memory above 85% for 10 minutes (CloudWatch agent metric)"
  dimensions = {
    InstanceId = aws_instance.app_host.id
  }
  alarm_actions = [aws_sns_topic.ops_alarms.arn]
  treat_missing_data = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "rds_cpu_high" {
  alarm_name          = "gateway-db-cpu-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods   = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/RDS"
  period              = 300
  statistic           = "Average"
  threshold           = 80
  alarm_description   = "gateway RDS CPU above 80% for 10 minutes"
  dimensions = {
    DBInstanceIdentifier = aws_db_instance.gateway_db.id
  }
  alarm_actions = [aws_sns_topic.ops_alarms.arn]
}

resource "aws_cloudwatch_metric_alarm" "compliance_lambda_errors" {
  alarm_name          = "compliance-alert-lambda-errors"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods   = 1
  metric_name         = "Errors"
  namespace           = "AWS/Lambda"
  period              = 300
  statistic           = "Sum"
  threshold           = 0
  alarm_description   = "compliance-alert-dispatcher Lambda threw at least one error"
  dimensions = {
    FunctionName = aws_lambda_function.compliance_alert_dispatcher.function_name
  }
  alarm_actions = [aws_sns_topic.ops_alarms.arn]
}

resource "aws_cloudwatch_dashboard" "compliance_health" {
  dashboard_name = "private-credit-pipeline-health"

  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "metric"
        x      = 0
        y      = 0
        width  = 12
        height = 6
        properties = {
          title  = "EC2 — CPU / Memory"
          region = "us-east-1"
          metrics = [
            ["AWS/EC2", "CPUUtilization", "InstanceId", aws_instance.app_host.id],
            ["PrivateCreditPipeline/EC2", "mem_used_percent", "InstanceId", aws_instance.app_host.id]
          ]
          period = 300
          stat   = "Average"
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 0
        width  = 12
        height = 6
        properties = {
          title  = "RDS — CPU / Connections"
          region = "us-east-1"
          metrics = [
            ["AWS/RDS", "CPUUtilization", "DBInstanceIdentifier", aws_db_instance.gateway_db.id],
            ["AWS/RDS", "DatabaseConnections", "DBInstanceIdentifier", aws_db_instance.gateway_db.id]
          ]
          period = 300
          stat   = "Average"
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 6
        width  = 12
        height = 6
        properties = {
          title  = "Lambda — Invocations / Errors / Duration"
          region = "us-east-1"
          metrics = [
            ["AWS/Lambda", "Invocations", "FunctionName", aws_lambda_function.compliance_alert_dispatcher.function_name],
            ["AWS/Lambda", "Errors", "FunctionName", aws_lambda_function.compliance_alert_dispatcher.function_name],
            ["AWS/Lambda", "Duration", "FunctionName", aws_lambda_function.compliance_alert_dispatcher.function_name]
          ]
          period = 300
          stat   = "Sum"
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 6
        width  = 12
        height = 6
        properties = {
          title  = "ALB — Request Count / Target Response Time"
          region = "us-east-1"
          metrics = [
            ["AWS/ApplicationELB", "RequestCount", "LoadBalancer", aws_lb.ingress_alb.arn_suffix],
            ["AWS/ApplicationELB", "TargetResponseTime", "LoadBalancer", aws_lb.ingress_alb.arn_suffix]
          ]
          period = 300
          stat   = "Sum"
        }
      }
    ]
  })
}

output "cloudwatch_dashboard_url" {
  value = "https://us-east-1.console.aws.amazon.com/cloudwatch/home?region=us-east-1#dashboards:name=${aws_cloudwatch_dashboard.compliance_health.dashboard_name}"
}
