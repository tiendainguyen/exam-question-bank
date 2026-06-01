package com.exambank.common;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * Base class for every per-user (personalized) entity. Holds the owning
 * {@code user_id}. Repositories MUST filter by the current user on reads, and
 * services MUST set {@code userId} from
 * {@link com.exambank.security.UserContext#getRequired()} before saving — the
 * column is non-null and non-updatable.
 *
 * <p>Shared reference data (e.g. the question bank) does NOT extend this class.
 */
@MappedSuperclass
public abstract class OwnedEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
}
