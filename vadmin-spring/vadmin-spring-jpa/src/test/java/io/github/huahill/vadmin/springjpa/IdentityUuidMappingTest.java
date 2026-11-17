package io.github.huahill.vadmin.springjpa;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.huahill.vadmin.springjpa.access.JpaPermissionEntity;
import io.github.huahill.vadmin.springjpa.access.JpaRoleEntity;
import io.github.huahill.vadmin.springjpa.access.JpaUserAccountEntity;
import io.github.huahill.vadmin.springjpa.audit.JpaAuditEntryEntity;
import java.sql.Types;
import java.util.List;
import java.util.function.Consumer;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.junit.jupiter.api.Test;

class IdentityUuidMappingTest {
    @Test
    void storesIdentityUuidsAsVarchar36OnMysqlAndNativeUuidOnPostgres() {
        assertCharacterUuid(MySQLDialect.class);
        assertCharacterUuid(MariaDBDialect.class);
        assertNativeUuid(PostgreSQLDialect.class);
    }

    private static void assertCharacterUuid(Class<? extends Dialect> dialect) {
        withMetadata(dialect, metadata -> {
            for (Column column : identityColumns(metadata)) {
                assertThat(column.getSqlType(metadata)).isEqualTo("varchar(36)");
                assertThat(column.getSqlTypeCode(metadata)).isEqualTo(Types.VARCHAR);
                assertThat(column.getLength()).isEqualTo(36L);
            }
        });
    }

    private static void assertNativeUuid(Class<? extends Dialect> dialect) {
        withMetadata(dialect, metadata -> {
            for (Column column : identityColumns(metadata)) {
                assertThat(column.getSqlType(metadata)).isEqualTo("uuid");
                assertThat(column.getSqlTypeCode(metadata)).isNotEqualTo(Types.VARCHAR);
            }
        });
    }

    private static List<Column> identityColumns(Metadata metadata) {
        return List.of(
                column(metadata, JpaUserAccountEntity.class, null),
                column(metadata, JpaRoleEntity.class, null),
                column(metadata, JpaPermissionEntity.class, null),
                column(metadata, JpaAuditEntryEntity.class, null),
                column(metadata, JpaAuditEntryEntity.class, "actorUserId"));
    }

    private static void withMetadata(Class<? extends Dialect> dialect, Consumer<Metadata> assertion) {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", dialect.getName())
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", "false")
                .build();
        try {
            Metadata metadata = new MetadataSources(registry)
                    .addAnnotatedClass(JpaUserAccountEntity.class)
                    .addAnnotatedClass(JpaRoleEntity.class)
                    .addAnnotatedClass(JpaPermissionEntity.class)
                    .addAnnotatedClass(JpaAuditEntryEntity.class)
                    .getMetadataBuilder()
                    .build();
            assertion.accept(metadata);
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    private static Column column(Metadata metadata, Class<?> entity, String property) {
        PersistentClass binding = metadata.getEntityBinding(entity.getName());
        return property == null
                ? (Column) binding.getIdentifier().getColumns().get(0)
                : (Column) binding.getProperty(property).getValue().getColumns().get(0);
    }
}
