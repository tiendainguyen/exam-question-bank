package com.exambank.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base for integration tests. Boots a real Postgres with the pgvector
 * extension (matches production image) so Flyway's {@code CREATE EXTENSION
 * vector} runs. No H2. The container is static → shared across the test class.
 */
@Testcontainers
public abstract class AbstractPostgresIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));
}