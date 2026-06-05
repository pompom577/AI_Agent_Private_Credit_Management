# =========================================================================
# Secret inputs — provide via terraform.tfvars (gitignored) or TF_VAR_* env vars.
# Never hardcode real values here.
# =========================================================================
variable "google_api_key" {
  type        = string
  sensitive   = true
  description = "Google Gemini API key"
}

variable "database_url" {
  type        = string
  sensitive   = true
  description = "PostgreSQL connection string for the application"
}

# =========================================================================
# Create the Secrets Vault Container
# =========================================================================
resource "aws_secretsmanager_secret" "app_secrets" {
  name                    = "dev-private-credit-pipeline-secrets"
  description             = "Production-grade hidden values for Gemini API and Neon DB"
  recovery_window_in_days = 0 
}

# =========================================================================
# Store Key-Value Secret Data 
# =========================================================================
resource "aws_secretsmanager_secret_version" "app_secrets_vals" {
  secret_id     = aws_secretsmanager_secret.app_secrets.id
  secret_string = jsonencode({
    GOOGLE_API_KEY = var.google_api_key
    DATABASE_URL   = var.database_url
  })
}

# =========================================================================
# Create IAM policy so your cloud containers can read this secret later
# =========================================================================
resource "aws_iam_policy" "secrets_read_policy" {
  name        = "dev-pipeline-secrets-read-policy"
  description = "Allows containers to pull keys dynamically into memory"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["secretsmanager:GetSecretValue"]
        Resource = [aws_secretsmanager_secret.app_secrets.arn]
      }
    ]
  })
}

# =========================================================================
# Output the Secret ARN
# =========================================================================
output "secrets_manager_arn" {
  value       = aws_secretsmanager_secret.app_secrets.arn
  description = "The Amazon Resource Name for your newly created secret vault"
}