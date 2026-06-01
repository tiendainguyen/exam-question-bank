package com.exambank.security;

import java.util.UUID;

/**
 * Authenticated principal stored in the Spring SecurityContext. Injectable into
 * controllers via {@code @AuthenticationPrincipal AuthUser user}.
 */
public record AuthUser(UUID userId, String email) {
}
