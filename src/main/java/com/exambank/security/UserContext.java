package com.exambank.security;

import java.util.Optional;
import java.util.UUID;

/**
 * ThreadLocal holder for the current authenticated user's id. Set by
 * {@link JwtAuthFilter} after token validation and ALWAYS cleared in a
 * {@code finally} block to prevent leaking identity across pooled threads.
 *
 * <p>Services use {@link #getRequired()} when creating personalized entities
 * (see {@link com.exambank.common.OwnedEntity}).
 */
public final class UserContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(UUID userId) {
        CURRENT.set(userId);
    }

    public static Optional<UUID> get() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static UUID getRequired() {
        UUID userId = CURRENT.get();
        if (userId == null) {
            throw new IllegalStateException("No authenticated user in context");
        }
        return userId;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
