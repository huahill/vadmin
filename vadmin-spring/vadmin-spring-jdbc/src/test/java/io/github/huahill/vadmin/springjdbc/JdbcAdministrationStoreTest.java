package io.github.huahill.vadmin.springjdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.huahill.vadmin.contracts.auth.PermissionCode;
import io.github.huahill.vadmin.contracts.navigation.PagedQuery;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class JdbcAdministrationStoreTest {
    @ParameterizedTest
    @EnumSource(UuidStorage.class)
    void normalizesTablePrefixesLikeTheExistingJpaAdapter(UuidStorage format) {
        var jdbc = database(format);
        var store = new JdbcAdministrationStore(jdbc, "CI_", format);
        assertThat(store.insertRole(UUID.randomUUID(), "operators")).isTrue();
        assertThat(store.roles()).hasSize(1);
        assertThatThrownBy(() -> new JdbcAdministrationStore(jdbc, "ci;DROP TABLE ci_users", format))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @EnumSource(UuidStorage.class)
    void managesUsersRolesPermissionsAndAuditWithBothUuidFormats(UuidStorage format) {
        JdbcTemplate jdbc = database(format);
        var store = new JdbcAdministrationStore(jdbc, "ci", format);
        var access = new JdbcAccessControlRepository(jdbc, "ci", format);
        UUID user = UUID.randomUUID();
        UUID role = UUID.randomUUID();
        UUID permission = UUID.randomUUID();
        assertThat(store.insertUser(user, "Operator", "hash")).isTrue();
        assertThat(store.insertUser(UUID.randomUUID(), "Operator", "hash")).isFalse();
        assertThat(store.insertRole(role, "administrator")).isTrue();
        assertThat(store.insertRole(UUID.randomUUID(), "administrator")).isFalse();
        jdbc.update("INSERT INTO ci_permissions VALUES (?, 'system:user:read', TRUE)", format.parameter(permission));
        store.replaceUserRoles(user, Set.of(role));
        assertThat(store.userRoleIds(user)).containsExactly(role);
        assertThat(store.isLastEnabledAdministrator(user)).isTrue();
        assertThat(access.findRoleByCode("administrator")).get().extracting(found -> found.id()).isEqualTo(role);
        assertThat(access.findPermissionByCode(PermissionCode.of("system:user:read"))).isPresent();
        access.grantPermission(role, permission);
        access.grantPermission(role, permission);
        access.incrementAuthVersionForRole(role);
        assertThat(store.roles().getFirst().permissionCodes()).containsExactly("system:user:read");
        assertThat(store.users(page(0, 1, Map.of("q", "operator"))).items()).singleElement()
                .satisfies(row -> assertThat(row.authVersion()).isEqualTo(2));
        assertThat(store.users(page(1, 1, Map.of())).items()).isEmpty();
        assertThat(store.roles(page(0, 1, Map.of())).total()).isEqualTo(1);
        assertThat(store.permissions(page(0, 1, Map.of())).items()).singleElement()
                .satisfies(row -> assertThat(row.code()).isEqualTo("system:user:read"));
        Instant occurred = Instant.parse("2026-10-19T01:00:00Z");
        jdbc.update("INSERT INTO ci_audit_entries VALUES (?, ?, 'create', 'user', ?, 'SUCCESS', ?, 'trace')",
                format.parameter(UUID.randomUUID()), format.parameter(user), user.toString(), Timestamp.from(occurred));
        assertThat(store.audit(page(0, 1, Map.of())).items()).singleElement()
                .satisfies(row -> assertThat(row.occurredAt()).isEqualTo(occurred));
        store.setEnabled(user, false);
        assertThat(store.isLastEnabledAdministrator(user)).isFalse();
        assertThat(store.users(page(0, 1, Map.of())).items().getFirst().enabled()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(UuidStorage.class)
    void rollsBackRoleReplacementAndVersionWhenTransactionFails(UuidStorage format) {
        JdbcTemplate jdbc = database(format);
        var store = new JdbcAdministrationStore(jdbc, "ci", format);
        UUID user = UUID.randomUUID();
        UUID role = UUID.randomUUID();
        store.insertUser(user, "operator", "hash");
        store.insertRole(role, "operator");
        store.replaceUserRoles(user, Set.of(role));
        var transaction = new TransactionTemplate(new DataSourceTransactionManager(jdbc.getDataSource()));
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            store.replaceUserRoles(user, Set.of());
            throw new IllegalStateException("audit failed");
        })).hasMessage("audit failed");
        assertThat(store.userRoleIds(user)).containsExactly(role);
        assertThat(store.users(page(0, 1, Map.of())).items().getFirst().authVersion()).isEqualTo(1);
    }

    private static PagedQuery page(int page, int size, Map<String, String> filters) {
        return new PagedQuery(page, size, "code", true, filters);
    }

    private static JdbcTemplate database(UuidStorage format) {
        var dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE="
                + (format == UuidStorage.STRING ? "MySQL" : "PostgreSQL") + ";DB_CLOSE_DELAY=-1");
        var jdbc = new JdbcTemplate(dataSource);
        String id = format == UuidStorage.STRING ? "CHAR(36)" : "UUID";
        jdbc.execute("CREATE TABLE ci_users (id " + id + " PRIMARY KEY, username VARCHAR(120) UNIQUE, "
                + "password_hash VARCHAR(255), enabled BOOLEAN, auth_version BIGINT)");
        jdbc.execute("CREATE TABLE ci_roles (id " + id + " PRIMARY KEY, code VARCHAR(120) UNIQUE)");
        jdbc.execute("CREATE TABLE ci_permissions (id " + id + " PRIMARY KEY, code VARCHAR(120), system_managed BOOLEAN)");
        jdbc.execute("CREATE TABLE ci_user_roles (user_id " + id + ", role_id " + id + ", PRIMARY KEY(user_id, role_id))");
        jdbc.execute("CREATE TABLE ci_role_permissions (role_id " + id + ", permission_id " + id
                + ", PRIMARY KEY(role_id, permission_id))");
        jdbc.execute("CREATE TABLE ci_audit_entries (id " + id + ", actor_user_id " + id + ", action_code VARCHAR(120), "
                + "target_type VARCHAR(120), target_id VARCHAR(255), outcome VARCHAR(16), occurred_at TIMESTAMP, correlation_id VARCHAR(64))");
        return jdbc;
    }
}
