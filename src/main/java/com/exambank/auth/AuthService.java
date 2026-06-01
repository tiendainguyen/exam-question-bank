package com.exambank.auth;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.exambank.auth.dto.AuthResponse;
import com.exambank.auth.dto.LoginRequest;
import com.exambank.auth.dto.SignupRequest;
import com.exambank.common.exception.EmailAlreadyUsedException;
import com.exambank.common.exception.InvalidCredentialsException;
import com.exambank.security.JwtService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException(email);
        }
        AppUser user = new AppUser(
                UUID.randomUUID(),
                email,
                passwordEncoder.encode(request.password()),
                request.displayName(),
                Instant.now());
        userRepository.save(user);
        return tokenFor(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return tokenFor(user);
    }

    private AuthResponse tokenFor(AppUser user) {
        String token = jwtService.issue(user.getId(), user.getEmail());
        return AuthResponse.bearer(token, user.getId(), user.getEmail());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}