package com.inpetu.connect.service;

import com.inpetu.connect.model.HelpdeskRateLock;
import com.inpetu.connect.model.HelpdeskRateLockId;
import com.inpetu.connect.repository.HelpdeskRateLockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class HelpdeskRateLockService {
    private final HelpdeskRateLockRepository rateLocks;

    public HelpdeskRateLockService(HelpdeskRateLockRepository rateLocks) {
        this.rateLocks = rateLocks;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureLock(String dimension, String keyHash) {
        HelpdeskRateLockId id = new HelpdeskRateLockId(dimension, keyHash);
        if (rateLocks.existsById(id)) {
            return;
        }
        HelpdeskRateLock row = new HelpdeskRateLock();
        row.setId(id);
        row.setUpdatedAt(LocalDateTime.now());
        rateLocks.saveAndFlush(row);
    }
}
