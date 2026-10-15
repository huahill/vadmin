package io.github.huahill.vadmin.springjpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.junit.jupiter.api.Test;

class AccessControlSchemaCustomizationTest {
    @Test
    void defaultsTheConfiguredPrefixToVadmin() {
        var properties = new LocalIamProperties();

        assertThat(properties.getTablePrefix()).isEqualTo("vadmin");
        assertThat(properties.tables().users()).isEqualTo("vadmin_users");
        assertThat(properties.tables().auditEntries()).isEqualTo("vadmin_audit_entries");
    }

    @Test
    void mergesTheDefaultPrefixIntoExistingFlywayPlaceholders() {
        var configuration = Flyway.configure().placeholders(Map.of("host", "kept"));

        new AccessControlFlywayCustomizer(new LocalIamProperties()).customize(configuration);

        assertThat(configuration.getPlaceholders())
                .containsEntry("host", "kept")
                .containsEntry("vadminTablePrefix", "vadmin_");
    }

    @Test
    void mergesACustomPrefixWithoutReplacingExistingFlywayPlaceholders() {
        var properties = new LocalIamProperties();
        properties.setTablePrefix("CI");
        var configuration = Flyway.configure().placeholders(Map.of("host", "kept"));

        new AccessControlFlywayCustomizer(properties).customize(configuration);

        assertThat(configuration.getPlaceholders())
                .containsEntry("host", "kept")
                .containsEntry("vadminTablePrefix", "ci_");
    }

    @Test
    void leavesTheNamingStrategyUntouchedForTheDefaultPrefix() {
        var hibernateProperties = new HashMap<String, Object>();
        hibernateProperties.put(
                "hibernate.physical_naming_strategy", PhysicalNamingStrategySnakeCaseImpl.class.getName());

        new AccessControlNamingCustomizer(new LocalIamProperties()).customize(hibernateProperties);

        assertThat(hibernateProperties).containsEntry(
                "hibernate.physical_naming_strategy", PhysicalNamingStrategySnakeCaseImpl.class.getName());
    }

    @Test
    void wrapsAStrategyClassNameAndRewritesOnlyTheDefaultTables() {
        var hibernateProperties = new HashMap<String, Object>();
        hibernateProperties.put(
                "hibernate.physical_naming_strategy", PhysicalNamingStrategySnakeCaseImpl.class.getName());
        var properties = new LocalIamProperties();
        properties.setTablePrefix("ci");

        new AccessControlNamingCustomizer(properties).customize(hibernateProperties);

        assertRewrittenTables((PhysicalNamingStrategy) hibernateProperties.get("hibernate.physical_naming_strategy"));
    }

    @Test
    void wrapsAStrategyClassAndRewritesOnlyTheDefaultTables() {
        var hibernateProperties = new HashMap<String, Object>();
        hibernateProperties.put("hibernate.physical_naming_strategy", PhysicalNamingStrategySnakeCaseImpl.class);
        var properties = new LocalIamProperties();
        properties.setTablePrefix("ci");

        new AccessControlNamingCustomizer(properties).customize(hibernateProperties);

        assertRewrittenTables((PhysicalNamingStrategy) hibernateProperties.get("hibernate.physical_naming_strategy"));
    }

    @Test
    void wrapsAStrategyInstanceWithoutRewritingTypeNames() {
        var hibernateProperties = new HashMap<String, Object>();
        hibernateProperties.put("hibernate.physical_naming_strategy", new DistinctTypeNamingStrategy());
        var properties = new LocalIamProperties();
        properties.setTablePrefix("ci");

        new AccessControlNamingCustomizer(properties).customize(hibernateProperties);

        var strategy = (PhysicalNamingStrategy) hibernateProperties.get("hibernate.physical_naming_strategy");
        assertThat(table(strategy, "vadmin_users")).isEqualTo("ci_users");
        assertThat(table(strategy, "host_table")).isEqualTo("host_table");
        assertThat(strategy.toPhysicalTypeName(Identifier.toIdentifier("vadmin_users"), null).getText())
                .isEqualTo("type_vadmin_users");
    }

    private static void assertRewrittenTables(PhysicalNamingStrategy strategy) {
        assertThat(table(strategy, "vadmin_users")).isEqualTo("ci_users");
        assertThat(table(strategy, "vadmin_roles")).isEqualTo("ci_roles");
        assertThat(table(strategy, "vadmin_permissions")).isEqualTo("ci_permissions");
        assertThat(table(strategy, "vadmin_audit_entries")).isEqualTo("ci_audit_entries");
        assertThat(table(strategy, "host_table")).isEqualTo("host_table");
        assertThat(strategy.toPhysicalTypeName(Identifier.toIdentifier("vadmin_users"), null).getText())
                .isEqualTo("vadmin_users");
    }

    private static String table(PhysicalNamingStrategy strategy, String logicalName) {
        return strategy.toPhysicalTableName(Identifier.toIdentifier(logicalName), null).getText();
    }

    private static final class DistinctTypeNamingStrategy implements PhysicalNamingStrategy {
        @Override
        public Identifier toPhysicalCatalogName(Identifier name, JdbcEnvironment jdbcEnvironment) {
            return name;
        }

        @Override
        public Identifier toPhysicalSchemaName(Identifier name, JdbcEnvironment jdbcEnvironment) {
            return name;
        }

        @Override
        public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
            return name;
        }

        @Override
        public Identifier toPhysicalSequenceName(Identifier name, JdbcEnvironment jdbcEnvironment) {
            return name;
        }

        @Override
        public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment jdbcEnvironment) {
            return name;
        }

        @Override
        public Identifier toPhysicalTypeName(Identifier name, JdbcEnvironment jdbcEnvironment) {
            return Identifier.toIdentifier("type_" + name.getText());
        }
    }
}
