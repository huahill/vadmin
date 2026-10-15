package io.github.huahill.vadmin.springjpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AccessControlTablesTest {
    @Test
    void usesVadminWhenTheConfiguredPrefixIsMissing() {
        var tables = AccessControlTables.of(null);

        assertThat(tables.prefix()).isEqualTo("vadmin_");
        assertThat(tables.users()).isEqualTo("vadmin_users");
        assertThat(tables.roles()).isEqualTo("vadmin_roles");
        assertThat(tables.permissions()).isEqualTo("vadmin_permissions");
        assertThat(tables.userRoles()).isEqualTo("vadmin_user_roles");
        assertThat(tables.rolePermissions()).isEqualTo("vadmin_role_permissions");
        assertThat(tables.auditEntries()).isEqualTo("vadmin_audit_entries");
    }

    @Test
    void lowercasesAndAppendsOneTrailingUnderscore() {
        assertThat(AccessControlTables.of("vadmin").prefix()).isEqualTo("vadmin_");
        assertThat(AccessControlTables.of("vadmin_").prefix()).isEqualTo("vadmin_");
        assertThat(AccessControlTables.of("CI").users()).isEqualTo("ci_users");
    }

    @Test
    void rejectsBlankOrNonIdentifierPrefixes() {
        assertThatThrownBy(() -> AccessControlTables.of(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AccessControlTables.of(" "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AccessControlTables.of(" ci"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AccessControlTables.of("1ci"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AccessControlTables.of("ci-app"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAPrefixThatCannotFitInAPostgreSqlIdentifier() {
        var accepted = "a".repeat(46);
        var rejected = "a".repeat(47);

        assertThat(AccessControlTables.of(accepted).rolePermissions()).hasSize(63);
        assertThatThrownBy(() -> AccessControlTables.of(rejected))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
