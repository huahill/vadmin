package io.github.huahill.vadmin.springjpa;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.huahill.vadmin.contracts.audit.AuditEvent;
import io.github.huahill.vadmin.contracts.audit.AuditMetadataRedactor;
import io.github.huahill.vadmin.contracts.audit.AuditOutcome;
import io.github.huahill.vadmin.contracts.auth.LocalUserAccountLookup;
import io.github.huahill.vadmin.contracts.auth.LocalUserSessionLookup;
import io.github.huahill.vadmin.springjpa.access.JpaLocalUserAccountLookup;
import io.github.huahill.vadmin.springjpa.access.JpaLocalUserSessionLookup;
import io.github.huahill.vadmin.springjpa.audit.JpaAuditSink;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = MySqlChar36IdentityTest.TestApplication.class)
class MySqlChar36IdentityTest {
    private static final UUID USER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private EntityManager entityManager;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                "jdbc:h2:mem:vadmin-char36;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.schema-locations", () -> "classpath:mysql-iam/schema.sql");
    }

    @Test
    @Transactional
    void should_round_trip_a_char36_user_session_and_audit_row() {
        entityManager.createNativeQuery("""
                insert into vadmin_users (id, username, password_hash, enabled, auth_version)
                values (:id, 'ada', 'hash', true, 2)
                """).setParameter("id", USER_ID.toString()).executeUpdate();
        entityManager.flush();
        entityManager.clear();

        LocalUserAccountLookup accounts = new JpaLocalUserAccountLookup(entityManager, AccessControlTables.of(null));
        LocalUserSessionLookup sessions = new JpaLocalUserSessionLookup(entityManager);
        var audit = new JpaAuditSink(entityManager, new AuditMetadataRedactor());

        assertThat(accounts.findByUserId(USER_ID)).get().satisfies(account -> {
            assertThat(account.userId()).isEqualTo(USER_ID);
            assertThat(account.username()).isEqualTo("ada");
        });
        assertThat(sessions.findSessionByUserId(USER_ID)).get()
                .extracting(session -> session.userId())
                .isEqualTo(USER_ID);

        audit.append(new AuditEvent(USER_ID, "user.enabled", "user", USER_ID.toString(), AuditOutcome.SUCCESS,
                Instant.parse("2026-11-17T02:00:00Z"), "corr-1", Map.of()));
        entityManager.flush();
        Object storedId = entityManager.createNativeQuery(
                "select actor_user_id from vadmin_audit_entries where correlation_id = 'corr-1'")
                .getSingleResult();
        assertThat(storedId.toString()).isEqualTo(USER_ID.toString());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            FlywayAutoConfiguration.class,
            JpaAccessControlConfiguration.class
    })
    @EntityScan(basePackages = "io.github.huahill.vadmin.springjpa")
    static class TestApplication { }
}
