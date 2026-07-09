# =========================================================================
# Task #2 Part 2: serverless architecture (Amazon API Gateway + AWS Lambda)
# integrated into the existing server-based system via the SNS topic already
# defined in external_alerts.tf — both the Spring Boot gateway's
# ComplianceAlertService and this Lambda publish to the same
# compliance_alerts topic, converging on the one Slack/Teams subscription.
# =========================================================================

data "archive_file" "compliance_alert_lambda_zip" {
  type        = "zip"
  source_file = "${path.module}/../lambda/compliance_alert_dispatcher/handler.py"
  output_path = "${path.module}/../lambda/compliance_alert_dispatcher.zip"
}

resource "aws_iam_role" "compliance_alert_lambda_role" {
  name = "compliance-alert-lambda-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "lambda.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "compliance_alert_lambda_logs" {
  role       = aws_iam_role.compliance_alert_lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy" "compliance_alert_lambda_sns_publish" {
  name = "compliance-alert-lambda-sns-publish"
  role = aws_iam_role.compliance_alert_lambda_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = "sns:Publish"
      Resource = aws_sns_topic.compliance_alerts.arn
    }]
  })
}

resource "aws_lambda_function" "compliance_alert_dispatcher" {
  function_name = "compliance-alert-dispatcher"
  role          = aws_iam_role.compliance_alert_lambda_role.arn
  runtime       = "python3.12"
  handler       = "handler.handler"

  filename         = data.archive_file.compliance_alert_lambda_zip.output_path
  source_code_hash = data.archive_file.compliance_alert_lambda_zip.output_base64sha256

  timeout = 5

  environment {
    variables = {
      COMPLIANCE_ALERTS_TOPIC_ARN = aws_sns_topic.compliance_alerts.arn
    }
  }

  tracing_config {
    # Task #2 Part 3: shows up on the same X-Ray service map as the gateway.
    mode = "Active"
  }

  tags = {
    Name = "compliance-alert-dispatcher"
  }
}

# --- API Gateway (HTTP API — simpler/cheaper than REST API for a single route) ---
resource "aws_apigatewayv2_api" "compliance_alerts_api" {
  name          = "compliance-alerts-api"
  protocol_type = "HTTP"
}

resource "aws_apigatewayv2_integration" "compliance_alert_lambda_integration" {
  api_id                 = aws_apigatewayv2_api.compliance_alerts_api.id
  integration_type       = "AWS_PROXY"
  integration_uri        = aws_lambda_function.compliance_alert_dispatcher.invoke_arn
  payload_format_version = "2.0"
}

resource "aws_apigatewayv2_route" "compliance_alert_route" {
  api_id    = aws_apigatewayv2_api.compliance_alerts_api.id
  route_key = "POST /alerts/compliance"
  target    = "integrations/${aws_apigatewayv2_integration.compliance_alert_lambda_integration.id}"
}

resource "aws_apigatewayv2_stage" "compliance_alerts_default_stage" {
  api_id      = aws_apigatewayv2_api.compliance_alerts_api.id
  name        = "$default"
  auto_deploy = true
}

resource "aws_lambda_permission" "allow_apigateway_invoke" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.compliance_alert_dispatcher.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.compliance_alerts_api.execution_arn}/*/*"
}

output "compliance_alerts_api_url" {
  description = "curl -X POST <this>/alerts/compliance -d '{\"agent_id\":\"agent-007\",\"quarantine_id\":\"<uuid>\",\"message\":\"test\"}'"
  value       = "${aws_apigatewayv2_stage.compliance_alerts_default_stage.invoke_url}/alerts/compliance"
}
