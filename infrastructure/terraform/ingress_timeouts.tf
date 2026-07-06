# Configure the Security Ingress Layer with strict timeout enforcement
resource "aws_lb" "ingress_alb" {
  name               = "hitl-ingress-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.gateway_sg.id]
  subnets            = [aws_subnet.public_subnet.id]

  # Enforce a strict connection timeout ceiling to prevent long hanging connections
  # This acts as the baseline threshold to trigger Sub-Story 3.1b behavior
  idle_timeout = 5 # Set to a low window (e.g., 5 seconds) to catch delayed agents
}

resource "aws_lb_target_group" "gateway_target" {
  name     = "gateway-service-tg"
  port     = 8080
  protocol = "HTTP"
  vpc_id   = aws_vpc.main.id

  health_check {
    path                = "/actuator/health"
    matcher             = "200"
    interval            = 10
    timeout             = 3
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}