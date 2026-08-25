package com.obs.backend.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.obs.backend.security.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcd";

    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);

    private JwtService jwtService(Duration accessTtl, Duration refreshTtl) {
        return jwtService(accessTtl, refreshTtl, CHALLENGE_TTL);
    }

    private JwtService jwtService(Duration accessTtl, Duration refreshTtl, Duration challengeTtl) {
        return new JwtService(new JwtProperties(SECRET, accessTtl, refreshTtl, challengeTtl));
    }

    private JwtService jwtService() {
        return jwtService(Duration.ofMinutes(15), Duration.ofDays(7));
    }

    @Test
    void accessTokenResolvesToItsSubjectAndRoles() {
        JwtService service = jwtService();

        String token = service.generateAccessToken("customer-42", Set.of(Role.CUSTOMER));

        assertThat(service.resolvePrincipal(token))
                .contains(new JwtPrincipal("customer-42", Set.of(Role.CUSTOMER)));
    }

    @Test
    void accessTokenCarriesMultipleRoles() {
        JwtService service = jwtService();

        String token = service.generateAccessToken("staff-1", Set.of(Role.ADMIN, Role.CUSTOMER));

        assertThat(service.resolvePrincipal(token))
                .contains(new JwtPrincipal("staff-1", Set.of(Role.ADMIN, Role.CUSTOMER)));
    }

    @Test
    void accessTokenWithNoRolesResolvesToAnEmptyRoleSet() {
        JwtService service = jwtService();

        String token = service.generateAccessToken("customer-42", Set.of());

        assertThat(service.resolvePrincipal(token)).contains(new JwtPrincipal("customer-42", Set.of()));
    }

    @Test
    void tokenWithAnUnknownRoleNameIsRejectedRatherThanThrowing() {
        JwtService service = jwtService();
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        String token =
                Jwts.builder()
                        .subject("customer-42")
                        .claim("type", "ACCESS")
                        .claim("roles", List.of("WIZARD"))
                        .issuedAt(Date.from(now))
                        .expiration(Date.from(now.plusSeconds(60)))
                        .signWith(key)
                        .compact();

        assertThat(service.resolvePrincipal(token)).isEmpty();
    }

    @Test
    void refreshTokenIsRejectedAsAnAccessCredential() {
        JwtService service = jwtService();

        String refreshToken = service.generateRefreshToken("customer-42");

        assertThat(service.resolvePrincipal(refreshToken)).isEmpty();
    }

    @Test
    void expiredAccessTokenIsRejected() {
        JwtService service = jwtService(Duration.ofMillis(-1), Duration.ofDays(7));

        String token = service.generateAccessToken("customer-42", Set.of(Role.CUSTOMER));

        assertThat(service.resolvePrincipal(token)).isEmpty();
    }

    @Test
    void tokenSignedWithADifferentSecretIsRejected() {
        JwtService issuer = jwtService();
        JwtService verifier =
                new JwtService(
                        new JwtProperties(
                                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                                Duration.ofMinutes(15),
                                Duration.ofDays(7),
                                CHALLENGE_TTL));

        String token = issuer.generateAccessToken("customer-42", Set.of(Role.CUSTOMER));

        assertThat(verifier.resolvePrincipal(token)).isEmpty();
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService service = jwtService();

        String token = service.generateAccessToken("customer-42", Set.of(Role.CUSTOMER));
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        assertThat(service.resolvePrincipal(tampered)).isEmpty();
    }

    @Test
    void challengeTokenResolvesToItsSubject() {
        JwtService service = jwtService();

        String token = service.generateChallengeToken("user-7");

        assertThat(service.resolveChallengeSubject(token)).contains("user-7");
    }

    @Test
    void challengeTokenIsRejectedAsAnAccessCredential() {
        JwtService service = jwtService();

        String token = service.generateChallengeToken("user-7");

        assertThat(service.resolvePrincipal(token)).isEmpty();
    }

    @Test
    void refreshTokenIsRejectedAsAChallengeCredential() {
        JwtService service = jwtService();

        String token = service.generateRefreshToken("user-7");

        assertThat(service.resolveChallengeSubject(token)).isEmpty();
    }

    @Test
    void refreshTokenResolvesToItsSubject() {
        JwtService service = jwtService();

        String token = service.generateRefreshToken("user-7");

        assertThat(service.resolveRefreshSubject(token)).contains("user-7");
    }

    @Test
    void expiredChallengeTokenIsRejected() {
        JwtService service = jwtService(Duration.ofMinutes(15), Duration.ofDays(7), Duration.ofMillis(-1));

        String token = service.generateChallengeToken("user-7");

        assertThat(service.resolveChallengeSubject(token)).isEmpty();
    }

    @Test
    void parseClaimsExposesSubjectTypeAndRoles() {
        JwtService service = jwtService();

        String token = service.generateAccessToken("customer-42", Set.of(Role.ADMIN));

        var claims = service.parseClaims(token);
        assertThat(claims.getSubject()).isEqualTo("customer-42");
        assertThat(claims.get("type", String.class)).isEqualTo("ACCESS");
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        assertThat(roles).containsExactly("ADMIN");
    }
}
