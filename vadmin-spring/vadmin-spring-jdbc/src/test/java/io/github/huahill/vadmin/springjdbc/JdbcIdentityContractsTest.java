package io.github.huahill.vadmin.springjdbc;

import io.github.huahill.vadmin.contracts.audit.AuditEvent;
import io.github.huahill.vadmin.contracts.audit.AuditMetadataRedactor;
import io.github.huahill.vadmin.contracts.audit.AuditOutcome;
import io.github.huahill.vadmin.contracts.auth.LocalUserSession;
import io.github.huahill.vadmin.contracts.auth.PermissionCode;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcIdentityContractsTest {
    @ParameterizedTest
    @EnumSource(UuidStorage.class)
    void resolvesAccountPermissionsAndSessionFromConfiguredTables(UuidStorage format) {
        var jdbc = JdbcIamTestDatabase.create(format, "ci");
        var store = new JdbcAdministrationStore(jdbc, "ci", format);
        var lookup = new JdbcLocalUserAccountLookup(jdbc, "CI_", format);
        var sessions = new JdbcLocalUserSessionLookup(jdbc, "ci", format);
        UUID user = UUID.randomUUID();
        UUID role = UUID.randomUUID();
        UUID otherRole = UUID.randomUUID();
        UUID permission = UUID.randomUUID();
        store.insertUser(user, "operator", "hash");
        store.insertRole(role, "operators");
        store.insertRole(otherRole, "readers");
        store.replaceUserRoles(user, Set.of(role, otherRole));
        jdbc.update("INSERT INTO ci_permissions (id, code, system_managed) VALUES (?, 'system:user:read', TRUE)",
                format.parameter(permission));
        var access = new JdbcAccessControlRepository(jdbc, "ci", format);
        access.grantPermission(role, permission);
        access.grantPermission(otherRole, permission);

        var account = lookup.findByUsername("operator").orElseThrow();
        assertThat(account.userId()).isEqualTo(user);
        assertThat(account.passwordHash()).isEqualTo("hash");
        assertThat(account.permissions()).containsExactly(PermissionCode.of("system:user:read"));
        assertThat(lookup.findByUserId(user)).contains(account);
        assertThat(lookup.findByUsername("absent")).isEmpty();
        assertThat(lookup.findByUserId(UUID.randomUUID())).isEmpty();
        store.setEnabled(user, false);
        assertThat(sessions.findSessionByUserId(user)).contains(new LocalUserSession(user, false, 2));
        assertThat(sessions.findSessionByUserId(UUID.randomUUID())).isEmpty();
        store.replaceUserRoles(user, Set.of());
        assertThat(lookup.findByUserId(user).orElseThrow().permissions()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(UuidStorage.class)
    void bootstrapsAndRefreshesAdministratorWithoutChangingThePassword(UuidStorage format) {
        var jdbc = JdbcIamTestDatabase.create(format, "ci");
        var store = new JdbcIamStore(jdbc, "ci", format);
        var transaction = new TransactionTemplate(new DataSourceTransactionManager(jdbc.getDataSource()));
        transaction.executeWithoutResult(status -> {
            store.synchronizePermissions(Set.of("system:user:read"));
            assertThat(store.hasAnyUser()).isFalse();
            assertThat(store.bootstrapAdministrator("initial-hash", Set.of("system:user:read"))).isEqualTo(1);
        });
        transaction.executeWithoutResult(status -> {
            store.synchronizePermissions(Set.of("system:role:read"));
            assertThat(store.grantMissingPermissionsToAdministrator(Set.of("system:role:read"))).isEqualTo(1);
            assertThat(store.grantMissingPermissionsToAdministrator(Set.of("system:role:read"))).isZero();
        });
        var admin = new JdbcLocalUserAccountLookup(jdbc, "ci", format).findByUsername("admin").orElseThrow();
        assertThat(admin.passwordHash()).isEqualTo("initial-hash");
        assertThat(admin.authVersion()).isEqualTo(1);
        assertThat(admin.permissions()).containsExactlyInAnyOrder(
                PermissionCode.of("system:user:read"), PermissionCode.of("system:role:read"));
    }

    @ParameterizedTest
    @EnumSource(UuidStorage.class)
    void persistsEscapedRedactedAuditMetadataAndParticipatesInRollback(UuidStorage format) {
        var jdbc = JdbcIamTestDatabase.create(format, "ci");
        var sink = new JdbcAuditSink(jdbc, "CI_", format, new AuditMetadataRedactor());
        UUID actor = UUID.randomUUID();
        var event = new AuditEvent(actor, "system:user:create", "user", "operator", AuditOutcome.SUCCESS,
                Instant.parse("2026-10-19T00:00:00Z"), "trace",
                Map.of("description", "中文\n\"quoted\"\\value", "password", "must-not-persist"));
        sink.append(event);
        var row = jdbc.queryForMap("SELECT * FROM ci_audit_entries");
        assertThat(row.get("ACTOR_USER_ID").toString()).isEqualTo(actor.toString());
        assertThat(row.get("ACTION_CODE")).isEqualTo("system:user:create");
        assertThat(jdbc.queryForObject("SELECT metadata FROM ci_audit_entries", String.class)).contains("description", "quoted")
                .doesNotContain("password", "must-not-persist");
        var transaction = new TransactionTemplate(new DataSourceTransactionManager(jdbc.getDataSource()));
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            sink.append(event);
            throw new IllegalStateException("rollback");
        })).hasMessage("rollback");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ci_audit_entries", Long.class)).isEqualTo(1);
    }
}
