package com.platform.gateway.services;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Mints short-lived HS256-signed Bearer JWTs for Gateway -> FastAPI handoff (1.1c).
 *
 * Claims contract (coordinated with Person 3 — Python/FastAPI):
 *   iss = "gateway-service"
 *   aud = "classification-service"
 *   sub = uploaded_by_user_id
 *   deal_id (custom)
 *   iat / exp (now + ttlSeconds)
 */
@Service
public class JwtService {

    public static final String ISSUER = "gateway-service";
    public static final String AUDIENCE = "classification-service";

    private final SecretKey key;
    private final long ttlSeconds;

    public JwtService(
            @Value("${gateway.jwt.secret}") String secret,
            @Value("${gateway.jwt.ttl-seconds}") long ttlSeconds) {
        // HS256 requires a >= 32-byte key. Keys.hmacShaKeyFor enforces this.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlSeconds = ttlSeconds;
    }

    public String mintInternalToken(String uploadedByUserId, String dealId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .subject(uploadedByUserId == null ? "anonymous" : uploadedByUserId)
                .claim("deal_id", dealId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key)
                .compact();
    }

    /** Exposed for tests that need to verify a freshly minted token. */
    SecretKey signingKey() {
        return key;
    }
}
