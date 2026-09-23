package io.github.huahill.vadmin.contracts.navigation;

import io.github.huahill.vadmin.contracts.auth.PermissionCode;

public record ActionDefinition(String actionId, PermissionCode requiredPermission, boolean destructive) { }
