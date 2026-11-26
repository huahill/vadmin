package io.github.huahill.vadmin.springjdbc;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.huahill.vadmin.contracts.auth.PermissionCode;
import io.github.huahill.vadmin.contracts.auth.SubjectId;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class JdbcExternalGrantStoreTest {
    @Test
    void grantsRolesWithoutCreatingALocalAccountAndBumpsTheSessionVersion() {
        var jdbc = JdbcIamTestDatabase.create(UuidStorage.STRING, "vadmin");
        var store = new JdbcExternalGrantStore(jdbc, "vadmin", UuidStorage.STRING);
        var transactions = new TransactionTemplate(new DataSourceTransactionManager(jdbc.getDataSource()));
        var iam = new JdbcIamStore(jdbc, "vadmin", UuidStorage.STRING);
        jdbc.update("INSERT INTO vadmin_permissions (id, code, system_managed) VALUES (?, 'system:user:read', TRUE)",
                UUID.randomUUID().toString());
        transactions.executeWithoutResult(status -> iam.ensureAdministratorRole(Set.of("system:user:read")));
        UUID extra = UUID.randomUUID();
        String permissionId = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO vadmin_roles (id, code) VALUES (?, 'operators')", extra.toString());
        jdbc.update("INSERT INTO vadmin_permissions (id, code, system_managed) VALUES (?, 'environment:catalog:write', TRUE)",
                permissionId);
        jdbc.update("INSERT INTO vadmin_role_permissions (role_id, permission_id) VALUES (?, ?)",
                extra.toString(), permissionId);
        UUID administrator = UUID.fromString(jdbc.queryForObject(
                "SELECT id FROM vadmin_roles WHERE code = 'administrator'", String.class));

        SubjectId subject = SubjectId.of("company-user-1");
        assertThat(store.access(subject).permissions()).isEmpty();
        assertThat(store.access(subject).authVersion()).isZero();
        store.replaceRoles(subject, "zhangsan", Set.of(administrator, extra));

        assertThat(store.access(subject).permissions()).contains(
                PermissionCode.of("system:user:read"), PermissionCode.of("environment:catalog:write"));
        assertThat(store.access(subject).authVersion()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM vadmin_users", Long.class)).isZero();
        store.replaceRoles(subject, "zhangsan", Set.of(administrator, extra));
        assertThat(store.access(subject).authVersion()).isEqualTo(1);
        assertThat(store.isLastAdministrator(subject)).isTrue();
        assertThat(store.list()).singleElement().extracting(grant -> grant.label()).isEqualTo("zhangsan");
    }
}
