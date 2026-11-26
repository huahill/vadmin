package io.github.huahill.vadmin.springjdbc;

import io.github.huahill.vadmin.contracts.auth.PermissionCode;
import io.github.huahill.vadmin.contracts.auth.SubjectId;
import io.github.huahill.vadmin.platform.access.ExternalGrantStore;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** JDBC grants of existing VAdmin roles to external subjects. */
public final class JdbcExternalGrantStore implements ExternalGrantStore {
    private final JdbcTemplate jdbc;
    private final JdbcIamTables tables;
    private final UuidStorage uuidStorage;

    public JdbcExternalGrantStore(JdbcTemplate jdbc, String tablePrefix, UuidStorage uuidStorage) {
        this.jdbc = jdbc;
        this.tables = new JdbcIamTables(tablePrefix);
        this.uuidStorage = uuidStorage;
    }

    @Override
    public ExternalAccess access(SubjectId subject) {
        List<PermissionCode> permissions = jdbc.query(
                "SELECT p.code FROM " + tables.externalGrants() + " g "
                        + "JOIN " + tables.rolePermissions() + " rp ON rp.role_id = g.role_id "
                        + "JOIN " + tables.permissions() + " p ON p.id = rp.permission_id "
                        + "WHERE g.subject = ? ORDER BY p.code",
                (resultSet, row) -> PermissionCode.of(resultSet.getString("code")),
                subject.value());
        List<Long> versions = jdbc.query(
                "SELECT auth_version FROM " + tables.externalSubjects() + " WHERE subject = ?",
                (resultSet, row) -> resultSet.getLong("auth_version"),
                subject.value());
        long version = versions.isEmpty() ? 0L : versions.getFirst();
        return new ExternalAccess(Set.copyOf(permissions), version);
    }

    @Override
    public Set<UUID> roleIds(SubjectId subject) {
        return new LinkedHashSet<>(jdbc.query(
                "SELECT role_id FROM " + tables.externalGrants() + " WHERE subject = ?",
                (resultSet, row) -> UUID.fromString(resultSet.getString("role_id")),
                subject.value()));
    }

    @Override
    public boolean isLastAdministrator(SubjectId subject) {
        if (!hasRole(subject, "administrator")) {
            return false;
        }
        Long count = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT g.subject) FROM " + tables.externalGrants() + " g "
                        + "JOIN " + tables.roles() + " r ON r.id = g.role_id WHERE r.code = 'administrator'",
                Long.class);
        return count != null && count == 1L;
    }

    @Override
    public boolean includesAdministrator(Set<UUID> roleIds) {
        if (roleIds.isEmpty()) {
            return false;
        }
        String placeholders = String.join(",", roleIds.stream().map(id -> "?").toList());
        List<Object> arguments = new ArrayList<>();
        roleIds.forEach(id -> arguments.add(uuidStorage.parameter(id)));
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + tables.roles() + " WHERE code = 'administrator' AND id IN (" + placeholders + ")",
                Long.class, arguments.toArray());
        return count != null && count > 0L;
    }

    @Override
    public boolean hasAdministrator() {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + tables.externalGrants() + " g JOIN " + tables.roles()
                        + " r ON r.id = g.role_id WHERE r.code = 'administrator'",
                Long.class);
        return count != null && count > 0L;
    }

    @Override
    public void replaceRoles(SubjectId subject, String label, Set<UUID> roleIds) {
        Set<UUID> current = roleIds(subject);
        upsertSubject(subject, label, current.equals(roleIds));
        if (current.equals(roleIds)) {
            return;
        }
        jdbc.update("DELETE FROM " + tables.externalGrants() + " WHERE subject = ?", subject.value());
        for (UUID roleId : roleIds) {
            jdbc.update("INSERT INTO " + tables.externalGrants() + " (subject, role_id) VALUES (?, ?)",
                    subject.value(), uuidStorage.parameter(roleId));
        }
        jdbc.update("UPDATE " + tables.externalSubjects() + " SET auth_version = auth_version + 1 WHERE subject = ?",
                subject.value());
    }

    @Override
    public void ensureAdministrator(SubjectId subject) {
        List<String> roleIds = jdbc.queryForList(
                "SELECT id FROM " + tables.roles() + " WHERE code = 'administrator'", String.class);
        if (roleIds.isEmpty()) {
            throw new IllegalStateException("administrator role must exist before granting an external subject");
        }
        UUID roleId = UUID.fromString(roleIds.getFirst());
        upsertSubject(subject, subject.value(), true);
        int inserted = jdbc.update(
                "INSERT INTO " + tables.externalGrants() + " (subject, role_id) "
                        + "SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM " + tables.externalGrants()
                        + " WHERE subject = ? AND role_id = ?)",
                subject.value(), uuidStorage.parameter(roleId), subject.value(), uuidStorage.parameter(roleId));
        if (inserted > 0) {
            jdbc.update("UPDATE " + tables.externalSubjects() + " SET auth_version = auth_version + 1 WHERE subject = ?",
                    subject.value());
        }
    }

    @Override
    public List<ExternalGrant> list() {
        record Row(String subject, String label, long authVersion, String roleCode) { }
        List<Row> rows = jdbc.query(
                "SELECT s.subject, s.label, s.auth_version, r.code AS role_code FROM " + tables.externalSubjects()
                        + " s LEFT JOIN " + tables.externalGrants() + " g ON g.subject = s.subject "
                        + "LEFT JOIN " + tables.roles() + " r ON r.id = g.role_id ORDER BY s.label, r.code",
                (resultSet, row) -> new Row(resultSet.getString("subject"), resultSet.getString("label"),
                        resultSet.getLong("auth_version"), resultSet.getString("role_code")));
        var grouped = new LinkedHashMap<String, ExternalGrant>();
        for (Row row : rows) {
            ExternalGrant existing = grouped.get(row.subject());
            List<String> codes = new ArrayList<>();
            if (existing != null) {
                codes.addAll(existing.roleCodes());
            }
            if (row.roleCode() != null) {
                codes.add(row.roleCode());
            }
            grouped.put(row.subject(), new ExternalGrant(SubjectId.of(row.subject()), row.label(), row.authVersion(), codes));
        }
        return List.copyOf(grouped.values());
    }

    private void upsertSubject(SubjectId subject, String label, boolean keepVersion) {
        String safeLabel = label == null || label.isBlank() ? subject.value() : label.strip();
        if (safeLabel.length() > 255) {
            safeLabel = safeLabel.substring(0, 255);
        }
        int updated = jdbc.update("UPDATE " + tables.externalSubjects() + " SET label = ? WHERE subject = ?",
                safeLabel, subject.value());
        if (updated == 0) {
            jdbc.update("INSERT INTO " + tables.externalSubjects() + " (subject, label, auth_version) VALUES (?, ?, 0)",
                    subject.value(), safeLabel);
        } else if (!keepVersion) {
            // version is incremented by the caller after the grant rows change
        }
    }

    private boolean hasRole(SubjectId subject, String code) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + tables.externalGrants() + " g JOIN " + tables.roles()
                        + " r ON r.id = g.role_id WHERE g.subject = ? AND r.code = ?",
                Long.class, subject.value(), code);
        return count != null && count > 0L;
    }
}
