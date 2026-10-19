package io.github.huahill.vadmin.springjdbc;

import io.github.huahill.vadmin.contracts.audit.AuditSink;
import io.github.huahill.vadmin.contracts.auth.LocalUserAccountLookup;
import io.github.huahill.vadmin.contracts.auth.LocalUserSessionLookup;
import io.github.huahill.vadmin.contracts.auth.PermissionCatalog;
import io.github.huahill.vadmin.platform.access.AccessControlRepository;
import io.github.huahill.vadmin.platform.administration.AdministrationStore;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcIamAutoConfigurationTest {
    private static final String CONFIGURATION = "io.github.huahill.vadmin.springjdbc.JdbcIamAutoConfiguration";

    @Test
    void discoversCompleteJdbcIamWithoutHostStorageBeans() throws Exception {
        assertThat(ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()))
                .contains(CONFIGURATION);
        var jdbc = JdbcIamTestDatabase.create(UuidStorage.STRING, "ci");
        context(jdbc).withPropertyValues("vadmin.jdbc.enabled=true", "vadmin.jdbc.table-prefix=CI_",
                        "APP_BOOTSTRAP_PASSWORD=first-secret")
                .run(application -> {
                    assertThat(application).hasNotFailed().hasSingleBean(AdministrationStore.class)
                            .hasSingleBean(AccessControlRepository.class).hasSingleBean(LocalUserAccountLookup.class)
                            .hasSingleBean(LocalUserSessionLookup.class).hasSingleBean(AuditSink.class);
                    application.getBean("jdbcIamInitializer", CommandLineRunner.class).run();
                    var account = application.getBean(LocalUserAccountLookup.class).findByUsername("admin").orElseThrow();
                    assertThat(account.enabled()).isTrue();
                    assertThat(application.getBean(LocalUserSessionLookup.class).findSessionByUserId(account.userId()))
                            .isPresent();
                });
    }

    @Test
    void staysInactiveByDefaultAndAllowsIdentityOverrides() throws Exception {
        var jdbc = JdbcIamTestDatabase.create(UuidStorage.STRING, "ci");
        context(jdbc).run(application -> assertThat(application)
                .doesNotHaveBean(AdministrationStore.class).doesNotHaveBean(CommandLineRunner.class));
        LocalUserSessionLookup sessions = id -> java.util.Optional.empty();
        context(jdbc).withPropertyValues("vadmin.jdbc.enabled=true", "vadmin.jdbc.table-prefix=ci")
                .withBean(LocalUserSessionLookup.class, () -> sessions)
                .run(application -> assertThat(application.getBean(LocalUserSessionLookup.class)).isSameAs(sessions));
    }

    private ApplicationContextRunner context(JdbcTemplate jdbc) throws ClassNotFoundException {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(Class.forName(CONFIGURATION)))
                .withBean(JdbcTemplate.class, () -> jdbc)
                .withBean(org.springframework.transaction.PlatformTransactionManager.class,
                        () -> new DataSourceTransactionManager(jdbc.getDataSource()))
                .withBean(org.springframework.security.crypto.password.PasswordEncoder.class,
                        PasswordEncoderFactories::createDelegatingPasswordEncoder)
                .withBean(PermissionCatalog.class, () -> new PermissionCatalog(Set.of()));
    }
}
