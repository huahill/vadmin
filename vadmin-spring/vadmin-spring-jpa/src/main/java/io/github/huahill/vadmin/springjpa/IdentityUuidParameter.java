package io.github.huahill.vadmin.springjpa;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/// Binds an identity UUID for native SQL using the same rule as {@link IdentityUuidJavaType}.
public final class IdentityUuidParameter {
    private IdentityUuidParameter() {
    }

    public static Object value(EntityManager entityManager, UUID id) {
        Dialect dialect = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactoryImplementor.class)
                .getJdbcServices()
                .getDialect();
        return dialect instanceof MySQLDialect ? id.toString() : id;
    }
}
