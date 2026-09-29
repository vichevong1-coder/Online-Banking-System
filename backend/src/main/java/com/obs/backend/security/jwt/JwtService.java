package com.obs.backend.security.jwt;

import com.obs.backend.security.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ROLES_CLAIM = "roles";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String subject, Set<Role> roles) {
        Instant now = Instant.now();
        @SuppressWarnings("null")
        List<String> roleNames = roles.stream().map(Role::name).toList();
        return Jwts.builder()
                .subject(subject)
                .claim(TOKEN_TYPE_CLAIM, JwtTokenType.ACCESS.name())
                .claim(ROLES_CLAIM, roleNames)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTokenTtl())))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(String subject) {
        return generateToken(subject, JwtTokenType.REFRESH, properties.refreshTokenTtl());
    }

    public String generateChallengeToken(String subject) {
        return generateToken(subject, JwtTokenType.CHALLENGE, properties.challengeTokenTtl());
    }

    private String generateToken(String subject, JwtTokenType type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(java.util.UUID.randomUUID().toString())
                .subject(subject)
                .claim(TOKEN_TYPE_CLAIM, type.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Returns the authenticated principal (subject + roles) if the token is a signature-valid,
     * unexpired ACCESS token — empty otherwise (expired, tampered, or a refresh token presented
     * as a bearer credential).
     */
    public Optional<JwtPrincipal> resolvePrincipal(String token) {
        return parseIfType(token, JwtTokenType.ACCESS)
                .flatMap(claims -> {
                    try {
                        return Optional.of(new JwtPrincipal(claims.getSubject(), extractRoles(claims)));
                    } catch (IllegalArgumentException e) {
                        return Optional.empty();
                    }
                });
    }

    /**
     * Returns the subject of a signature-valid, unexpired CHALLENGE token — the short-lived
     * token issued by /auth/login that a client exchanges for real tokens at /auth/2fa/verify.
     */
    @SuppressWarnings("null")
    public Optional<String> resolveChallengeSubject(String token) {
        return parseIfType(token, JwtTokenType.CHALLENGE).map(Claims::getSubject);
    }

    /**
     * Returns the subject of a signature-valid, unexpired REFRESH token — used by /auth/refresh
     * to reissue an access token.
     */
    @SuppressWarnings("null")
    public Optional<String> resolveRefreshSubject(String token) {
        return parseIfType(token, JwtTokenType.REFRESH).map(Claims::getSubject);
    }

    private Optional<Claims> parseIfType(String token, JwtTokenType expectedType) {
        try {
            Claims claims = parseClaims(token);
            if (!expectedType.name().equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private Set<Role> extractRoles(Claims claims) {
        List<String> roleNames = claims.get(ROLES_CLAIM, List.class);
        if (roleNames == null) {
            return Set.of();
        }
        return roleNames.stream().map(Role::valueOf).collect(Collectors.toUnmodifiableSet());
    }
}
