package io.github.huahill.vadmin.platform.access;

import io.github.huahill.vadmin.contracts.auth.CurrentUser;

public interface GrantPermissionUseCase { void grant(CurrentUser actor, GrantPermissionCommand command); }
