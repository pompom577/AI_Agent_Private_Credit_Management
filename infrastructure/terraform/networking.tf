# =========================================================================
# 1. Create a Secure Virtual Private Cloud (VPC)
# =========================================================================
resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "private-credit-pipeline-vpc"
  }
}

# =========================================================================
# 2. Public Subnet (For Frontend and Spring Boot Gateway to accept traffic)
# =========================================================================
resource "aws_subnet" "public_subnet" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "us-east-1a"
  map_public_ip_on_launch = true

  tags = {
    Name = "pipeline-public-subnet"
  }
}

# =========================================================================
# 3. Private Subnet (For Isolated FastAPI Classification Service)
# =========================================================================
resource "aws_subnet" "private_subnet" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.2.0/24"
  availability_zone = "us-east-1b"

  tags = {
    Name = "pipeline-private-subnet"
  }
}

# =========================================================================
#  Wege 4. Internet Gateway & Public Routing
# =========================================================================
resource "aws_internet_gateway" "gw" {
  vpc_id = aws_vpc.main.id
  tags   = { Name = "pipeline-igw" }
}

resource "aws_route_table" "public_rt" {
  vpc_id = aws_vpc.main.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.gw.id
  }
}

resource "aws_route_table_association" "public_assoc" {
  subnet_id      = aws_subnet.public_subnet.id
  route_table_id = aws_route_table.public_rt.id
}

# =========================================================================
# 5. S3 VPC Gateway Endpoint (Private tunnel straight to S3 - $0.00 Cost)
# =========================================================================
resource "aws_vpc_endpoint" "s3" {
  vpc_id            = aws_vpc.main.id
  service_name      = "com.amazonaws.us-east-1.s3"
  vpc_endpoint_type = "Gateway"
  route_table_ids   = [aws_route_table.public_rt.id]

  tags = {
    Name = "pipeline-s3-endpoint"
  }
}

# =========================================================================
# 6. Security Group for Ingestion Engine (FastAPI Classification Service)
# =========================================================================
resource "aws_security_group" "classification_sg" {
  name        = "classification-security-group"
  description = "Controls internal routing for Python FastAPI container"
  vpc_id      = aws_vpc.main.id

  # OUTBOUND: Allows Python to call outside to Gemini API, Neon DB, and Webhooks
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "classification-service-sg"
  }
}

# =========================================================================
# 7. Security Group for the API Gateway (Spring Boot)
# =========================================================================
resource "aws_security_group" "gateway_sg" {
  name        = "gateway-security-group"
  description = "Allows traffic to API Gateway"
  vpc_id      = aws_vpc.main.id

  # Standard public web traffic ingress rule to reach Spring Boot
  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # UNBLOCK INTERNAL TRAFFIC: Explicitly allow Python container to hit your webhook on port 8080
  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.classification_sg.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "gateway-service-sg"
  }
}

# =========================================================================
# 8. Security Group for High-Risk Destination Endpoints (TC-SEC-01)
# =========================================================================
# Zero-trust boundary for the internal services the HITL flow forwards
# approved payloads to (/api/credit/approve, /api/credit/transfer,
# /api/trade/execute). Only PayloadExecutionService running under
# gateway_sg may reach them; direct traffic from the AI agent subnet
# (or anywhere else) is never permitted.
resource "aws_security_group" "destination_sg" {
  name        = "hitl-destination-security-group"
  description = "Restricts high-risk destination endpoints to the Gateway's execution service only"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port       = 0
    to_port         = 0
    protocol        = "-1"
    security_groups = [aws_security_group.gateway_sg.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "hitl-destination-sg"
  }
}