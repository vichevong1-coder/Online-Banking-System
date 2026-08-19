package com.obs.backend.feature.qr.repository;

import com.obs.backend.feature.qr.entity.Merchant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    /** The only read path: resolve the code scanned off a merchant QR (US-034). */
    Optional<Merchant> findByMerchantCode(String merchantCode);
}
