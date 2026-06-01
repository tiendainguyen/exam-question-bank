package com.exambank.user;

import java.util.UUID;

import com.exambank.common.ApiResponse;
import com.exambank.security.AuthUser;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal protected endpoint proving JWT auth + principal resolution work
 * end to end. Returns the caller's identity.
 */
@RestController
@RequestMapping("/api")
public class MeController {

    public record MeResponse(UUID userId, String email) {
    }

    @GetMapping("/me")
    public ApiResponse<MeResponse> me(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(new MeResponse(user.userId(), user.email()));
    }
}