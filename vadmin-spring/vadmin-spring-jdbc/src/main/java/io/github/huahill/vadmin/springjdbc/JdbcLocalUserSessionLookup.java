package io.github.huahill.vadmin.springjdbc;

import io.github.huahill.vadmin.contracts.auth.LocalUserSession;
import io.github.huahill.vadmin.contracts.auth.LocalUserSessionLookup;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;
import java.util.UUID;

/** Resolves the current session state (enabled flag and auth version) of a VAdmin local account. */
public final class JdbcLocalUserSessionLookup implements LocalUserSessionLookup {

    private final JdbcTemplate jdbcTemplate;
    private final JdbcIamTables tables;
    private final UuidStorage uuidStorage;

    public JdbcLocalUserSessionLookup(JdbcTemplate jdbcTemplate, String tablePrefix, UuidStorage uuidStorage) {
        this.jdbcTemplate = jdbcTemplate;
        this.tables = new JdbcIamTables(tablePrefix);
        this.uuidStorage = uuidStorage;
    }

    @Override
    public Optional<LocalUserSession> findSessionByUserId(UUID userId) {
        var sessions = jdbcTemplate.query(
                "SELECT id, enabled, auth_version FROM " + tables.users() + " WHERE id = ?",
                (resultSet, row) -> new LocalUserSession(UUID.fromString(resultSet.getString("id")),
                        resultSet.getBoolean("enabled"), resultSet.getLong("auth_version")),
                uuidStorage.parameter(userId));
        return sessions.stream().findFirst();
    }
}
