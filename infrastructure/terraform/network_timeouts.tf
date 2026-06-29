# Configure the API Gateway Router / Load Balancer Timeout
resource "aws_lb_listener" "gateway_listener" {
  load_balancer_arn = aws_lb.main_lb.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.gateway_tg.arn
  }
}

resource "aws_lb" "main_lb" {
  name               = "ddac-api-gateway"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.lb_sg.id]
  subnets            = aws_subnet.public_subnets[*].id

  # SLA Requirement: Drop connections if downstream storage/services stall 
  # This matches the backend 1.8-second fallback rule (TC-BE-04)
  idle_timeout = 20 
}