# Validates internal JWT issued by Spring Boot gateway 
# to ensure only trusted backend services can call /classify

from fastapi import Header, HTTPException, status
from jose import JWTError, jwt

from app.core.config import (
    INTERNAL_JWT_SECRET,
    JWT_ALGORITHM,
    JWT_AUDIENCE,
    JWT_ISSUER,
)


def verify_bearer_token(authorization: str | None = Header(default=None)):
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing Bearer token",
        )

    token = authorization.replace("Bearer ", "", 1)

    try:
        # Validate signature, expiry, audience, and issuer. python-jose's
        # `audience` and `issuer` kwargs trigger JWTClaimsError (subclass of
        # JWTError) when the corresponding claim is absent or mismatched.
        payload = jwt.decode(
            token,
            INTERNAL_JWT_SECRET,
            algorithms=[JWT_ALGORITHM],
            audience=JWT_AUDIENCE,
            issuer=JWT_ISSUER,
        )
        return payload
    except JWTError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired token",
        )