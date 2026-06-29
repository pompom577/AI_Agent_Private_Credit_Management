import os

from dotenv import load_dotenv

# Load .env before reading any env vars. config.py is imported transitively
# before main.py calls load_dotenv(), so we must call it here to guarantee
# the correct secret is picked up regardless of import order.
load_dotenv()

INTERNAL_JWT_SECRET = os.getenv("INTERNAL_JWT_SECRET", "mysecret")
JWT_ALGORITHM = os.getenv("JWT_ALGORITHM", "HS256")

# Must match the constants in gateway's JwtService.java (ISSUER, AUDIENCE).
# Hardcoded as part of the inter-service contract; overridable via env for tests.
JWT_ISSUER = os.getenv("JWT_ISSUER", "gateway-service")
JWT_AUDIENCE = os.getenv("JWT_AUDIENCE", "classification-service")

# URL the async pipeline uses to fire the classification-complete callback (Story 1.2)
GATEWAY_CALLBACK_URL = os.getenv(
    "GATEWAY_CALLBACK_URL",
    "http://gateway-service:8080/webhooks/classification-complete",
)

# URL the async pipeline uses to fire the extraction-failure event (Story 1.3).
# Must match the gateway's InternalEventHandler (@PostMapping /webhooks/extraction-failed).
GATEWAY_EXTRACTION_EVENT_URL = os.getenv(
    "GATEWAY_EXTRACTION_EVENT_URL",
    "http://gateway-service:8080/webhooks/extraction-failed",
)