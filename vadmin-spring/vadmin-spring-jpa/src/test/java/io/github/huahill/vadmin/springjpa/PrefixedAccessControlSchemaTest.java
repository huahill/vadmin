package io.github.huahill.vadmin.springjpa;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.huahill.vadmin.contracts.auth.LocalUserAccountLookup;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = PrefixedAccessControlSchemaTest.TestApplication.class)
@Testcontainers
class PrefixedAccessControlSchemaTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private LocalUserAccountLookup lookup;

    @Autowired
    private EntityManager entityManager;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("vadmin.local-iam.table-prefix", () -> "ci");
    }

    @Test
    @Transactional
    void validatesTheConfiguredPrefixAndReadsTheRenamedUserTable() {
        assertThat(names("""
                select table_name
                from information_schema.tables
                where table_schema = 'public'
                  and table_name in ('users', 'vadmin_users', 'ci_users')
                """)).containsExactly("ci_users");
        assertThat(names("""
                select constraint_name
                from information_schema.table_constraints
                where table_schema = 'public'
                  and table_name = 'ci_users'
                  and constraint_name = 'uk_users_username'
                """)).containsExactly("uk_users_username");

        var userId = UUID.randomUUID();
        entityManager.createNativeQuery("""
                insert into ci_users (id, username, password_hash, enabled, auth_version)
                values (:id, 'operator', 'stored-hash', true, 3)
                """).setParameter("id", userId).executeUpdate();

        var account = lookup.findByUsername("operator").orElseThrow();

        assertThat(account.userId()).isEqualTo(userId);
        assertThat(account.passwordHash()).isEqualTo("stored-hash");
        assertThat(account.authVersion()).isEqualTo(3);
    }

    @SuppressWarnings("unchecked")
    private List<String> names(String sql) {
        return entityManager.createNativeQuery(sql).getResultList();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication { }
}
