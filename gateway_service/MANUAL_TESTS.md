# Manual TC-GW-01..06 playbook

End-to-end walkthrough of the six test cases from `1.1 Story Task.docx`,
runnable on a single laptop with Docker and a Python interpreter.
Automated equivalents live in `src/test/java/...` and pass under `mvn verify`.

---

## 0. One-time setup

### 0.1 Start MinIO (S3-compatible, free, replaces LocalStack)

```bash
docker run --rm -d -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  --name minio \
  minio/minio server /data --console-address ":9001"
```

Web console: <http://localhost:9001> (login `minioadmin` / `minioadmin`).

Create the bucket:

```bash
# Option A — awscli (brew install awscli)
aws --endpoint-url=http://localhost:9000 \
    s3 mb s3://deal-ingestion-local --region us-east-1

# Option B — click "Create Bucket" in the console UI
```

### 0.2 Start the FastAPI stub for the /classify handoff

```bash
python3 gateway_service/scripts/fake_classify.py
# leave running; will print each Bearer JWT it receives
```

### 0.3 Boot the gateway pointing at both

```bash
export AWS_ACCESS_KEY_ID=minioadmin
export AWS_SECRET_ACCESS_KEY=minioadmin
export AWS_REGION=us-east-1
export S3_BUCKET=deal-ingestion-local
export AWS_ENDPOINT_URL=http://localhost:9000   # MinIO endpoint
export CLASSIFY_BASE_URL=http://localhost:8000  # fake_classify.py

mvn -f gateway_service/pom.xml spring-boot:run
```

Wait for `Started GatewayApplication` and verify health:

```bash
curl http://localhost:8080/actuator/health   # {"status":"UP"}
```

---

## TC-GW-01 — Stream acceptance (happy path → 201)

```bash
echo "hello deal" > /tmp/note.txt
zip /tmp/deal.zip /tmp/note.txt

curl -i -X POST \
  -F "file=@/tmp/deal.zip" \
  -H "X-User-Id: user-123" \
  http://localhost:8080/uploads
```

**Expected**

```
HTTP/1.1 201
{"bucket_url":"s3://deal-ingestion-local/<uuid>/deal.zip","deal_id":"<uuid>"}
```

The `fake_classify.py` terminal should also print:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Body: {"bucket_url":"...","deal_id":"...","uploaded_by_user_id":"user-123"}
```

---

## TC-GW-02 — Magic-byte / MIME mismatch (renamed `.exe` → 415)

No S3 / FastAPI required.

```bash
printf 'MZ\x90\x00not-a-zip' > /tmp/fake.zip
curl -i -X POST -F "file=@/tmp/fake.zip" http://localhost:8080/uploads
```

**Expected** — `HTTP/1.1 415` and body `{"reason":"invalid zip format"}`.

---

## TC-GW-03 — Archive contains executable entry (`.exe` inside → 415)

```bash
mkdir -p /tmp/bad
printf 'MZ\x00\x00' > /tmp/bad/malware.exe
echo "ok" > /tmp/bad/notes.txt
( cd /tmp/bad && zip /tmp/bad.zip malware.exe notes.txt )

curl -i -X POST -F "file=@/tmp/bad.zip" http://localhost:8080/uploads
```

**Expected** — `HTTP/1.1 415` and `{"reason":"unsupported entries"}`.

Other denylisted extensions to try: `.bat`, `.sh`, `.dll`, `.jar`, `.cmd`,
`.com`, `.msi`, `.scr`, `.ps1`, `.vbs`.

---

## TC-GW-04 — Encrypted / password-protected archive → 422

```bash
echo "confidential" > /tmp/secret.txt
zip -P hunter2 /tmp/locked.zip /tmp/secret.txt

curl -i -X POST -F "file=@/tmp/locked.zip" http://localhost:8080/uploads
```

**Expected** — `HTTP/1.1 422` and `{"reason":"encrypted archive"}`.

---

## TC-GW-05 — Persistence verification (object lands in S3)

After **TC-GW-01** succeeded, grab the `deal_id` from its response, then:

```bash
aws --endpoint-url=http://localhost:9000 \
    s3 ls s3://deal-ingestion-local/ --recursive
# expect: <deal_id>/deal.zip   <size>   <date>

aws --endpoint-url=http://localhost:9000 \
    s3 cp s3://deal-ingestion-local/<deal_id>/deal.zip /tmp/roundtrip.zip
unzip -l /tmp/roundtrip.zip   # entries must match the original
```

Visual alternative — open <http://localhost:9001>, browse the bucket, click
the object and use *Preview* / *Download*.

---

## TC-GW-06 — Downstream classification handoff

### 6a. JWT contract on success

Re-run TC-GW-01. In the `fake_classify.py` terminal, copy the JWT after
`Bearer` and paste it into <https://jwt.io>. Verify:

| Claim       | Expected                                    |
| ----------- | ------------------------------------------- |
| `iss`       | `gateway-service`                           |
| `aud`       | `classification-service`                    |
| `sub`       | `user-123` (matches `X-User-Id`)            |
| `deal_id`   | matches the `deal_id` in the 201 response   |
| `exp - iat` | `60` seconds (`gateway.jwt.ttl-seconds`)    |

### 6b. Propagate 401 from FastAPI

Stop `fake_classify.py`, restart with:

```bash
python3 gateway_service/scripts/fake_classify.py --status 401
```

Re-run TC-GW-01. Gateway must return:

```
HTTP/1.1 401
{"reason":"classification handoff failed: 401"}
```

### 6c. Propagate 422 from FastAPI

```bash
python3 gateway_service/scripts/fake_classify.py --status 422
```

Re-run TC-GW-01 → expect `HTTP/1.1 422` and
`{"reason":"classification handoff failed: 422"}`.

### 6d. Propagate 502 when FastAPI is unreachable

Stop `fake_classify.py` entirely. Re-run TC-GW-01 → expect
`HTTP/1.1 502` and `{"reason":"classification handoff failed: 502"}`.

---

## Tear-down

```bash
docker stop minio
# Ctrl-C on the spring-boot:run and fake_classify.py terminals
```
