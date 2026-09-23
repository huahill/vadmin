package io.github.huahill.vadmin.contracts.auth;

import java.util.Set;

import io.github.huahill.vadmin.contracts.error.BusinessFailure;
import io.github.huahill.vadmin.contracts.error.ErrorCode;

public final class PermissionCatalog {
    private final Set<PermissionCode> permissions;

    public PermissionCatalog(Set<PermissionCode> permissions) {
        this.permissions = Set.copyOf(permissions);
    }

    public void requireKnown(PermissionCode permission) {
        if (!permissions.contains(permission)) {
            throw new BusinessFailure(ErrorCode.VALIDATION_FAILED, "permission.unknown", java.util.Map.of("permissionCode", "unknown"));
        }
    }

    public Set<PermissionCode> all() {
        return permissions;
    }
}
