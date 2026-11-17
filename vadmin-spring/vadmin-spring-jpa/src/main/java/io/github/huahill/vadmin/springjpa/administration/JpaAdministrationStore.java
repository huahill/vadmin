package io.github.huahill.vadmin.springjpa.administration;

import io.github.huahill.vadmin.contracts.navigation.PagedQuery;
import io.github.huahill.vadmin.contracts.navigation.PagedResult;
import io.github.huahill.vadmin.platform.administration.AdministrationStore;
import io.github.huahill.vadmin.springjpa.AccessControlTables;
import io.github.huahill.vadmin.springjpa.access.JpaPermissionEntity;
import io.github.huahill.vadmin.springjpa.access.JpaRoleEntity;
import io.github.huahill.vadmin.springjpa.access.JpaUserAccountEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/// System-administration storage for the host's existing character UUID tables.
public final class JpaAdministrationStore implements AdministrationStore {
    private final EntityManager entityManager;
    private final AccessControlTables tables;

    public JpaAdministrationStore(EntityManager entityManager) {
        this(entityManager, AccessControlTables.of(null));
    }

    public JpaAdministrationStore(EntityManager entityManager, AccessControlTables tables) {
        this.entityManager = Objects.requireNonNull(entityManager, "EntityManager must not be null.");
        this.tables = Objects.requireNonNull(tables, "Access tables must not be null.");
    }

    @Override
    public PagedResult<UserRow> users(PagedQuery query) {
        String filter = "%" + query.filters().getOrDefault("q", "").strip().toLowerCase(Locale.ROOT) + "%";
        CriteriaBuilder criteria = entityManager.getCriteriaBuilder();
        var count = criteria.createQuery(Long.class);
        Root<JpaUserAccountEntity> counted = count.from(JpaUserAccountEntity.class);
        count.select(criteria.count(counted)).where(usernameMatches(criteria, counted, filter));
        long total = entityManager.createQuery(count).getSingleResult();

        var selection = criteria.createQuery(JpaUserAccountEntity.class);
        Root<JpaUserAccountEntity> root = selection.from(JpaUserAccountEntity.class);
        selection.select(root).where(usernameMatches(criteria, root, filter))
                .orderBy(userOrder(criteria, root, query));
        List<UserRow> items = entityManager.createQuery(selection)
                .setFirstResult(offset(query))
                .setMaxResults(query.pageSize())
                .getResultList()
                .stream()
                .map(user -> new UserRow(user.id(), user.username(), user.enabled(), user.authVersion()))
                .toList();
        return new PagedResult<>(items, total);
    }

    @Override
    public List<RoleRow> roles() {
        List<JpaRoleEntity> rows = entityManager.createQuery(
                "select role from JpaRoleEntity role order by role.code, role.id", JpaRoleEntity.class)
                .getResultList();
        return withPermissions(rows.stream().map(role -> new RoleRow(role.id(), role.code(), 0, List.of())).toList());
    }

    @Override
    public PagedResult<RoleRow> roles(PagedQuery query) {
        long total = count(JpaRoleEntity.class);
        List<JpaRoleEntity> rows = entityManager.createQuery(
                "select role from JpaRoleEntity role order by role.code " + direction(query) + ", role.id",
                JpaRoleEntity.class)
                .setFirstResult(offset(query))
                .setMaxResults(query.pageSize())
                .getResultList();
        return new PagedResult<>(withPermissions(rows.stream()
                .map(role -> new RoleRow(role.id(), role.code(), 0, List.of())).toList()), total);
    }

    @Override
    public PagedResult<PermissionRow> permissions(PagedQuery query) {
        List<PermissionRow> rows = entityManager.createQuery(
                "select permission from JpaPermissionEntity permission order by permission.code " + direction(query),
                JpaPermissionEntity.class)
                .setFirstResult(offset(query))
                .setMaxResults(query.pageSize())
                .getResultList()
                .stream()
                .map(permission -> new PermissionRow(permission.code(), permission.systemManaged()))
                .toList();
        return new PagedResult<>(rows, count(JpaPermissionEntity.class));
    }

    @Override
    public PagedResult<AuditRow> audit(PagedQuery query) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                select occurred_at, action_code, target_type, target_id, outcome, correlation_id
                from %s order by occurred_at desc, id
                """.formatted(tables.auditEntries()))
                .setFirstResult(offset(query))
                .setMaxResults(query.pageSize())
                .getResultList();
        List<AuditRow> items = rows.stream().map(JpaAdministrationStore::auditRow).toList();
        Number total = (Number) entityManager.createNativeQuery("select count(*) from " + tables.auditEntries())
                .getSingleResult();
        return new PagedResult<>(items, total.longValue());
    }

    @Override
    public Set<UUID> userRoleIds(UUID userId) {
        @SuppressWarnings("unchecked")
        List<Object> rows = entityManager.createNativeQuery(
                "select role_id from " + tables.userRoles() + " where user_id = :userId")
                .setParameter("userId", userId.toString())
                .getResultList();
        return rows.stream().map(JpaAdministrationStore::uuid).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean userExists(UUID userId) {
        Number count = (Number) entityManager.createNativeQuery(
                "select count(*) from " + tables.users() + " where id = :id")
                .setParameter("id", userId.toString())
                .getSingleResult();
        return count.longValue() > 0;
    }

    @Override
    public boolean insertUser(UUID id, String username, String passwordHash) {
        return insert("""
                insert into %s (id, username, password_hash, enabled, auth_version)
                values (:id, :username, :passwordHash, true, 0)
                """.formatted(tables.users()), Map.of(
                "id", id.toString(), "username", username, "passwordHash", passwordHash));
    }

    @Override
    public boolean insertRole(UUID id, String code) {
        return insert("insert into " + tables.roles() + " (id, code) values (:id, :code)",
                Map.of("id", id.toString(), "code", code));
    }

    @Override
    public void setEnabled(UUID userId, boolean enabled) {
        entityManager.createNativeQuery("""
                update %s set enabled = :enabled, auth_version = auth_version + 1 where id = :id
                """.formatted(tables.users()))
                .setParameter("enabled", enabled)
                .setParameter("id", userId.toString())
                .executeUpdate();
    }

    @Override
    public void replaceUserRoles(UUID userId, Set<UUID> roleIds) {
        entityManager.createNativeQuery("delete from " + tables.userRoles() + " where user_id = :userId")
                .setParameter("userId", userId.toString())
                .executeUpdate();
        for (UUID roleId : roleIds) {
            entityManager.createNativeQuery(
                    "insert into " + tables.userRoles() + " (user_id, role_id) values (:userId, :roleId)")
                    .setParameter("userId", userId.toString())
                    .setParameter("roleId", roleId.toString())
                    .executeUpdate();
        }
        entityManager.createNativeQuery(
                "update " + tables.users() + " set auth_version = auth_version + 1 where id = :id")
                .setParameter("id", userId.toString())
                .executeUpdate();
    }

    @Override
    public void lockAdministratorRole() {
        entityManager.createQuery("select role from JpaRoleEntity role where role.code = 'administrator'",
                        JpaRoleEntity.class)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
    }

    @Override
    public boolean isLastEnabledAdministrator(UUID userId) {
        @SuppressWarnings("unchecked")
        List<Object> rows = entityManager.createNativeQuery("""
                select u.id from %s u
                join %s membership on membership.user_id = u.id
                join %s role on role.id = membership.role_id
                where role.code = 'administrator' and u.enabled = true
                """.formatted(tables.users(), tables.userRoles(), tables.roles()))
                .getResultList();
        return rows.size() == 1 && userId.equals(uuid(rows.getFirst()));
    }

    private List<RoleRow> withPermissions(List<RoleRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<String>> codes = new HashMap<>();
        @SuppressWarnings("unchecked")
        List<Object[]> grants = entityManager.createNativeQuery("""
                select membership.role_id, permission.code
                from %s membership
                join %s permission on permission.id = membership.permission_id
                where membership.role_id in (:roleIds)
                order by permission.code
                """.formatted(tables.rolePermissions(), tables.permissions()))
                .setParameter("roleIds", rows.stream().map(row -> row.id().toString()).toList())
                .getResultList();
        for (Object[] grant : grants) {
            codes.computeIfAbsent(uuid(grant[0]), ignored -> new ArrayList<>()).add(String.valueOf(grant[1]));
        }
        return rows.stream().map(row -> {
            List<String> permissions = codes.getOrDefault(row.id(), List.of());
            return new RoleRow(row.id(), row.code(), permissions.size(), permissions);
        }).toList();
    }

    private boolean insert(String sql, Map<String, String> parameters) {
        try {
            var statement = entityManager.createNativeQuery(sql);
            parameters.forEach(statement::setParameter);
            statement.executeUpdate();
            return true;
        } catch (PersistenceException duplicate) {
            return false;
        }
    }

    private <T> long count(Class<T> type) {
        CriteriaBuilder criteria = entityManager.getCriteriaBuilder();
        var query = criteria.createQuery(Long.class);
        query.select(criteria.count(query.from(type)));
        return entityManager.createQuery(query).getSingleResult();
    }

    private static Predicate usernameMatches(CriteriaBuilder criteria, Root<JpaUserAccountEntity> root, String filter) {
        return criteria.like(criteria.lower(root.get("username")), filter);
    }

    private static List<Order> userOrder(CriteriaBuilder criteria, Root<JpaUserAccountEntity> root, PagedQuery query) {
        String field = switch (query.sortField()) {
            case "enabled" -> "enabled";
            case "authVersion" -> "authVersion";
            default -> "username";
        };
        Order primary = query.ascending() ? criteria.asc(root.get(field)) : criteria.desc(root.get(field));
        return List.of(primary, criteria.asc(root.get("id")));
    }

    private static AuditRow auditRow(Object[] row) {
        return new AuditRow(instant(row[0]), String.valueOf(row[1]), String.valueOf(row[2]), String.valueOf(row[3]),
                String.valueOf(row[4]), row[5] == null ? null : String.valueOf(row[5]));
    }

    private static Instant instant(Object value) {
        return switch (value) {
            case Instant instant -> instant;
            case Timestamp timestamp -> timestamp.toInstant();
            case OffsetDateTime offset -> offset.toInstant();
            case LocalDateTime local -> local.toInstant(ZoneOffset.UTC);
            default -> throw new IllegalStateException("Unsupported audit timestamp: " + value.getClass().getName());
        };
    }

    private static UUID uuid(Object value) {
        return value instanceof UUID id ? id : UUID.fromString(String.valueOf(value));
    }

    private static int offset(PagedQuery query) {
        return Math.toIntExact(Math.multiplyExact((long) query.page(), query.pageSize()));
    }

    private static String direction(PagedQuery query) {
        return query.ascending() ? "asc" : "desc";
    }
}
