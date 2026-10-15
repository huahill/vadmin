package io.github.huahill.vadmin.springjpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class AccessControlMigrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Test
    void flywayCreatesPrefixedAccessControlTables() throws Exception {
        var flyway = flyway("public", "vadmin_").load();

        flyway.migrate();
        var repeatMigration = flyway.migrate();

        assertThat(repeatMigration.migrationsExecuted).isZero();
        assertThat(tableNames("public")).containsExactly(
                "vadmin_audit_entries",
                "vadmin_permissions",
                "vadmin_role_permissions",
                "vadmin_roles",
                "vadmin_user_roles",
                "vadmin_users");
    }

    @Test
    void flywayRenamesTablesCreatedByTheOriginalMigration() throws Exception {
        flyway("legacy", "vadmin_").target("1").load().migrate();

        assertThat(tableNames("legacy")).contains("users", "role_permissions", "audit_entries");

        flyway("legacy", "vadmin_").load().migrate();

        assertThat(tableNames("legacy")).contains(
                "vadmin_users", "vadmin_role_permissions", "vadmin_audit_entries")
                .doesNotContain("users", "role_permissions", "audit_entries");
    }

    @Test
    void flywayHonorsANonDefaultPrefixInItsOwnSchema() throws Exception {
        flyway("ci", "ci_").load().migrate();

        assertThat(tableNames("ci")).containsExactly(
                "ci_audit_entries",
                "ci_permissions",
                "ci_role_permissions",
                "ci_roles",
                "ci_user_roles",
                "ci_users");
    }

    private static org.flywaydb.core.api.configuration.FluentConfiguration flyway(String schema, String prefix) {
        return Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .schemas(schema)
                .defaultSchema(schema)
                .createSchemas(true)
                .placeholders(Map.of("vadminTablePrefix", prefix));
    }

    private static List<String> tableNames(String schema) throws Exception {
        try (var connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                var statement = connection.prepareStatement("""
                        select table_name
                        from information_schema.tables
                        where table_schema = ?
                          and table_type = 'BASE TABLE'
                          and table_name not like 'flyway_%'
                        order by table_name
                        """)) {
            statement.setString(1, schema);
            try (var result = statement.executeQuery()) {
                var names = new ArrayList<String>();
                while (result.next()) {
                    names.add(result.getString(1));
                }
                return names;
            }
        }
    }
}
