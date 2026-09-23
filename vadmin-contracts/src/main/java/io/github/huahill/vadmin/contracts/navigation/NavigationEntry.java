package io.github.huahill.vadmin.contracts.navigation;

import io.github.huahill.vadmin.contracts.auth.PermissionCode;

public record NavigationEntry(String pageId, String titleKey, String iconKey, int order, PermissionCode requiredPermission) { }
