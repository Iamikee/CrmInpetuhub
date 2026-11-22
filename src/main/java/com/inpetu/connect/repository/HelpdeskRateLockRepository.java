package com.inpetu.connect.repository;

import com.inpetu.connect.model.HelpdeskRateLock;
import com.inpetu.connect.model.HelpdeskRateLockId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HelpdeskRateLockRepository extends JpaRepository<HelpdeskRateLock, HelpdeskRateLockId> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rateLock from HelpdeskRateLock rateLock where rateLock.id.dimension = :dimension and rateLock.id.keyHash = :keyHash")
    Optional<HelpdeskRateLock> findLocked(@Param("dimension") String dimension,
                                          @Param("keyHash") String keyHash);
}
