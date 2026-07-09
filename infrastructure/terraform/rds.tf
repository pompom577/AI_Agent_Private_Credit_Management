# =========================================================================
# Task #1 requirement: AWS cloud database service (RDS) backing the gateway's
# JPA/Hibernate persistence layer, replacing the Neon-hosted Postgres used
# during local development. Same engine (Postgres), same schema.sql — this
# is a drop-in connection-string swap, no application code changes needed.
# =========================================================================

resource "aws_db_subnet_group" "gateway_db_subnets" {
  name       = "gateway-db-subnet-group"
  subnet_ids = [aws_subnet.public_subnet.id, aws_subnet.private_subnet.id]

  tags = {
    Name = "gateway-db-subnet-group"
  }
}

# Only the Spring Boot gateway (gateway_sg) may reach Postgres — RDS sits
# inside the VPC with no public endpoint.
resource "aws_security_group" "rds_sg" {
  name        = "gateway-rds-security-group"
  description = "Allows Postgres traffic from the gateway service only"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.gateway_sg.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "gateway-rds-sg"
  }
}

resource "aws_db_instance" "gateway_db" {
  identifier     = "private-credit-gateway-db"
  engine         = "postgres"
  engine_version = "16.4"

  # db.t3.micro / 20GB is the AWS Free Tier eligible shape — sufficient for
  # coursework-scale traffic; bump instance_class/allocated_storage for
  # anything beyond a demo.
  instance_class    = "db.t3.micro"
  allocated_storage = 20
  storage_type      = "gp3"

  db_name  = "private_credit_db"
  username = var.db_username
  password = var.db_password
  port     = 5432

  db_subnet_group_name   = aws_db_subnet_group.gateway_db_subnets.name
  vpc_security_group_ids = [aws_security_group.rds_sg.id]
  publicly_accessible    = false

  # Coursework-scale settings — dial these up for anything long-lived.
  backup_retention_period = 1
  skip_final_snapshot     = true
  deletion_protection     = false
  multi_az                = false

  tags = {
    Name = "private-credit-gateway-db"
  }
}

variable "db_username" {
  description = "Master username for the RDS Postgres instance"
  type        = string
  default     = "platform_admin"
}

variable "db_password" {
  description = "Master password for the RDS Postgres instance — pass via TF_VAR_db_password, never commit a real value"
  type        = string
  sensitive   = true
}

output "rds_endpoint" {
  description = "Feed this into DB_ENDPOINT / SPRING_DATASOURCE_URL for the gateway service"
  value       = aws_db_instance.gateway_db.address
}
