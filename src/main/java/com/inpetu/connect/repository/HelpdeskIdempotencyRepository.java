package com.inpetu.connect.repository;

import com.inpetu.connect.model.HelpdeskIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HelpdeskIdempotencyRepository extends JpaRepository<HelpdeskIdempotency, Long> {
    Optional<HelpdeskIdempotency> findByScopeHashAndIdempotencyKey(byte[] scopeHash, byte[] idempotencyKey);
}
