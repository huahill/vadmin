package io.github.huahill.vadmin.administration;

import io.github.huahill.vadmin.contracts.navigation.PagedQuery;
import io.github.huahill.vadmin.contracts.navigation.PagedResult;
import io.github.huahill.vadmin.localiam.ConditionalOnVadminLocalIam;
import io.github.huahill.vadmin.springjpa.AccessControlTables;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnVadminLocalIam
public class AdministrationQueryService {
    private final JdbcTemplate jdbcTemplate;
    private final AccessControlTables tables;

    public AdministrationQueryService(JdbcTemplate jdbcTemplate, AccessControlTables tables) {
        this.jdbcTemplate = jdbcTemplate;
        this.tables = tables;
    }

    @Transactional(readOnly = true)
    public List<UserRow> users(String filter) {
        var value = "%" + (filter == null ? "" : filter.strip()) + "%";
        return jdbcTemplate.query("""
                select id, username, enabled, auth_version from %s
                where username ilike ? order by username limit 100
                """.formatted(tables.users()), (result, row) -> new UserRow(result.getObject("id", UUID.class), result.getString("username"),
                result.getBoolean("enabled"), result.getLong("auth_version")), value);
    }

    @Transactional(readOnly = true)
    public PagedResult<UserRow> users(PagedQuery query) {
        var value = "%" + query.filters().getOrDefault("q", "").strip() + "%";
        var total = jdbcTemplate.queryForObject(
                "select count(*) from %s where username ilike ?".formatted(tables.users()), Long.class, value);
        var items = jdbcTemplate.query("""
                        select id, username, enabled, auth_version from %s
                        where username ilike ? order by username limit ? offset ?
                        """.formatted(tables.users()), (result, row) -> new UserRow(result.getObject("id", UUID.class), result.getString("username"),
                        result.getBoolean("enabled"), result.getLong("auth_version")), value, query.pageSize(), offset(query));
        return new PagedResult<>(items, total);
    }

    @Transactional(readOnly = true)
    public List<RoleRow> roles() {
        return jdbcTemplate.query("""
                select role.id, role.code, count(grant_item.permission_id) as permission_count,
                       coalesce(string_agg(permission.code, ',' order by permission.code), '') as permission_codes
                from %s role left join %s grant_item on grant_item.role_id = role.id
                left join %s permission on permission.id = grant_item.permission_id
                group by role.id, role.code order by role.code
                """.formatted(tables.roles(), tables.rolePermissions(), tables.permissions()),
                (result, row) -> new RoleRow(result.getObject("id", UUID.class), result.getString("code"),
                result.getLong("permission_count"), permissionCodes(result.getString("permission_codes"))));
    }

    @Transactional(readOnly = true)
    public PagedResult<RoleRow> roles(PagedQuery query) {
        var total = jdbcTemplate.queryForObject("select count(*) from %s".formatted(tables.roles()), Long.class);
        var items = jdbcTemplate.query("""
                        select role.id, role.code, count(grant_item.permission_id) as permission_count,
                               coalesce(string_agg(permission.code, ',' order by permission.code), '') as permission_codes
                        from %s role left join %s grant_item on grant_item.role_id = role.id
                        left join %s permission on permission.id = grant_item.permission_id
                        group by role.id, role.code order by role.code limit ? offset ?
                        """.formatted(tables.roles(), tables.rolePermissions(), tables.permissions()),
                        (result, row) -> new RoleRow(result.getObject("id", UUID.class), result.getString("code"),
                        result.getLong("permission_count"), permissionCodes(result.getString("permission_codes"))),
                query.pageSize(), offset(query));
        return new PagedResult<>(items, total);
    }

    @Transactional(readOnly = true)
    public List<PermissionRow> permissions() {
        return jdbcTemplate.query("select code, system_managed from %s order by code".formatted(tables.permissions()),
                (result, row) -> new PermissionRow(result.getString("code"), result.getBoolean("system_managed")));
    }

    @Transactional(readOnly = true)
    public PagedResult<PermissionRow> permissions(PagedQuery query) {
        var total = jdbcTemplate.queryForObject("select count(*) from %s".formatted(tables.permissions()), Long.class);
        var items = jdbcTemplate.query(
                "select code, system_managed from %s order by code limit ? offset ?".formatted(tables.permissions()),
                (result, row) -> new PermissionRow(result.getString("code"), result.getBoolean("system_managed")),
                query.pageSize(), offset(query));
        return new PagedResult<>(items, total);
    }

    @Transactional(readOnly = true)
    public List<AuditRow> audit() {
        return jdbcTemplate.query("""
                select occurred_at, action_code, target_type, target_id, outcome, correlation_id
                from %s order by occurred_at desc limit 100
                """.formatted(tables.auditEntries()), (result, row) -> auditRow(result));
    }

    @Transactional(readOnly = true)
    public PagedResult<AuditRow> audit(PagedQuery query) {
        var total = jdbcTemplate.queryForObject("select count(*) from %s".formatted(tables.auditEntries()), Long.class);
        var items = jdbcTemplate.query("""
                select occurred_at, action_code, target_type, target_id, outcome, correlation_id
                from %s order by occurred_at desc limit ? offset ?
                """.formatted(tables.auditEntries()), (result, row) -> auditRow(result), query.pageSize(), offset(query));
        return new PagedResult<>(items, total);
    }

    private int offset(PagedQuery query) {
        return Math.multiplyExact(query.page(), query.pageSize());
    }

    private static List<String> permissionCodes(String commaSeparatedCodes) {
        return commaSeparatedCodes.isBlank() ? List.of() : List.of(commaSeparatedCodes.split(","));
    }

    private static AuditRow auditRow(java.sql.ResultSet result) throws java.sql.SQLException {
        return new AuditRow(result.getObject("occurred_at", OffsetDateTime.class).toInstant(),
                result.getString("action_code"), result.getString("target_type"), result.getString("target_id"),
                result.getString("outcome"), result.getString("correlation_id"));
    }

    public record UserRow(UUID id, String username, boolean enabled, long authVersion) { }
    public record RoleRow(UUID id, String code, long permissionCount, List<String> permissionCodes) {
        public RoleRow {
            permissionCodes = List.copyOf(permissionCodes);
        }
    }
    public record PermissionRow(String code, boolean systemManaged) { }
    public record AuditRow(Instant occurredAt, String action, String targetType, String targetId, String outcome,
                           String correlationId) { }
}
