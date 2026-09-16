package com.aileapro.api;

import com.aileapro.persistence.EmailTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailTokenRepository extends JpaRepository<EmailTokenEntity, UUID> {
    Optional<EmailTokenEntity> findByTokenHashAndPurpose(String tokenHash, String purpose);
}
