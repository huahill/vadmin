package io.github.huahill.vadmin.springjpa;

import io.github.huahill.vadmin.contracts.audit.AuditMetadataRedactor;
import io.github.huahill.vadmin.contracts.audit.AuditSink;
import io.github.huahill.vadmin.contracts.auth.LocalUserAccountLookup;
import io.github.huahill.vadmin.contracts.auth.LocalUserSessionLookup;
import io.github.huahill.vadmin.platform.access.AccessControlRepository;
import io.github.huahill.vadmin.platform.administration.AdministrationStore;
import io.github.huahill.vadmin.springjdbc.JdbcAdministrationStore;
import io.github.huahill.vadmin.springjdbc.UuidStorage;
import io.github.huahill.vadmin.springjpa.access.JpaAccessControlRepository;
import io.github.huahill.vadmin.springjpa.access.JpaLocalUserAccountLookup;
import io.github.huahill.vadmin.springjpa.access.JpaLocalUserSessionLookup;
import io.github.huahill.vadmin.springjpa.access.PermissionCatalogSynchronizer;
import io.github.huahill.vadmin.springjpa.audit.JpaAuditSink;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@AutoConfiguration
@AutoConfigureBefore({
        org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration.class,
        org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration.class
})
@EnableConfigurationProperties(LocalIamProperties.class)
public class JpaAccessControlConfiguration {
    @Bean
    AdministrationStore administrationStore(JdbcTemplate jdbcTemplate, LocalIamProperties properties) {
        return new JdbcAdministrationStore(jdbcTemplate, properties.getTablePrefix(), UuidStorage.NATIVE);
    }

    @Bean
    AccessControlTables accessControlTables(LocalIamProperties properties) {
        return properties.tables();
    }

    @Bean
    AccessControlFlywayCustomizer accessControlFlywayCustomizer(LocalIamProperties properties) {
        return new AccessControlFlywayCustomizer(properties);
    }

    @Bean
    AccessControlNamingCustomizer accessControlNamingCustomizer(LocalIamProperties properties) {
        return new AccessControlNamingCustomizer(properties);
    }

    @Bean
    AccessControlRepository accessControlRepository(EntityManager entityManager, AccessControlTables tables) {
        return new JpaAccessControlRepository(entityManager, tables);
    }

    @Bean
    LocalUserAccountLookup localUserAccountLookup(EntityManager entityManager, AccessControlTables tables) {
        return new JpaLocalUserAccountLookup(entityManager, tables);
    }

    @Bean
    LocalUserSessionLookup localUserSessionLookup(EntityManager entityManager) {
        return new JpaLocalUserSessionLookup(entityManager);
    }

    @Bean
    AuditMetadataRedactor auditMetadataRedactor() {
        return new AuditMetadataRedactor();
    }

    @Bean
    AuditSink auditSink(EntityManager entityManager, AuditMetadataRedactor redactor) {
        return new JpaAuditSink(entityManager, redactor);
    }

    @Bean
    PermissionCatalogSynchronizer permissionCatalogSynchronizer(EntityManager entityManager) {
        return new PermissionCatalogSynchronizer(entityManager);
    }
}
