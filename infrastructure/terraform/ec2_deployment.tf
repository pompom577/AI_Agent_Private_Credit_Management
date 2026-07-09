# =========================================================================
# Task #1 requirement: deploy the application to AWS using an EC2 compute
# service. Single instance running the existing docker-compose.yml stack
# as-is (gateway, classification-service, frontend, redis, xray-daemon) —
# no application changes needed, just a host to run the same containers on.
# =========================================================================

data "aws_ssm_parameter" "amazon_linux_2023" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

# --- IAM role for the EC2 instance -----------------------------------------
# Reuses the S3 policies from iam_roles.tf (previously defined but never
# attached to anything) plus the AWS-managed policies CloudWatch/X-Ray need.
resource "aws_iam_role" "ec2_app_role" {
  name = "gateway-ec2-app-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ec2_s3_write" {
  role       = aws_iam_role.ec2_app_role.name
  policy_arn = aws_iam_policy.gateway_s3_write_policy.arn
}

resource "aws_iam_role_policy_attachment" "ec2_s3_read" {
  role       = aws_iam_role.ec2_app_role.name
  policy_arn = aws_iam_policy.fastapi_s3_read_policy.arn
}

resource "aws_iam_role_policy_attachment" "ec2_cloudwatch_agent" {
  role       = aws_iam_role.ec2_app_role.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}

resource "aws_iam_role_policy_attachment" "ec2_xray_write" {
  role       = aws_iam_role.ec2_app_role.name
  policy_arn = "arn:aws:iam::aws:policy/AWSXRayDaemonWriteAccess"
}

resource "aws_iam_instance_profile" "ec2_app_profile" {
  name = "gateway-ec2-app-profile"
  role = aws_iam_role.ec2_app_role.name
}

# --- Security group ----------------------------------------------------
resource "aws_security_group" "ec2_app_sg" {
  name        = "gateway-ec2-app-sg"
  description = "SSH + app ports for the single-instance docker-compose deployment"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "SSH - restrict cidr_blocks to your own IP/32 before applying"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.ssh_allowed_cidr]
  }

  ingress {
    description = "Frontend (nginx)"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Gateway service direct access"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "gateway-ec2-app-sg"
  }
}

variable "ssh_allowed_cidr" {
  description = "CIDR allowed to SSH into the instance — set to \"<your-ip>/32\", never leave as 0.0.0.0/0 outside a quick demo"
  type        = string
  default     = "0.0.0.0/0"
}

variable "ec2_key_pair_name" {
  description = "Name of an EXISTING EC2 key pair in this account/region (create one in the EC2 console first: Key Pairs > Create key pair)"
  type        = string
}

# --- The instance itself -------------------------------------------------
resource "aws_instance" "app_host" {
  ami                    = data.aws_ssm_parameter.amazon_linux_2023.value
  instance_type          = "t3.micro"
  subnet_id              = aws_subnet.public_subnet.id
  vpc_security_group_ids = [aws_security_group.ec2_app_sg.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2_app_profile.name
  key_name               = var.ec2_key_pair_name

  # Bootstraps Docker + Compose only. The app source itself is deployed
  # separately (scp/rsync or git clone from your own remote — see
  # infrastructure/RUNBOOK.md) since this repo has no git remote baked in.
  user_data = <<-EOF
    #!/bin/bash
    set -euo pipefail
    dnf update -y
    dnf install -y docker amazon-cloudwatch-agent
    systemctl enable docker
    systemctl start docker
    usermod -aG docker ec2-user

    curl -SL https://github.com/docker/compose/releases/download/v2.29.7/docker-compose-linux-x86_64 \
      -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose

    # Task #2 Part 3: EC2 doesn't publish memory/disk metrics by default —
    # only the CloudWatch agent can, hence installing + configuring it here.
    cat > /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json <<'CWCONFIG'
    {
      "metrics": {
        "namespace": "PrivateCreditPipeline/EC2",
        "metrics_collected": {
          "mem": { "measurement": ["mem_used_percent"] },
          "disk": { "measurement": ["used_percent"], "resources": ["/"] },
          "cpu": { "measurement": ["cpu_usage_active"], "totalcpu": true }
        }
      }
    }
    CWCONFIG

    /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
      -a fetch-config -m ec2 -s \
      -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json
  EOF

  tags = {
    Name = "private-credit-app-host"
  }
}

# Wires the EC2 instance into the ALB defined in ingress_timeouts.tf so the
# strict 5s idle-timeout / health-check enforcement from Sub-Story 3.1b
# applies to real traffic once this is deployed.
resource "aws_lb_target_group_attachment" "app_host_attachment" {
  target_group_arn = aws_lb_target_group.gateway_target.arn
  target_id        = aws_instance.app_host.id
  port             = 8080
}

output "ec2_public_ip" {
  value = aws_instance.app_host.public_ip
}

output "ec2_ssh_command" {
  value = "ssh -i <your-key.pem> ec2-user@${aws_instance.app_host.public_ip}"
}
