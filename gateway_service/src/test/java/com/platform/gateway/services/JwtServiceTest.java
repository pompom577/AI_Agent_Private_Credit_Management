package com.platform.gateway.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JwtService}. Verifies that minted tokens can be parsed back
 * with the same key and that the claim contract agreed with Person 3 (Python/FastAPI)
 * is honoured: iss / aud / sub / deal_id / exp.
 */
class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-32-characters-long-aaaa";

    @Test
    void mintedTokenRoundTrips_andContainsExpectedClaims() {
        JwtService svc = new JwtService(SECRET, 60);

        String token = svc.mintInternalToken("user-42", "deal-abc");

        Jws<Claims> parsed = Jwts.parser()
                .verifyWith(svc.signingKey())
                .build()
                .parseSignedClaims(token);

        Claims c = parsed.getPayload();
        assertThat(c.getIssuer()).isEqualTo(JwtService.ISSUER);
        assertThat(c.getAudience()).contains(JwtService.AUDIENCE);
        assertThat(c.getSubject()).isEqualTo("user-42");
        assertThat(c.get("deal_id", String.class)).isEqualTo("deal-abc");
        assertThat(c.getExpiration()).isAfter(c.getIssuedAt());
    }

    @Test
    void anonymousSubject_whenUserIdMissing() {
        JwtService svc = new JwtService(SECRET, 60);

        String token = svc.mintInternalToken(null, "deal-xyz");

        Claims c = Jwts.parser().verifyWith(svc.signingKey()).build()
                .parseSignedClaims(token).getPayload();
        assertThat(c.getSubject()).isEqualTo("anonymous");
    }
}
