# Deployment Runbook — Task #1 & Task #2 (CT071-3-3-DDAC)

Everything in `infrastructure/terraform/` is written and ready. I could not run
`terraform apply` myself — the AWS credentials available in this environment
(`infrastructure/docker/.env`) are locked to S3-only access (confirmed via
read-only probes against SNS/EC2/RDS/Lambda/API Gateway/CloudWatch/IAM, all
denied). This runbook is what you run yourself, from a machine with broader
AWS credentials (an admin IAM user, or one with the policies listed below).

## What each new file does

| File | Fulfills |
|---|---|
| `terraform/rds.tf` | Task #1 — AWS RDS Postgres (replaces Neon) |
| `terraform/ec2_deployment.tf` | Task #1 — EC2 compute deployment |
| `scripts/migrate_neon_to_rds.sh` | Task #1 — "migrate your data" screenshot step |
| `terraform/serverless.tf` + `lambda/compliance_alert_dispatcher/` | Task #2 Part 2 — API Gateway + Lambda + SNS |
| `terraform/cloudwatch.tf` | Task #2 Part 3 — CloudWatch dashboard + alarms |
| `gateway_service/.../config/XRayConfig.java` + X-Ray deps in `pom.xml` | Task #2 Part 3 — X-Ray service map |
| Architecture diagram artifact (link in chat) | Task #2 Part 1 — old vs new diagrams |

## 0. Prerequisites

1. **Broader IAM credentials.** The IAM user you deploy with needs (at minimum):
   `AmazonEC2FullAccess`, `AmazonRDSFullAccess`, `AWSLambda_FullAccess`,
   `AmazonAPIGatewayAdministrator`, `CloudWatchFullAccess`, `IAMFullAccess`
   (for the roles this creates), and the existing S3/SNS access.
2. **An EC2 key pair** in `us-east-1`: EC2 Console → Key Pairs → Create key
   pair → download the `.pem` → `chmod 400 your-key.pem`.
3. **Terraform ≥ 1.5** and the AWS CLI configured with the credentials above
   (`aws configure`, or export `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY`).

## 1. Apply the infrastructure

```bash
cd infrastructure/terraform
terraform init      # pulls in the new hashicorp/archive provider

export TF_VAR_db_password='choose-a-real-password-here'
export TF_VAR_ec2_key_pair_name='your-key-pair-name'
export TF_VAR_ssh_allowed_cidr='<your-ip>/32'   # find yours: curl -s ifconfig.me

terraform plan    # review before applying anything real/billed
terraform apply
```

This creates (all billed resources — review the plan before confirming):
RDS `db.t3.micro`, EC2 `t3.micro`, an IAM role + instance profile, a Lambda
function, an HTTP API Gateway, 4 CloudWatch alarms, and a CloudWatch dashboard.

**Screenshot checkpoint (Task #1, Part 1 — "creating your cloud database
service"):** the RDS console showing the new `private-credit-gateway-db`
instance in "Available" status.

## 2. Migrate the data from Neon to RDS

```bash
terraform output rds_endpoint     # grab the new RDS address

export SOURCE_DB_URL="postgresql://neondb_owner:<neon-password>@ep-ancient-flower-apre4f1g-pooler.c-7.us-east-1.aws.neon.tech/neondb?sslmode=require"
export TARGET_DB_URL="postgresql://platform_admin:<the-password-you-set-above>@<rds-endpoint-from-above>:5432/private_credit_db"

../scripts/migrate_neon_to_rds.sh
```

The script prints a row-count comparison table per table — that table itself
is your **screenshot for "migrating your data to cloud database service"**.

## 3. Deploy the application onto the EC2 instance

This repo has no git remote configured, so `terraform`'s `user_data` only
bootstraps Docker/Compose — getting the app code onto the box is a manual
step (pick one):

```bash
# Option A — rsync (simplest, no GitHub needed)
terraform output ec2_public_ip
rsync -avz -e "ssh -i your-key.pem" \
  --exclude 'node_modules' --exclude 'target' --exclude '.git' \
  "../.." ec2-user@<ec2-public-ip>:~/app/

# Option B — if you push this repo to your own GitHub first
ssh -i your-key.pem ec2-user@<ec2-public-ip>
git clone <your-repo-url> ~/app
```

Then, on the instance, point it at RDS and start the stack:

```bash
ssh -i your-key.pem ec2-user@<ec2-public-ip>
cd ~/app/infrastructure/docker
cp .env .env   # copy your real .env over too (scp it, don't retype secrets)
echo "RDS_ENDPOINT=<rds-endpoint>"   >> .env
echo "DB_USERNAME=platform_admin"    >> .env
echo "DB_PASSWORD=<the-password>"    >> .env
docker-compose up -d --build
```

**Screenshot checkpoint (Task #1, Part 2 — "deploying your system to Amazon
Compute Services"):** `docker ps` output on the EC2 instance, plus the app
loading in a browser at `http://<ec2-public-ip>:3000` (or the ALB's DNS name
once its health check passes — `terraform output` doesn't print it by name,
grab it from EC2 Console → Load Balancers → `hitl-ingress-alb` → DNS name).

**Screenshot checkpoint (Task #1, submission requirement):** the working app
URL + these login/access details, captured for the PPT slide.

## 4. Verify the serverless path (Task #2 Part 2)

```bash
terraform output compliance_alerts_api_url

curl -X POST "<that-url>" \
  -H "Content-Type: application/json" \
  -d '{"agent_id":"agent-007","quarantine_id":"11111111-1111-1111-1111-111111111111","message":"manual smoke test"}'
# expect: {"status": "published", "quarantine_id": "..."} with HTTP 202
```

Then check the SNS-subscribed Slack/Teams channel (or CloudWatch Logs for the
`compliance-alert-dispatcher` Lambda if no webhook is wired yet) for the
message. **Screenshot checkpoint:** the curl response + the Lambda's
CloudWatch Logs entry + (if configured) the Slack message.

## 5. Verify monitoring (Task #2 Part 3)

```bash
terraform output cloudwatch_dashboard_url
```

Open that URL — this is your **CPU/Memory Utilization screenshot**.

For the **X-Ray service map**: hit the deployed app a few times (upload a
deal, browse the compliance dashboard) to generate traced requests, then open
AWS Console → X-Ray → Traces → Service map. You should see `gateway-service`
as the central node with `S3` and `SNS` as connected downstream nodes (from
the `TracingInterceptor` wired into `S3Config`/`SnsConfig`).

## 6. Tearing down (avoid ongoing charges)

```bash
cd infrastructure/terraform
terraform destroy
```

RDS `skip_final_snapshot = true` and `deletion_protection = false` are set
deliberately for coursework teardown — flip both before you'd ever do this
against a database that matters.
