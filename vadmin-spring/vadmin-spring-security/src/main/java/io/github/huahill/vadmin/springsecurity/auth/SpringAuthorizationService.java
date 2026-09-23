package io.github.huahill.vadmin.springsecurity.auth;

import io.github.huahill.vadmin.contracts.auth.AuthorizationService;
import io.github.huahill.vadmin.contracts.auth.CurrentUser;
import io.github.huahill.vadmin.contracts.auth.PermissionCode;
import io.github.huahill.vadmin.contracts.error.BusinessFailure;
import io.github.huahill.vadmin.contracts.error.ErrorCode;
import java.util.Map;

public final class SpringAuthorizationService implements AuthorizationService {
    @Override
    public boolean hasPermission(CurrentUser user, PermissionCode permission) {
        return user.permissions().contains(permission);
    }

    @Override
    public void requirePermission(CurrentUser user, PermissionCode permission) {
        if (!hasPermission(user, permission)) {
            throw new BusinessFailure(ErrorCode.AUTHORIZATION_DENIED, "authorization.denied", Map.of());
        }
    }
}
