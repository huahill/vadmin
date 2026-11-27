package io.github.huahill.vadmin.springjdbc;

import io.github.huahill.vadmin.contracts.audit.AuditMetadataRedactor;
import io.github.huahill.vadmin.contracts.audit.AuditSink;
import io.github.huahill.vadmin.contracts.auth.ExternalAccessResolver;
import io.github.huahill.vadmin.contracts.auth.ExternalDirectory;
import io.github.huahill.vadmin.contracts.auth.LocalUserAccountLookup;
import io.github.huahill.vadmin.contracts.auth.LocalUserSessionLookup;
import io.github.huahill.vadmin.contracts.auth.PermissionCatalog;
import io.github.huahill.vadmin.platform.access.AccessControlRepository;
import io.github.huahill.vadmin.platform.access.ExternalGrantStore;
import io.github.huahill.vadmin.platform.administration.AdministrationStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Complete JDBC IAM persistence and first-start initialization for an existing schema. */
@AutoConfiguration(afterName = "org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration")
@ConditionalOnProperty(prefix = "vadmin.jdbc", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(JdbcIamProperties.class)
public class JdbcIamAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    AdministrationStore administrationStore(JdbcTemplate jdbc, JdbcIamProperties properties) {
        return new JdbcAdministrationStore(jdbc, properties.getTablePrefix(), properties.getUuidStorage());
    }

    @Bean
    @ConditionalOnMissingBean
    AccessControlRepository accessControlRepository(JdbcTemplate jdbc, JdbcIamProperties properties,
                                                    Environment environment) {
        boolean externalIdentity = "external".equalsIgnoreCase(environment.getProperty("vadmin.identity.mode", "local"));
        return new JdbcAccessControlRepository(jdbc, properties.getTablePrefix(), properties.getUuidStorage(),
                externalIdentity);
    }

    @Bean
    @ConditionalOnMissingBean
    LocalUserAccountLookup localUserAccountLookup(JdbcTemplate jdbc, JdbcIamProperties properties) {
        return new JdbcLocalUserAccountLookup(jdbc, properties.getTablePrefix(), properties.getUuidStorage());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "vadmin.identity", name = "mode", havingValue = "local", matchIfMissing = true)
    LocalUserSessionLookup localUserSessionLookup(JdbcTemplate jdbc, JdbcIamProperties properties) {
        return new JdbcLocalUserSessionLookup(jdbc, properties.getTablePrefix(), properties.getUuidStorage());
    }

    @Bean
    @ConditionalOnMissingBean(ExternalGrantStore.class)
    @ConditionalOnProperty(prefix = "vadmin.identity", name = "mode", havingValue = "external")
    ExternalGrantStore externalGrantStore(JdbcTemplate jdbc, JdbcIamProperties properties) {
        return new JdbcExternalGrantStore(jdbc, properties.getTablePrefix(), properties.getUuidStorage());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "vadmin.identity", name = "mode", havingValue = "external")
    LocalUserSessionLookup externalUserSessionLookup(ExternalGrantStore grants) {
        return new JdbcExternalSessionLookup(grants);
    }

    @Bean
    @ConditionalOnMissingBean(ExternalAccessResolver.class)
    @ConditionalOnProperty(prefix = "vadmin.identity", name = "mode", havingValue = "external")
    ExternalAccessResolver externalAccessResolver(ExternalGrantStore grants) {
        return new JdbcExternalAccessResolver(grants);
    }

    @Bean
    @ConditionalOnMissingBean
    AuditMetadataRedactor auditMetadataRedactor() {
        return new AuditMetadataRedactor();
    }

    @Bean
    @ConditionalOnMissingBean
    AuditSink auditSink(JdbcTemplate jdbc, JdbcIamProperties properties, AuditMetadataRedactor redactor) {
        return new JdbcAuditSink(jdbc, properties.getTablePrefix(), properties.getUuidStorage(), redactor);
    }

    @Bean
    @DependsOnDatabaseInitialization
    @Order(Ordered.HIGHEST_PRECEDENCE + 100)
    @ConditionalOnMissingBean(name = "jdbcIamInitializer")
    CommandLineRunner jdbcIamInitializer(JdbcTemplate jdbc, JdbcIamProperties properties,
                                         PasswordEncoder passwords, PermissionCatalog catalog,
                                         Environment environment, PlatformTransactionManager transactionManager,
                                         ObjectProvider<ExternalGrantStore> externalGrants,
                                         ObjectProvider<ExternalDirectory> directory) {
        return new JdbcIamInitializer(new JdbcIamStore(jdbc, properties.getTablePrefix(), properties.getUuidStorage()),
                passwords, catalog, environment, new TransactionTemplate(transactionManager),
                externalGrants.getIfAvailable(), directory.getIfAvailable());
    }
}
