# Pre-existing gap fixed here: redis_sg / redis_subnets were referenced below
# but never defined anywhere, so this file could never actually apply.
resource "aws_security_group" "redis_sg" {
  name        = "gateway-redis-security-group"
  description = "Allows Redis traffic from the gateway service only"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port       = 6379
    to_port         = 6379
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
    Name = "gateway-redis-sg"
  }
}

resource "aws_elasticache_subnet_group" "redis_subnets" {
  name       = "gateway-redis-subnet-group"
  subnet_ids = [aws_subnet.public_subnet.id, aws_subnet.private_subnet.id]
}

resource "aws_elasticache_cluster" "redis_gateway_cache" {
  cluster_id           = "ddac-gateway-cache"
  engine               = "redis"
  node_type            = "cache.t4g.micro" # Cost-effective, high-performance ARM instance
  num_cache_nodes      = 1
  parameter_group_name = aws_elasticache_parameter_group.redis_params.name
  port                 = 6379
  security_group_ids   = [aws_security_group.redis_sg.id]
  subnet_group_name    = aws_elasticache_subnet_group.redis_subnets.name

  tags = {
    Environment = "Production"
    Role        = "Person-4-Infrastructure"
  }
}

# CRITICAL: Configure the Max Memory Eviction Policy to prevent crashes (TC-INF-03)
resource "aws_elasticache_parameter_group" "redis_params" {
  name   = "ddac-redis-params"
  family = "redis7"

  # Use Least Recently Used (LRU) logic to evict old cached pages when memory limit is reached
  parameter {
    name  = "maxmemory-policy"
    value = "allkeys-lru"
  }
}