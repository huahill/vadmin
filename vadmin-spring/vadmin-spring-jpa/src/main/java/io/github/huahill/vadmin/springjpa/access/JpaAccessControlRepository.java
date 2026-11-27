package io.github.huahill.vadmin.springjpa.access;

import io.github.huahill.vadmin.contracts.auth.PermissionCode;
import io.github.huahill.vadmin.platform.access.AccessControlRepository;
import io.github.huahill.vadmin.platform.access.Permission;
import io.github.huahill.vadmin.platform.access.Role;
import io.github.huahill.vadmin.springjpa.AccessControlTables;
import io.github.huahill.vadmin.springjpa.IdentityUuidParameter;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;

public final class JpaAccessControlRepository implements AccessControlRepository {
    private final EntityManager entityManager;
    private final AccessControlTables tables;

    public JpaAccessControlRepository(EntityManager entityManager, AccessControlTables tables) {
        this.entityManager = entityManager;
        this.tables = tables;
    }

    @Override
    public Optional<Role> findRoleByCode(String code) {
        return entityManager.createQuery("select role from JpaRoleEntity role where role.code = :code", JpaRoleEntity.class)
                .setParameter("code", code)
                .getResultStream()
                .findFirst()
                .map(role -> new Role(role.id(), role.code()));
    }

    @Override
    public Optional<Permission> findPermissionByCode(PermissionCode code) {
        return entityManager.createQuery(
                        "select permission from JpaPermissionEntity permission where permission.code = :code",
                        JpaPermissionEntity.class)
                .setParameter("code", code.value())
                .getResultStream()
                .findFirst()
                .map(permission -> new Permission(permission.id(), PermissionCode.of(permission.code())));
    }

    @Override
    public void grantPermission(UUID roleId, UUID permissionId) {
        entityManager.createNativeQuery("""
                insert into %s (role_id, permission_id)
                values (:roleId, :permissionId)
                on conflict do nothing
                """.formatted(tables.rolePermissions()))
                .setParameter("roleId", IdentityUuidParameter.value(entityManager, roleId))
                .setParameter("permissionId", IdentityUuidParameter.value(entityManager, permissionId))
                .executeUpdate();
    }

    @Override
    public boolean hasGrantedPermission(UUID roleId, UUID permissionId) {
        Number count = (Number) entityManager.createNativeQuery(
                "select count(*) from %s where role_id = :roleId and permission_id = :permissionId"
                        .formatted(tables.rolePermissions()))
                .setParameter("roleId", IdentityUuidParameter.value(entityManager, roleId))
                .setParameter("permissionId", IdentityUuidParameter.value(entityManager, permissionId))
                .getSingleResult();
        return count != null && count.longValue() > 0L;
    }

    @Override
    public int grantedPermissionCount(UUID roleId) {
        Number count = (Number) entityManager.createNativeQuery(
                "select count(*) from %s where role_id = :roleId".formatted(tables.rolePermissions()))
                .setParameter("roleId", IdentityUuidParameter.value(entityManager, roleId))
                .getSingleResult();
        return count == null ? 0 : count.intValue();
    }

    @Override
    public boolean revokePermission(UUID roleId, UUID permissionId) {
        return entityManager.createNativeQuery(
                "delete from %s where role_id = :roleId and permission_id = :permissionId"
                        .formatted(tables.rolePermissions()))
                .setParameter("roleId", IdentityUuidParameter.value(entityManager, roleId))
                .setParameter("permissionId", IdentityUuidParameter.value(entityManager, permissionId))
                .executeUpdate() > 0;
    }

    @Override
    public void incrementAuthVersionForRole(UUID roleId) {
        entityManager.createNativeQuery("""
                update %s
                set auth_version = auth_version + 1
                where id in (select user_id from %s where role_id = :roleId)
                """.formatted(tables.users(), tables.userRoles()))
                .setParameter("roleId", IdentityUuidParameter.value(entityManager, roleId))
                .executeUpdate();
    }
}
