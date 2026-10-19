package io.github.huahill.vadmin.springjdbc;

import io.github.huahill.vadmin.contracts.auth.LocalUserAccount;
import io.github.huahill.vadmin.contracts.auth.LocalUserAccountLookup;
import io.github.huahill.vadmin.contracts.auth.PermissionCode;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Loads local accounts and their effective permissions from the configured JDBC IAM schema. */
public final class JdbcLocalUserAccountLookup implements LocalUserAccountLookup {

    private static final String SELECT_ACCOUNT = """
            SELECT u.id, u.username, u.password_hash, u.enabled, u.auth_version, p.code AS permission_code
              FROM %s u
              LEFT JOIN %s ur ON ur.user_id = u.id
              LEFT JOIN %s rp ON rp.role_id = ur.role_id
              LEFT JOIN %s p ON p.id = rp.permission_id
             WHERE u.%s = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final JdbcIamTables tables;
    private final UuidStorage uuidStorage;

    public JdbcLocalUserAccountLookup(JdbcTemplate jdbcTemplate, String tablePrefix, UuidStorage uuidStorage) {
        this.jdbcTemplate = jdbcTemplate;
        this.tables = new JdbcIamTables(tablePrefix);
        this.uuidStorage = uuidStorage;
    }

    @Override
    public Optional<LocalUserAccount> findByUsername(String username) {
        return find("username", username);
    }

    @Override
    public Optional<LocalUserAccount> findByUserId(UUID userId) {
        return find("id", uuidStorage.parameter(userId));
    }

    private Optional<LocalUserAccount> find(String identityColumn, Object key) {
        List<LocalUserAccount> accounts = new ArrayList<>(1);
        Set<PermissionCode> permissions = new HashSet<>();
        jdbcTemplate.query(SELECT_ACCOUNT.formatted(tables.users(), tables.userRoles(), tables.rolePermissions(),
                tables.permissions(), identityColumn), resultSet -> {
            if (accounts.isEmpty()) {
                accounts.add(new LocalUserAccount(UUID.fromString(resultSet.getString("id")),
                        resultSet.getString("username"), resultSet.getString("password_hash"),
                        resultSet.getBoolean("enabled"), resultSet.getLong("auth_version"), Set.of()));
            }
            String permissionCode = resultSet.getString("permission_code");
            if (permissionCode != null) {
                permissions.add(PermissionCode.of(permissionCode));
            }
        }, key);
        if (accounts.isEmpty()) {
            return Optional.empty();
        }
        LocalUserAccount account = accounts.getFirst();
        return Optional.of(new LocalUserAccount(account.userId(), account.username(), account.passwordHash(),
                account.enabled(), account.authVersion(), permissions));
    }
}
