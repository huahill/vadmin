package io.github.huahill.vadmin.springjpa;

import org.hibernate.dialect.MySQLDialect;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.UUIDJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/// Identity UUID storage for VAdmin tables.
///
/// MySQL and MariaDB have no native UUID column in the host schema, so those
/// dialects bind the same Java UUID as VARCHAR(36). PostgreSQL keeps the native
/// uuid column created by the Flyway migration.
public final class IdentityUuidJavaType extends UUIDJavaType {
    @Override
    public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
        if (indicators.getDialect() instanceof MySQLDialect) {
            return indicators.getJdbcType(SqlTypes.VARCHAR);
        }
        return super.getRecommendedJdbcType(indicators);
    }
}
