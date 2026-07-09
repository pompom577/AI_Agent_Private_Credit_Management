# --- SPRING BOOT GATEWAY POLICY: WRITE ACCESS ONLY ---
resource "aws_iam_policy" "gateway_s3_write_policy" {
  name        = "gateway-s3-write-policy-v2"
  description = "Allows Spring Boot Gateway to stream file chunks securely."

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = [
        "s3:PutObject",
        "s3:AbortMultipartUpload"
      ]
      Resource = "${aws_s3_bucket.deal_ingestion_bucket.arn}/*"
    }]
  })
}

# --- FASTAPI MICROSERVICE POLICY: READ ACCESS ONLY ---
resource "aws_iam_policy" "fastapi_s3_read_policy" {
  name        = "fastapi-s3-read-policy-v2"
  description = "Allows FastAPI microservice to ingest documents."

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = [
        "s3:GetObject",
        "s3:ListBucket"
      ]
      Resource = [
        aws_s3_bucket.deal_ingestion_bucket.arn,
        "${aws_s3_bucket.deal_ingestion_bucket.arn}/*"
      ]
    }]
  })
}