package io.github.huahill.vadmin.springjpa;

import io.github.huahill.vadmin.contracts.audit.AuditMetadataRedactor;
import io.github.huahill.vadmin.contracts.audit.AuditSink;
import io.github.huahill.vadmin.contracts.auth.LocalUserAccountLookup;
import io.github.huahill.vadmin.contracts.auth.LocalUserSessionLookup;
import io.github.huahill.vadmin.platform.access.AccessControlRepository;
import io.github.huahill.vadmin.platform.administration.AdministrationStore;
import io.github.huahill.vadmin.springjpa.access.JpaAccessControlRepository;
import io.github.huahill.vadmin.springjpa.access.JpaLocalUserAccountLookup;
import io.github.huahill.vadmin.springjpa.access.JpaLocalUserSessionLookup;
import io.github.huahill.vadmin.springjpa.access.PermissionCatalogSynchronizer;
import io.github.huahill.vadmin.springjpa.administration.JpaAdministrationStore;
import io.github.huahill.vadmin.springjpa.audit.JpaAuditSink;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@AutoConfiguration
@AutoConfigureBefore({
        org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration.class,
        org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration.class
})
@EnableConfigurationProperties(LocalIamProperties.class)
public class JpaAccessControlConfiguration {
    @Bean
    @ConditionalOnBean(DataSource.class)
    AdministrationStore administrationStore(EntityManager entityManager, LocalIamProperties properties) {
        return new JpaAdministrationStore(entityManager, properties.tables());
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
    @ConditionalOnBean(DataSource.class)
    AccessControlRepository accessControlRepository(EntityManager entityManager, AccessControlTables tables) {
        return new JpaAccessControlRepository(entityManager, tables);
    }

    @Bean
    @ConditionalOnBean(DataSource.class)
    LocalUserAccountLookup localUserAccountLookup(EntityManager entityManager, AccessControlTables tables) {
        return new JpaLocalUserAccountLookup(entityManager, tables);
    }

    @Bean
    @ConditionalOnBean(DataSource.class)
    LocalUserSessionLookup localUserSessionLookup(EntityManager entityManager) {
        return new JpaLocalUserSessionLookup(entityManager);
    }

    @Bean
    AuditMetadataRedactor auditMetadataRedactor() {
        return new AuditMetadataRedactor();
    }

    @Bean
    @ConditionalOnBean(DataSource.class)
    AuditSink auditSink(EntityManager entityManager, AuditMetadataRedactor redactor) {
        return new JpaAuditSink(entityManager, redactor);
    }

    @Bean
    @ConditionalOnBean(DataSource.class)
    PermissionCatalogSynchronizer permissionCatalogSynchronizer(EntityManager entityManager) {
        return new PermissionCatalogSynchronizer(entityManager);
    }
}
