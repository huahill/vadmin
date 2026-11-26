package io.github.huahill.vadmin.springjdbc;

import io.github.huahill.vadmin.contracts.auth.CurrentUser;
import io.github.huahill.vadmin.contracts.auth.ExternalAccessResolver;
import io.github.huahill.vadmin.contracts.auth.ExternalIdentity;
import io.github.huahill.vadmin.contracts.auth.SubjectId;
import io.github.huahill.vadmin.platform.access.ExternalGrantStore;

/** Resolves an external login from VAdmin role grants. It does not create a local account. */
public final class JdbcExternalAccessResolver implements ExternalAccessResolver {
    private final ExternalGrantStore grants;

    public JdbcExternalAccessResolver(ExternalGrantStore grants) {
        this.grants = grants;
    }

    @Override
    public CurrentUser resolve(ExternalIdentity identity) {
        SubjectId subject = SubjectId.of(identity.subject());
        var access = grants.access(subject);
        String username = identity.displayName() == null || identity.displayName().isBlank()
                ? subject.value() : identity.displayName().strip();
        return new CurrentUser(subject, username, access.permissions(), access.authVersion());
    }
}
