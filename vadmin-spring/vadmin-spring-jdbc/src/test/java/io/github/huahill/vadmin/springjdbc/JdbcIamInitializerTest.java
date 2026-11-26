package io.github.huahill.vadmin.springjdbc;

import io.github.huahill.vadmin.contracts.auth.PermissionCatalog;
import io.github.huahill.vadmin.contracts.auth.PermissionCode;
import io.github.huahill.vadmin.contracts.auth.ExternalDirectory;
import io.github.huahill.vadmin.contracts.auth.ExternalPerson;
import io.github.huahill.vadmin.contracts.auth.SubjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.mock.env.MockEnvironment;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class JdbcIamInitializerTest {

    private JdbcTemplate jdbc;
    private JdbcIamInitializer initializer;
    private TransactionTemplate transactions;

    @BeforeEach
    void setUp() throws SQLException {
        jdbc = JdbcIamTestDatabase.create(UuidStorage.STRING, "vadmin");
        transactions = new TransactionTemplate(new DataSourceTransactionManager(jdbc.getDataSource()));
        initializer = new JdbcIamInitializer(new JdbcIamStore(jdbc, "vadmin", UuidStorage.STRING), mock(PasswordEncoder.class),
                new PermissionCatalog(Set.of(
                        PermissionCode.of("environment:catalog:read"),
                        PermissionCode.of("environment:catalog:write"))),
                mock(Environment.class), transactions, null);
    }

    @Test
    void grantsRenamedCatalogPermissionsToExistingAdministrator() {
        String userId = UUID.randomUUID().toString();
        String roleId = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO vadmin_users (id, username, password_hash, enabled, auth_version) "
                + "VALUES (?, 'admin', '{bcrypt}hash', TRUE, 0)", userId);
        jdbc.update("INSERT INTO vadmin_roles (id, code) VALUES (?, 'administrator')", roleId);
        jdbc.update("INSERT INTO vadmin_user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        String stalePermissionId = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO vadmin_permissions (id, code, system_managed) VALUES (?, 'rdcci:catalog:read', TRUE)",
                stalePermissionId);
        jdbc.update("INSERT INTO vadmin_role_permissions (role_id, permission_id) VALUES (?, ?)", roleId, stalePermissionId);

        initializer.run();

        assertThat(jdbc.queryForList(
                "SELECT p.code FROM vadmin_role_permissions rp JOIN vadmin_permissions p ON p.id = rp.permission_id WHERE rp.role_id = ?",
                String.class, roleId))
                .contains("environment:catalog:read", "environment:catalog:write")
                .contains("rdcci:catalog:read");
        assertThat(jdbc.queryForObject("SELECT auth_version FROM vadmin_users WHERE id = ?", Long.class, userId))
                .isEqualTo(1);

        initializer.run();

        assertThat(jdbc.queryForObject("SELECT auth_version FROM vadmin_users WHERE id = ?", Long.class, userId))
                .isEqualTo(1);
    }

    @Test
    void should_synchronizePermissionsInStableLockOrderAcrossReplicas() {
        var store = mock(JdbcIamStore.class);
        org.mockito.Mockito.when(store.hasAnyUser()).thenReturn(true);
        var runner = new JdbcIamInitializer(store, mock(PasswordEncoder.class),
                new PermissionCatalog(java.util.stream.IntStream.range(0, 20)
                        .mapToObj(i -> PermissionCode.of("system:permission:read" + i))
                        .collect(java.util.stream.Collectors.toSet())), mock(Environment.class), transactions, null);

        runner.run();

        org.mockito.Mockito.verify(store).synchronizePermissions(org.mockito.ArgumentMatchers.argThat(codes ->
                java.util.List.copyOf(codes).equals(codes.stream().sorted().toList())));
    }

    @Test
    void should_rollbackAllBootstrapWritesWhenInitialPasswordIsMissing() {
        var environment = new MockEnvironment();
        initializer = new JdbcIamInitializer(new JdbcIamStore(jdbc, "vadmin", UuidStorage.STRING), mock(PasswordEncoder.class),
                new PermissionCatalog(Set.of(PermissionCode.of("system:user:read"))), environment, transactions, null);

        assertThatThrownBy(initializer::run).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_BOOTSTRAP_PASSWORD");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM vadmin_permissions", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM vadmin_users", Long.class)).isZero();
    }

    @Test
    void should_createAdministratorOnceAndKeepItsPasswordOnRestart() {
        var passwords = org.springframework.security.crypto.factory.PasswordEncoderFactories.createDelegatingPasswordEncoder();
        var environment = new MockEnvironment().withProperty("APP_BOOTSTRAP_PASSWORD", "first-secret");
        initializer = new JdbcIamInitializer(new JdbcIamStore(jdbc, "vadmin", UuidStorage.STRING), passwords,
                new PermissionCatalog(Set.of(PermissionCode.of("system:user:read"))), environment, transactions, null);

        initializer.run();
        String hash = jdbc.queryForObject("SELECT password_hash FROM vadmin_users WHERE username = 'admin'", String.class);
        assertThat(passwords.matches("first-secret", hash)).isTrue();
        environment.setProperty("APP_BOOTSTRAP_PASSWORD", "changed-secret");
        initializer.run();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM vadmin_users", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT password_hash FROM vadmin_users WHERE username = 'admin'", String.class))
                .isEqualTo(hash);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM vadmin_user_roles", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM vadmin_role_permissions", Long.class)).isEqualTo(1);
    }

    @Test
    void should_rollbackTheUserWhenRoleCreationFails() {
        jdbc.update("INSERT INTO vadmin_roles (id, code) VALUES (?, 'administrator')", UUID.randomUUID().toString());
        var encoder = mock(PasswordEncoder.class);
        org.mockito.Mockito.when(encoder.encode("secret")).thenReturn("hash");
        var runner = new JdbcIamInitializer(new JdbcIamStore(jdbc, "vadmin", UuidStorage.STRING), encoder,
                new PermissionCatalog(Set.of()), new MockEnvironment().withProperty("APP_BOOTSTRAP_PASSWORD", "secret"),
                transactions, null);

        assertThatThrownBy(runner::run).isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM vadmin_users", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM vadmin_user_roles", Long.class)).isZero();
    }

    @Test
    void should_allowConcurrentFirstStartsWithoutResettingTheWinningAccount() throws Exception {
        var store = org.mockito.Mockito.spy(new JdbcIamStore(jdbc, "vadmin", UuidStorage.STRING));
        var ready = new java.util.concurrent.CountDownLatch(2);
        org.mockito.Mockito.doAnswer(invocation -> {
            boolean exists = (boolean) invocation.callRealMethod();
            if (!exists) {
                ready.countDown();
                assertThat(ready.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            }
            return exists;
        }).when(store).hasAnyUser();
        var encoder = mock(PasswordEncoder.class);
        org.mockito.Mockito.when(encoder.encode("secret")).thenReturn("hash");
        var runner = new JdbcIamInitializer(store, encoder, new PermissionCatalog(Set.of()),
                new MockEnvironment().withProperty("APP_BOOTSTRAP_PASSWORD", "secret"), transactions, null);

        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> runner.run());
            var second = executor.submit(() -> runner.run());
            first.get(10, java.util.concurrent.TimeUnit.SECONDS);
            second.get(10, java.util.concurrent.TimeUnit.SECONDS);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM vadmin_users", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM vadmin_roles", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM vadmin_user_roles", Long.class)).isEqualTo(1);
    }

    @Test
    void should_resolveABootstrapLoginNameToTheExternalSubject() {
        var environment = new MockEnvironment().withProperty("vadmin.identity.bootstrap-subjects", "HuangX10");
        ExternalDirectory directory = query -> List.of(
                new ExternalPerson(SubjectId.of("someone-else"), "other", "其他"),
                new ExternalPerson(SubjectId.of("user-480"), "huangx10", "黄校"));
        var runner = externalInitializer(environment, directory);

        runner.run();

        assertThat(jdbc.queryForObject("SELECT subject FROM vadmin_external_subjects", String.class)).isEqualTo("user-480");
        assertThat(jdbc.queryForObject("SELECT label FROM vadmin_external_subjects", String.class)).isEqualTo("huangx10");
        assertThat(jdbc.queryForObject(
                "SELECT r.code FROM vadmin_external_grants g JOIN vadmin_roles r ON r.id = g.role_id", String.class))
                .isEqualTo("administrator");
    }

    @Test
    void should_keepABootstrapSubjectIdWhenTheDirectoryReturnsThatPerson() {
        var environment = new MockEnvironment().withProperty("vadmin.identity.bootstrap-subjects", "user-480");
        ExternalDirectory directory = query -> List.of(
                new ExternalPerson(SubjectId.of("user-480"), "huangx10", "黄校"));
        externalInitializer(environment, directory).run();

        assertThat(jdbc.queryForObject("SELECT subject FROM vadmin_external_subjects", String.class)).isEqualTo("user-480");
        assertThat(jdbc.queryForObject("SELECT label FROM vadmin_external_subjects", String.class)).isEqualTo("huangx10");
    }

    @Test
    void should_rejectABootstrapLoginNameThatDoesNotIdentifyOnePerson() {
        var environment = new MockEnvironment().withProperty("vadmin.identity.bootstrap-subjects", "huangx10");
        ExternalDirectory directory = query -> List.of(
                new ExternalPerson(SubjectId.of("one"), "huangx10", "黄校"),
                new ExternalPerson(SubjectId.of("two"), "HuangX10", "另一人"));

        assertThatThrownBy(externalInitializer(environment, directory)::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("huangx10");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM vadmin_external_subjects", Long.class)).isZero();
    }

    private JdbcIamInitializer externalInitializer(MockEnvironment environment, ExternalDirectory directory) {
        return new JdbcIamInitializer(new JdbcIamStore(jdbc, "vadmin", UuidStorage.STRING), mock(PasswordEncoder.class),
                new PermissionCatalog(Set.of(PermissionCode.of("system:user:read"))), environment, transactions,
                new JdbcExternalGrantStore(jdbc, "vadmin", UuidStorage.STRING), directory);
    }
}
