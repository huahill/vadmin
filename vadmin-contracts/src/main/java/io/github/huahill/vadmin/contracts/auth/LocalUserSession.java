package io.github.huahill.vadmin.contracts.auth;

import java.util.Objects;
import java.util.UUID;

public record LocalUserSession(SubjectId subject, boolean enabled, long authVersion) {
    public LocalUserSession {
        Objects.requireNonNull(subject, "subject");
        if (authVersion < 0) {
            throw new IllegalArgumentException("authVersion must not be negative");
        }
    }

    public LocalUserSession(UUID userId, boolean enabled, long authVersion) {
        this(SubjectId.of(userId), enabled, authVersion);
    }

    public UUID userId() {
        return UUID.fromString(subject.value());
    }
}
