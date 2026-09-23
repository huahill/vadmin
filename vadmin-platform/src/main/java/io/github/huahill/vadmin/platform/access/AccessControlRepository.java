package io.github.huahill.vadmin.platform.access;

import java.util.Optional;
import java.util.UUID;

import io.github.huahill.vadmin.contracts.auth.PermissionCode;

public interface AccessControlRepository {
    Optional<Role> findRoleByCode(String code);
    Optional<Permission> findPermissionByCode(PermissionCode code);
    void grantPermission(UUID roleId, UUID permissionId);
    void incrementAuthVersionForRole(UUID roleId);
}
