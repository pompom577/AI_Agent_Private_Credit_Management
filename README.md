# AI Agent — Private Credit Deal Ingestion & Compliance Platform

A microservice platform for ingesting private-credit deal documents, classifying
and extracting financial data from them, routing high-risk AI-agent actions
through a human-in-the-loop (HITL) compliance review, and observing the whole
pipeline in production via AWS.

---

## Architecture

```
                     ┌──────────────────────────┐
   Analyst (browser) │   frontend (React/Vite)   │  :3000 (:80 in container)
                     └────────────┬─────────────┘
        /uploads /documents /api  │  /sse  /quarantine  /quarantine/stream
                                  ▼
                     ┌──────────────────────────┐
                     │  gateway-service          │  :8080
                     │  (Spring Boot 3, Java 21) │
                     │  • upload + S3 + JWT      │
                     │  • HITL quarantine filter │
                     │  • SSE notification bridge│
                     │  • dual-DB failover       │
                     └──────┬─────────────▲──────┘
            POST /classify  │             │ webhooks: classification-complete /
            (JWT)           ▼             │           extraction-failed
                     ┌──────────────────────────┐
                     │  classification-service   │  :8000
                     │  (FastAPI, Python 3.11)   │
                     │  • classify documents      │
                     │  • extract tabular metrics │
                     │  • hallucination guard      │
                     └────────────┬─────────────┘
                                  ▼
                     ┌──────────────────────────┐        ┌───────────────────┐
                     │  PostgreSQL (primary)      │◄──────►│ PostgreSQL (Neon)  │
                     │  RDS in prod / Neon in dev │  sync  │  hot backup         │
                     └──────────────────────────┘        └───────────────────┘
```

### Components

| Folder                    | Service                        | Stack                          | Port |
| -------------------------- | ------------------------------- | -------------------------------- | ---- |
| `frontend/`                | Analyst & compliance-officer UI | React + TypeScript + Vite       | 3000 |
| `gateway_service/`         | API Gateway + HITL + audit      | Spring Boot 3 (Java 21), Maven  | 8080 |
| `classification-service/`  | Extraction Engine               | FastAPI (Python 3.11)           | 8000 |
| `infrastructure/`          | DB schema, Docker, Terraform, observability | PostgreSQL, Docker, Terraform, CloudWatch, X-Ray | 5432 |

### Key flows

**1. Deal ingestion & extraction**
1. Analyst uploads a deal ZIP → gateway validates it, stores it to S3, mints an
   internal JWT, and hands off to FastAPI `/classify`.
2. FastAPI classifies each document, then extracts tabular financial metrics
   from classified PDFs (balance sheets, cap tables), guarding against LLM
   hallucination before persisting.
3. On success: rows are written to `extracted_metrics`, document status →
   `Extracted`.
4. On failure (flat image / corrupted table): status →
   `Extraction_Failed_Requires_OCR`, and an event is POSTed back to the
   gateway.
5. The gateway pushes that event over **Server-Sent Events** only to the
   analyst who owns the deal, who sees a non-blocking "OCR review required"
   banner deep-linking to manual review.

**2. Human-in-the-loop (HITL) compliance review**
1. Any AI agent request to a high-risk endpoint (`/api/credit/approve`,
   `/api/credit/transfer`, `/api/trade/execute`) is intercepted by the
   gateway's `HitlInterceptorFilter` *before* it reaches its destination.
2. The payload is parked in the `quarantine` table with status `Pending` and
   broadcast live to the Compliance Dashboard over SSE.
3. A compliance officer reviews the payload in the UI and **Approves** or
   **Rejects** it. Approval forwards the original payload to its intended
   destination; rejection discards it.
4. Every decision is written to an **append-only** `hitl_audit_ledger` table
   (updates/deletes blocked by DB trigger) in the same transaction as the
   status change — the payload is never released unless the audit write also
   succeeds.

**3. Dual-database resilience**
- The gateway can run against a single Postgres instance, or in dual-DB mode:
  AWS RDS as primary with a Neon Postgres instance as a continuously-synced
  hot backup. On primary failure, reads/writes fail over to Neon automatically;
  `BackupSyncService` mirrors every table back once the primary recovers.
- See `infrastructure/RUNBOOK.md` for the RDS cutover procedure and
  `infrastructure/scripts/migrate_neon_to_rds.sh` for the migration script.

**4. Observability**
- AWS X-Ray traces gateway requests through S3/SNS calls (service map).
- CloudWatch dashboard + alarms track CPU/memory and pipeline health.
- A Lambda (`infrastructure/lambda/compliance_alert_dispatcher/`) fans
  compliance alerts out to SNS.
- Prometheus alerting rules and a Grafana dashboard for audit-ledger health
  live under `infrastructure/observability/`.

---

## System Requirements

### Option A — Docker (recommended)

- **Docker** 24+ and **Docker Compose v2**
- ~4 GB free RAM, ~3 GB disk for images
- Outbound network access to your PostgreSQL host, AWS S3, and the Google
  Gemini API

### Option B — Local development (run each service directly)

- **Python** 3.11+
- **Java (JDK)** 21 and **Maven** 3.9+
- **Node.js** 20+ (Node 22 recommended) and npm
- **PostgreSQL** 14+ (local or remote, e.g. Neon)

### External services

- **PostgreSQL** database (SQL migrations are shipped for it under
  `infrastructure/init-scripts/`)
- **AWS S3** bucket for uploaded archives
- **Google Gemini API** key (free tier works) for document classification
- Optional for full parity with prod: **AWS RDS**, **SNS**, **Lambda**,
  **CloudWatch**, **X-Ray** — provisioned via `infrastructure/terraform/`

---

## Dependencies

- **classification-service** — FastAPI, Uvicorn, SQLAlchemy, psycopg2,
  Pydantic, httpx, boto3, LlamaIndex (Gemini), PyMuPDF, python-docx, and
  pdfplumber (table extraction). See `classification-service/requirements.txt`.
- **gateway_service** — Spring Boot 3.3, Spring Web + WebSocket/SSE, AWS SDK
  (S3, X-Ray), JJWT, PostgreSQL JDBC, Redis cache client. See
  `gateway_service/pom.xml`.
- **frontend** — React, TypeScript, Vite, Tailwind, React Router,
  `react-dropzone`, `react-pdf`/`pdfjs-dist`. See `frontend/package.json`.

---

## Getting Started (Docker)

1. Copy the environment template and fill in real values:

   ```bash
   cp infrastructure/docker/.env.example infrastructure/docker/.env
   # edit infrastructure/docker/.env — DATABASE_URL, AWS keys, GOOGLE_API_KEY, etc.
   ```

   **Never commit `infrastructure/docker/.env`** — it holds live credentials
   and is gitignored deliberately.

2. Start the stack:

   ```bash
   cd infrastructure/docker
   docker-compose up -d --build
   ```

3. Open the app:
   - Frontend: http://localhost:3000
   - Gateway API: http://localhost:8080
   - Classification service: http://localhost:8000
   - Compliance dashboard: http://localhost:3000/compliance-review

4. Tear down:

   ```bash
   docker-compose down
   ```

For cloud deployment (RDS, EC2, Lambda, CloudWatch, X-Ray), follow
[`infrastructure/RUNBOOK.md`](infrastructure/RUNBOOK.md) end to end.

---

## Testing

```bash
# gateway_service (Java/Maven)
cd gateway_service && ./mvnw test

# classification-service (Python)
cd classification-service && pytest

# frontend (Vitest)
cd frontend && npm test
```

---

## Repository layout

```
.
├── frontend/                 React + TS analyst/compliance UI
├── gateway_service/          Spring Boot API gateway, HITL filter, audit ledger
├── classification-service/   FastAPI classification & extraction engine
└── infrastructure/
    ├── docker/                docker-compose.yml + .env.example
    ├── init-scripts/          Postgres schema migrations
    ├── terraform/              RDS, EC2, Lambda, CloudWatch, IAM, networking
    ├── lambda/                 compliance_alert_dispatcher
    ├── observability/          Grafana dashboards, Prometheus alert rules
    ├── scripts/                 Neon → RDS migration & repair scripts
    └── RUNBOOK.md               Step-by-step cloud deployment guide
```
