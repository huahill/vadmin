package io.github.huahill.vadmin.contracts.auth;

import java.util.Optional;
import java.util.UUID;

public interface LocalUserSessionLookup {
    Optional<LocalUserSession> findSessionByUserId(UUID userId);

    /**
     * Session state for a local UUID or an external subject. The default only understands UUID subjects;
     * external identity replaces this method.
     */
    default Optional<LocalUserSession> findSession(SubjectId subject) {
        try {
            return findSessionByUserId(UUID.fromString(subject.value()));
        } catch (IllegalArgumentException notALocalAccount) {
            return Optional.empty();
        }
    }
}
