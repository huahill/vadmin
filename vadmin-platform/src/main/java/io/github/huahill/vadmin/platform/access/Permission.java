package io.github.huahill.vadmin.platform.access;

import java.util.UUID;

import io.github.huahill.vadmin.contracts.auth.PermissionCode;

public record Permission(UUID id, PermissionCode code) { }
