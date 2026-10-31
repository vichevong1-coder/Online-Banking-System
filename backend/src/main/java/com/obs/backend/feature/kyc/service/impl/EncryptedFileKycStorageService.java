package com.obs.backend.feature.kyc.service.impl;

import com.obs.backend.feature.kyc.config.KycProperties;
import com.obs.backend.feature.kyc.service.KycStorageService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * AES-256-GCM, one random 12-byte IV per file, stored as {@code IV || ciphertext+tag}. GCM's tag
 * means a modified file fails to decrypt rather than yielding altered bytes. The directory is
 * expected to sit on an encrypted volume as well; this layer protects the files if the volume is
 * mounted or copied elsewhere.
 */
@Service
public class EncryptedFileKycStorageService implements KycStorageService {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final Pattern KEY_PATTERN = Pattern.compile("[0-9a-f-]{36}\\.enc");

    private final Path root;
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public EncryptedFileKycStorageService(KycProperties properties) {
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(properties.encryptionKey());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("kyc.storage.encryption-key must be base64", e);
        }
        if (raw.length != 32) {
            throw new IllegalStateException("kyc.storage.encryption-key must decode to 32 bytes (AES-256)");
        }
        this.key = new SecretKeySpec(raw, "AES");
        this.root = Path.of(properties.directory()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create KYC storage directory " + root, e);
        }
    }

    @Override
    public String store(byte[] plaintext) {
        String storageKey = UUID.randomUUID() + ".enc";
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext);
            byte[] out = ByteBuffer.allocate(IV_BYTES + ciphertext.length).put(iv).put(ciphertext).array();
            Files.write(resolve(storageKey), out, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return storageKey;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not encrypt KYC image", e);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write KYC image", e);
        }
    }

    @Override
    public byte[] load(String storageKey) {
        try {
            byte[] stored = Files.readAllBytes(resolve(storageKey));
            if (stored.length <= IV_BYTES) {
                throw new IllegalStateException("KYC image file is truncated");
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, stored, 0, IV_BYTES));
            return cipher.doFinal(stored, IV_BYTES, stored.length - IV_BYTES);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("KYC image failed integrity check", e);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read KYC image", e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete KYC image", e);
        }
    }

    private Path resolve(String storageKey) {
        if (!KEY_PATTERN.matcher(storageKey).matches()) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        Path path = root.resolve(storageKey).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return path;
    }
}
