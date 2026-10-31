package com.obs.backend.feature.kyc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param directory     where encrypted images are written; should be a mounted, encrypted volume
 * @param encryptionKey base64 of a 32-byte AES-256 key
 * @param maxBytes      per-image upload cap
 */
@ConfigurationProperties(prefix = "kyc.storage")
public record KycProperties(String directory, String encryptionKey, long maxBytes) {
}
