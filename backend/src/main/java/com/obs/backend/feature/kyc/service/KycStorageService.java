package com.obs.backend.feature.kyc.service;

/** Stores KYC image bytes encrypted at rest. Callers only ever see plaintext and an opaque key. */
public interface KycStorageService {

    /** Encrypts and persists the bytes, returning the key to load or delete them with. */
    String store(byte[] plaintext);

    byte[] load(String storageKey);

    void delete(String storageKey);
}
