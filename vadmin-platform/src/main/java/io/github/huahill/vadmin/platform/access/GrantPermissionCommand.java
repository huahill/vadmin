package io.github.huahill.vadmin.platform.access;

import io.github.huahill.vadmin.contracts.auth.PermissionCode;

public record GrantPermissionCommand(String roleCode, PermissionCode permissionCode) { }
