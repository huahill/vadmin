package io.github.huahill.vadmin.springjpa;

import java.util.Map;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;

/// 只在前缀不是默认 {@code vadmin} 时包装物理命名策略，把四张 JPA 表改写到配置的前缀下。
public final class AccessControlNamingCustomizer implements HibernatePropertiesCustomizer {
    private final LocalIamProperties properties;

    public AccessControlNamingCustomizer(LocalIamProperties properties) {
        this.properties = properties;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        var tables = properties.tables();
        if (tables.prefix().equals(AccessControlTables.of(AccessControlTables.DEFAULT_CONFIGURED_PREFIX).prefix())) {
            return;
        }
        var delegate = strategy(hibernateProperties.get(MappingSettings.PHYSICAL_NAMING_STRATEGY));
        hibernateProperties.put(MappingSettings.PHYSICAL_NAMING_STRATEGY, new PrefixedPhysicalNamingStrategy(delegate, tables));
    }

    private static PhysicalNamingStrategy strategy(Object configured) {
        if (configured instanceof PhysicalNamingStrategy namingStrategy) {
            return namingStrategy;
        }
        if (configured instanceof Class<?> type) {
            return instantiate(type);
        }
        if (configured instanceof String className) {
            try {
                return instantiate(Class.forName(className));
            } catch (ClassNotFoundException exception) {
                throw new IllegalStateException("Cannot load Hibernate physical naming strategy " + className, exception);
            }
        }
        return new PhysicalNamingStrategySnakeCaseImpl();
    }

    private static PhysicalNamingStrategy instantiate(Class<?> type) {
        if (!PhysicalNamingStrategy.class.isAssignableFrom(type)) {
            throw new IllegalStateException(type.getName() + " is not a PhysicalNamingStrategy");
        }
        try {
            return type.asSubclass(PhysicalNamingStrategy.class).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot create Hibernate physical naming strategy " + type.getName(), exception);
        }
    }

    private static final class PrefixedPhysicalNamingStrategy implements PhysicalNamingStrategy {
        private final PhysicalNamingStrategy delegate;
        private final Map<String, String> physicalNames;

        private PrefixedPhysicalNamingStrategy(PhysicalNamingStrategy delegate, AccessControlTables tables) {
            this.delegate = delegate;
            var defaults = AccessControlTables.of(AccessControlTables.DEFAULT_CONFIGURED_PREFIX);
            this.physicalNames = Map.of(
                    defaults.users(), tables.users(),
                    defaults.roles(), tables.roles(),
                    defaults.permissions(), tables.permissions(),
                    defaults.auditEntries(), tables.auditEntries());
        }

        @Override
        public Identifier toPhysicalCatalogName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
            return delegate.toPhysicalCatalogName(logicalName, jdbcEnvironment);
        }

        @Override
        public Identifier toPhysicalSchemaName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
            return delegate.toPhysicalSchemaName(logicalName, jdbcEnvironment);
        }

        @Override
        public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
            var physical = delegate.toPhysicalTableName(logicalName, jdbcEnvironment);
            if (physical == null) {
                return null;
            }
            var renamed = physicalNames.get(physical.getText());
            if (renamed == null) {
                return physical;
            }
            return Identifier.toIdentifier(renamed, physical.isQuoted());
        }

        @Override
        public Identifier toPhysicalSequenceName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
            return delegate.toPhysicalSequenceName(logicalName, jdbcEnvironment);
        }

        @Override
        public Identifier toPhysicalColumnName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
            return delegate.toPhysicalColumnName(logicalName, jdbcEnvironment);
        }

        @Override
        public Identifier toPhysicalTypeName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
            return delegate.toPhysicalTypeName(logicalName, jdbcEnvironment);
        }
    }
}
