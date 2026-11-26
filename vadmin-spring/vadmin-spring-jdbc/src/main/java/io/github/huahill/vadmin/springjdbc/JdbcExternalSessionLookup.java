package io.github.huahill.vadmin.springjdbc;

import io.github.huahill.vadmin.contracts.auth.LocalUserSession;
import io.github.huahill.vadmin.contracts.auth.LocalUserSessionLookup;
import io.github.huahill.vadmin.contracts.auth.SubjectId;
import io.github.huahill.vadmin.platform.access.ExternalGrantStore;
import java.util.Optional;
import java.util.UUID;

/** Keeps an external session valid until its granted roles change. Missing grants stay signed in with no permissions. */
public final class JdbcExternalSessionLookup implements LocalUserSessionLookup {
    private final ExternalGrantStore grants;

    public JdbcExternalSessionLookup(ExternalGrantStore grants) {
        this.grants = grants;
    }

    @Override
    public Optional<LocalUserSession> findSessionByUserId(UUID userId) {
        return findSession(SubjectId.of(userId));
    }

    @Override
    public Optional<LocalUserSession> findSession(SubjectId subject) {
        return Optional.of(new LocalUserSession(subject, true, grants.access(subject).authVersion()));
    }
}
