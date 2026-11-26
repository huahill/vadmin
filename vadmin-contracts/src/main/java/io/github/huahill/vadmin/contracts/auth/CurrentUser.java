package io.github.huahill.vadmin.contracts.auth;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record CurrentUser(SubjectId subject, String username, Set<PermissionCode> permissions, long authVersion)
        implements Serializable {
    public CurrentUser {
        Objects.requireNonNull(subject, "subject");
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        username = username.strip();
        permissions = Set.copyOf(permissions);
        if (authVersion < 0) {
            throw new IllegalArgumentException("authVersion must not be negative");
        }
    }

    /** Local accounts are identified by a UUID. External subjects use {@link #subject()}. */
    public CurrentUser(UUID userId, String username, Set<PermissionCode> permissions, long authVersion) {
        this(SubjectId.of(userId), username, permissions, authVersion);
    }
}
