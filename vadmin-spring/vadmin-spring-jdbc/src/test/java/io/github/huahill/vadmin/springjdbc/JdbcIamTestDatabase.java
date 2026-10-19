package io.github.huahill.vadmin.springjdbc;

import java.util.UUID;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

final class JdbcIamTestDatabase {
    static JdbcTemplate create(UuidStorage format, String prefix) {
        var dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE="
                + (format == UuidStorage.STRING ? "MySQL" : "PostgreSQL") + ";DB_CLOSE_DELAY=-1");
        var jdbc = new JdbcTemplate(dataSource);
        var tables = new JdbcIamTables(prefix);
        String id = format == UuidStorage.STRING ? "CHAR(36)" : "UUID";
        jdbc.execute("CREATE TABLE " + tables.users() + " (id " + id + " PRIMARY KEY, username VARCHAR(120) UNIQUE, "
                + "password_hash VARCHAR(255), enabled BOOLEAN, auth_version BIGINT)");
        jdbc.execute("CREATE TABLE " + tables.roles() + " (id " + id + " PRIMARY KEY, code VARCHAR(120) UNIQUE)");
        jdbc.execute("CREATE TABLE " + tables.permissions() + " (id " + id + " PRIMARY KEY, code VARCHAR(120) UNIQUE, "
                + "system_managed BOOLEAN, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE " + tables.userRoles() + " (user_id " + id + ", role_id " + id
                + ", PRIMARY KEY(user_id, role_id))");
        jdbc.execute("CREATE TABLE " + tables.rolePermissions() + " (role_id " + id + ", permission_id " + id
                + ", PRIMARY KEY(role_id, permission_id))");
        jdbc.execute("CREATE TABLE " + tables.audit() + " (id " + id + ", actor_user_id " + id + ", action_code VARCHAR(120), "
                + "target_type VARCHAR(120), target_id VARCHAR(255), outcome VARCHAR(16), occurred_at TIMESTAMP, "
                + "correlation_id VARCHAR(64), metadata " + (format == UuidStorage.NATIVE ? "JSONB" : "TEXT") + ")");
        return jdbc;
    }
}
