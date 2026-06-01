package com.exambank;

import java.util.Map;

import com.exambank.support.AbstractPostgresIT;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Foundation isolation test: each authenticated user resolves to their OWN
 * identity, and protected routes reject anonymous access. This is the harness
 * that later epics extend with real owned-data cross-tenant assertions once
 * personalized entities exist.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserIsolationIT extends AbstractPostgresIT {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void eachUserSeesOnlyTheirOwnIdentity() {
        String tokenA = signup("alice@example.com", "password123");
        String tokenB = signup("bob@example.com", "password123");

        Map<String, Object> meA = me(tokenA);
        Map<String, Object> meB = me(tokenB);

        assertThat(meA.get("email")).isEqualTo("alice@example.com");
        assertThat(meB.get("email")).isEqualTo("bob@example.com");
        assertThat(meA.get("userId")).isNotNull();
        // Two distinct users must never collapse to the same identity.
        assertThat(meA.get("userId")).isNotEqualTo(meB.get("userId"));
    }

    @Test
    void protectedEndpointRejectsAnonymousAccess() {
        ResponseEntity<String> response = rest.getForEntity("/api/me", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginReturnsAWorkingToken() {
        signup("carol@example.com", "password123");
        String token = login("carol@example.com", "password123");
        assertThat(me(token).get("email")).isEqualTo("carol@example.com");
    }

    @Test
    void duplicateEmailSignupIsRejected() {
        signup("dave@example.com", "password123");
        ResponseEntity<String> second = rest.postForEntity("/api/auth/signup",
                json(Map.of("email", "dave@example.com", "password", "password123")), String.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // --- helpers ---

    @SuppressWarnings("unchecked")
    private String signup(String email, String password) {
        ResponseEntity<Map> res = rest.postForEntity("/api/auth/signup",
                json(Map.of("email", email, "password", password)), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) dataOf(res.getBody()).get("accessToken");
    }

    @SuppressWarnings("unchecked")
    private String login(String email, String password) {
        ResponseEntity<Map> res = rest.postForEntity("/api/auth/login",
                json(Map.of("email", email, "password", password)), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) dataOf(res.getBody()).get("accessToken");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> me(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<Map> res = rest.exchange("/api/me", HttpMethod.GET,
                new HttpEntity<>(headers), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return dataOf(res.getBody());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> dataOf(Map<String, Object> body) {
        return (Map<String, Object>) body.get("data");
    }

    private HttpEntity<Map<String, String>> json(Map<String, String> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}