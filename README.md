# AI Agent — Private Credit Deal Ingestion Platform

A microservice platform for ingesting private-credit deal documents, classifying
them, extracting tabular financial metrics, and notifying analysts in real time
when a document needs manual OCR review.

This repository implements **Sub-Story 1.3 — Tabular Data Extraction & Fallback**,
which bridges AI document parsing, batch backend processing, and real-time
frontend notifications.

---

## Architecture

```
                     ┌──────────────────────────┐
   Analyst (browser) │   frontend (React/Vite)   │  :3000
                     └────────────┬─────────────┘
                       /uploads   │   /sse (SSE stream)
                                  ▼
                     ┌──────────────────────────┐
                     │  gateway-service          │  :8080
                     │  (Spring Boot, Java 21)   │
                     │  • upload + S3 + JWT      │
                     │  • SSE notification bridge │
                     └──────┬─────────────▲──────┘
            POST /classify  │             │ POST /webhooks/extraction-failed
            (JWT)           ▼             │ POST /webhooks/classification-complete
                     ┌──────────────────────────┐
                     │  classification-service   │  :8000
                     │  (FastAPI, Python 3.11)   │
                     │  • classify documents     │
                     │  • extract tables (1.3)   │
                     │  • persist metrics        │
                     └────────────┬─────────────┘
                                  ▼
                     ┌──────────────────────────┐
                     │  PostgreSQL                │  :5432
                     │  deals / document_records │
                     │  / extracted_metrics      │
                     └──────────────────────────┘
```

### Components

| Folder                    | Service           | Stack                         | Port |
| ------------------------- | ----------------- | ----------------------------- | ---- |
| `frontend/`               | Analyst UI        | React + TypeScript + Vite     | 3000 |
| `gateway_service/`        | API Gateway       | Spring Boot (Java 21), Maven  | 8080 |
| `classification-service/` | Extraction Engine | FastAPI (Python 3.11)         | 8000 |
| `infrastructure/`         | DB schema + IaC   | PostgreSQL, Docker, Terraform | 5432 |

### Story 1.3 end-to-end flow

1. Analyst uploads a deal ZIP → gateway validates, stores to S3, mints a JWT, and
   hands off to FastAPI `/classify`.
2. FastAPI classifies each document, then runs the **tabular extraction batch**
   over the classified financial PDFs (Balance Sheet, Cap Table).
3. On success: rows are written to `extracted_metrics`, document status → `Extracted`.
4. On failure (flat image / corrupted table): status → `Extraction_Failed_Requires_OCR`,
   and an event is POSTed to the gateway (`/webhooks/extraction-failed`).
5. The gateway routes that event over **Server-Sent Events** only to the analyst
   who owns the deal.
6. The React app renders a non-blocking "OCR review required for …" banner that
   deep-links to the manual review screen.

---

## System Requirements

### Option A — Docker (recommended)

- **Docker** 24+ and **Docker Compose v2**
- ~4 GB free RAM, ~3 GB disk for images
- Outbound network access to your PostgreSQL host, AWS S3, and the Google Gemini API

### Option B — Local development (run each service directly)

- **Python** 3.11+
- **Java (JDK)** 21 and **Maven** 3.9+
- **Node.js** 20+ (Node 22 recommended) and npm
- **PostgreSQL** 14+ (local or remote, e.g. Neon)

### External services

- **PostgreSQL** database (the platform ships SQL migrations for it)
- **AWS S3** bucket for uploaded archives
- **Google Gemini API** key (free tier works) for document classification

---

## Dependencies

- **classification-service** — FastAPI, Uvicorn, SQLAlchemy, psycopg2, Pydantic,
  httpx, boto3, LlamaIndex (Gemini), PyMuPDF, python-docx, and **pdfplumber**
  (Story 1.3 table extraction). See `classification-service/requirements.txt`.
- **gateway_service** — Spring Boot 3.3, Spring Web + WebSocket/SSE, AWS SDK (S3),
  JJWT, PostgreSQL JDBC. See `gateway_service/pom.xml`.
- **frontend** — React, TypeScript, Vite, Tailwind, Zustand, React Router. See
  `frontend/package.json`.

---

## Configuration (environment variables)

Copy the example file and fill in your own values. **Never commit real secrets.**

```bash
cp infrastructure/docker/.env.example infrastructure/docker/.env
```

| Variable                       | Used by                 | Description                                |
| ------------------------------ | ----------------------- | ------------------------------------------ |
| `DATABASE_URL`                 | classification-service  | `postgresql://user:pass@host:5432/db`      |
| `SPRING_DATASOURCE_URL`        | gateway-service         | JDBC URL for the same PostgreSQL database  |
| `SPRING_DATASOURCE_USERNAME`   | gateway-service         | DB username                                |
| `SPRING_DATASOURCE_PASSWORD`   | gateway-service         | DB password                                |
| `AWS_ACCESS_KEY_ID`            | gateway, classification | AWS credentials for the S3 bucket          |
| `AWS_SECRET_ACCESS_KEY`        | gateway, classification | AWS credentials for the S3 bucket          |
| `AWS_REGION`                   | gateway, classification | e.g. `us-east-1`                           |
| `S3_BUCKET`                    | gateway-service         | Target S3 bucket name                      |
| `GOOGLE_API_KEY`               | classification-service  | Google Gemini API key (classification)     |
| `INTERNAL_JWT_SECRET`          | gateway, classification | Shared HS256 secret for internal JWTs      |
| `GATEWAY_CALLBACK_URL`         | classification-service  | `…/webhooks/classification-complete`       |
| `GATEWAY_EXTRACTION_EVENT_URL` | classification-service  | `…/webhooks/extraction-failed` (Story 1.3) |
| `BACKUP_DB_URL`                | gateway-service         | Task #1 dual-DB: Neon JDBC URL, hot backup for RDS. Optional — leave unset for single-DB mode. |
| `BACKUP_DB_USER` / `BACKUP_DB_PASSWORD` | gateway-service | Credentials for the Neon backup above |
| `BACKUP_DATABASE_URL`          | classification-service  | Same Neon backup DB, SQLAlchemy form   |

### Dual-database failover (Task #1)

When `BACKUP_DB_URL`/`BACKUP_DATABASE_URL` are set, both services route
reads/writes to the primary database (RDS in prod, Neon in dev) while it's
healthy, and fail over to the Neon backup automatically when it isn't:

- **gateway-service** — `DualDataSourceConfig` + `FailoverRoutingDataSource`
  route JDBC traffic, and `BackupSyncService` continuously mirrors every
  table into Neon so both databases stay in sync.
- **classification-service** — `database.py` probes the primary at most once
  every 5s and hands out a Neon-backed session when it's down.

Leave the backup variables empty to run against a single database.

---

## Quick Start — Docker Compose

```bash
# 1. Configure secrets
cp infrastructure/docker/.env.example infrastructure/docker/.env
#    then edit infrastructure/docker/.env with your DB / AWS / Gemini credentials

# 2. Build and start all services
cd infrastructure/docker
docker compose up --build

# 3. Open the app
#    Frontend : http://localhost:3000
#    Gateway  : http://localhost:8080
#    FastAPI  : http://localhost:8000/docs
```

The gateway runs the SQL migrations in `infrastructure/init-scripts/` against the
configured database on startup (`deals`, `document_records`, `extracted_metrics`).

To stop:

```bash
docker compose down
```

---

## Local Development (without Docker)

Run each service in its own terminal. Set the same environment variables as above
(e.g. via a per-service `.env`).

### 1. PostgreSQL

Point all services at a PostgreSQL instance, then apply the migrations in order:

```bash
psql "$DATABASE_URL" -f infrastructure/init-scripts/01-init-db.sql
psql "$DATABASE_URL" -f infrastructure/init-scripts/02-document-records-schema.sql
psql "$DATABASE_URL" -f infrastructure/init-scripts/03-extracted-metrics-schema.sql
```

### 2. classification-service (FastAPI, :8000)

```bash
cd classification-service
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

> Note: `pdfplumber` (table extraction) is pure-Python and needs no system
> libraries. The bundled `.venv/` in this repo is not used — create a fresh one.

### 3. gateway-service (Spring Boot, :8080)

```bash
cd gateway_service
./mvnw spring-boot:run        # or: mvn spring-boot:run
```

### 4. frontend (Vite dev server, :5173)

```bash
cd frontend
npm install
npm run dev
```

In dev, Vite proxies `/uploads` and `/sse` to the gateway on `:8080`
(see `frontend/vite.config.ts`), so the browser stays on a single origin.

---

## Testing

```bash
# Gateway (JUnit)
cd gateway_service && mvn test

# Frontend type-check / build
cd frontend && npm run build
```

The test matrix for Story 1.3 (extraction precision, batch isolation, schema
constraints, real-time notifications) is documented in `1.3_Story_Task.md`.

---

## Project Layout

```
.
├── classification-service/   # FastAPI: classify + extract tables + persist metrics
│   └── app/
│       ├── services/         # table_extractor, format_validator, text_extractor …
│       ├── workers/          # async_pipeline (batch loop + 1.3 extraction)
│       ├── models/           # schemas, extracted_metric (SQLAlchemy)
│       └── exceptions/       # ExtractionFailureException
├── gateway_service/          # Spring Boot: uploads, JWT, SSE notification bridge
│   └── src/main/java/com/platform/gateway/
│       ├── handlers/         # InternalEventHandler (/webhooks/extraction-failed, /sse)
│       ├── messaging/        # NotificationBridge (per-user SSE routing)
│       └── controllers/      # UploadController, WebhookController
├── frontend/                 # React UI: SSE client + OCR-review banner
│   └── src/
│       ├── hooks/            # useExtractionSocket (SSE)
│       ├── components/notifications/  # ExtractionToast, ManualReviewBanner
│       └── store/            # notificationSlice (Zustand)
└── infrastructure/
    ├── init-scripts/         # PostgreSQL migrations (01, 02, 03)
    ├── docker/               # docker-compose.yml + .env.example
    └── terraform/            # AWS IaC (S3, IAM, Secrets Manager)
```

---

## Security Notes

- **Do not commit real credentials.** All secrets belong in untracked `.env`
  files or a secrets manager. `.gitignore` excludes `.env`, Terraform state, and
  build artifacts.
- Internal service-to-service calls are authenticated with short-lived HS256 JWTs
  signed with `INTERNAL_JWT_SECRET` (must match across gateway and FastAPI).
- The SSE notification bridge routes extraction-failure events **only** to the
  analyst who owns the deal — failures for one analyst are never broadcast to others.
