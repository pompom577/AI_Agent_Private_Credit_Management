# Configure the API Gateway Router / Load Balancer Timeout
#
# This used to declare its own "main_lb" aws_lb resource (idle_timeout = 20s,
# citing the unrelated TC-BE-04 1.8s SLA) alongside ingress_timeouts.tf's
# "ingress_alb" (idle_timeout = 5s, purpose-built for the Sub-Story 3.1b
# disconnect-detection window). Two ALBs timeout-gating the same gateway
# service with contradictory values made no sense — consolidated onto the
# ingress_timeouts.tf resources, whose 5s ceiling is the one this story
# actually needs to catch delayed/dropped AI agents.
resource "aws_lb_listener" "gateway_listener" {
  load_balancer_arn = aws_lb.ingress_alb.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.gateway_target.arn
  }
}