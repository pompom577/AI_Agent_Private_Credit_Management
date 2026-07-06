resource "aws_s3_bucket" "deal_ingestion_bucket" {
  # 1. New unique bucket name
  bucket        = "private-credit-ingestion-dev-storage"
  
  # 2. Changed to true for easy cleanup at the end of your semester
  force_destroy = true 
}

# Enforce secure encryption at rest
resource "aws_s3_bucket_server_side_encryption_configuration" "s3_encryption" {
  bucket = aws_s3_bucket.deal_ingestion_bucket.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# Automate cleaning up stalled chunk uploads and archiving old files
resource "aws_s3_bucket_lifecycle_configuration" "bucket_lifecycle" {
  bucket = aws_s3_bucket.deal_ingestion_bucket.id

  # Rule 1: Clean up broken/stalled file uploads after 1 day
  rule {
    id     = "abort-incomplete-multipart-uploads"
    status = "Enabled"

    filter {} 

    abort_incomplete_multipart_upload {
      days_after_initiation = 1
    }
  }

  # Rule 2: Move old files to cheap Glacier storage after 30 days
  rule {
    id     = "archive-old-deals"
    status = "Enabled"

    filter {} 

    transition {
      days          = 30
      storage_class = "GLACIER"
    }
  }
}