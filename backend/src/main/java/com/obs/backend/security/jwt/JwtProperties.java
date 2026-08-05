package com.obs.backend.security.jwt;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret, Duration accessTokenTtl, Duration refreshTokenTtl, Duration challengeTokenTtl) {
}
