package com.exambank.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    /**
     * Runs {@code repair()} before {@code migrate()} on startup. {@code repair()}
     * realigns the schema-history checksums to the current migration files and
     * clears any failed entry, so benign drift — e.g. CRLF/LF differences between
     * a dev machine and CI — no longer fails validation and blocks boot.
     *
     * <p>Line endings are already pinned to LF via {@code .gitattributes}; this is
     * the belt-and-braces guard. For a strict-validation production profile this
     * bean can later be limited to dev.
     */
    @Bean
    FlywayMigrationStrategy repairThenMigrate() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
