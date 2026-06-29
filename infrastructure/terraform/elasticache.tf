resource "aws_elasticache_cluster" "redis_gateway_cache" {
  cluster_id           = "ddac-gateway-cache"
  engine               = "redis"
  node_type            = "cache.t4g.micro" # Cost-effective, high-performance ARM instance
  num_cache_nodes      = 1
  parameter_group_name = "default.redis7"
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