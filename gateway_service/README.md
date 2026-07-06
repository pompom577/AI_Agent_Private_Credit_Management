# gateway-service

Spring Boot 3 / Java 21 / Maven gateway for the Deal Ingestion Platform.
Owns Sub-Stories **1.1a**, **1.1b**, **1.1c** of Epic 1 (Triage & Extraction).

> Status: **all four iterations complete** — secure upload (1.1a), unhappy-path
> mapping (1.1b), and FastAPI classification handoff (1.1c) are all wired and
> covered by tests TC-GW-01..06.

## Prerequisites

- JDK 21 (e.g., `sdk install java 21-tem`)
- Maven 3.9+ (or run `./mvnw` once wrapper is added)
- AWS credentials (env vars or instance profile) with `s3:PutObject` on
  `S3_BUCKET` — **or** a local S3-compatible server reachable via
  `AWS_ENDPOINT_URL` (see _Local development_ below).

## Environment variables

| Variable              | Default                 | Purpose                                 |
| --------------------- | ----------------------- | --------------------------------------- |
| `S3_BUCKET`           | `deal-ingestion-local`  | Target bucket for `PUT object` (1.1a)   |
| `AWS_REGION`          | `ap-southeast-1`        | AWS region for the S3 client            |
| `AWS_ENDPOINT_URL`    | _(unset → real AWS)_    | Redirect SDK at MinIO / LocalStack      |
| `CLASSIFY_BASE_URL`   | `http://localhost:8000` | FastAPI `/classify` base URL (1.1c)     |
| `INTERNAL_JWT_SECRET` | `change-me-...`         | HS256 secret shared with FastAPI (1.1c) |

## Build & run

```bash
mvn clean verify              # compile + run smoke test
mvn spring-boot:run           # start on :8080
curl http://localhost:8080/actuator/health
```

`POST /uploads` performs the full pipeline: validate magic bytes →
inspect entries → reject encrypted archives → S3 `PutObject` → mint JWT →
`POST /classify` against FastAPI → return `201 {bucket_url, deal_id}`.

## Local development (no AWS account)

Two containers replace real AWS S3 and Person 3's FastAPI:

```bash
# 1. MinIO  (S3-compatible, free, replaces LocalStack which now gates S3 behind a license)
docker run --rm -d -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin -e MINIO_ROOT_PASSWORD=minioadmin \
  --name minio minio/minio server /data --console-address ":9001"

aws --endpoint-url=http://localhost:9000 \
    s3 mb s3://deal-ingestion-local --region us-east-1

# 2. Fake FastAPI /classify (echoes JWT, returns 202 by default)
python3 scripts/fake_classify.py

# 3. Boot gateway against both
export AWS_ACCESS_KEY_ID=minioadmin AWS_SECRET_ACCESS_KEY=minioadmin
export AWS_REGION=us-east-1 S3_BUCKET=deal-ingestion-local
export AWS_ENDPOINT_URL=http://localhost:9000
export CLASSIFY_BASE_URL=http://localhost:8000
mvn -f gateway_service/pom.xml spring-boot:run
```

Full TC-GW-01..06 walkthrough with `curl` snippets: see
[`MANUAL_TESTS.md`](MANUAL_TESTS.md).

## Roadmap (Person 2 — Backend)

| Iter | Sub-Story | Deliverable                                                         | Test cases       | Status |
| ---- | --------- | ------------------------------------------------------------------- | ---------------- | ------ |
| 1    | —         | Skeleton: boots, health UP, all stubs in place                      | smoke test       | ✅     |
| 2    | 1.1a      | Magic-byte check + S3 `PUT object` + 201 `{bucket_url, deal_id}`    | TC-GW-01, 02, 05 | ✅     |
| 3    | 1.1b      | Deep entry inspection → 415; encrypted-archive → 422                | TC-GW-03, 04     | ✅     |
| 4    | 1.1c      | Mint short-lived JWT, WebClient `POST /classify`, propagate 202/4xx | TC-GW-06         | ✅     |

## TODO markers

Search the source for these tags to pick up where the skeleton left off:

- `TODO(1.1a)` — secure upload happy path
- `TODO(1.1b)` — upload exception mapping (415 / 422)
- `TODO(1.1c)` — classification handoff (JWT + FastAPI call)

## Coordination

- **Person 1 (Frontend)** — confirm CORS origin to whitelist in `SecurityConfig`.
- **Person 3 (Python/FastAPI)** — finalize JWT claims (`iss`, `aud`, `sub`,
  `exp`) and the JSON field casing (`bucket_url` vs `bucketUrl`); the current
  contract assumes snake_case per `1.1 Story Task.docx`.
- **Person 4 (DevOps)** — provision the S3 bucket + IAM (`s3:PutObject` for
  this service, `s3:GetObject` for classification-service) and inject
  `INTERNAL_JWT_SECRET` securely into both backend services.
