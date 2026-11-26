package io.github.huahill.vadmin.contracts.auth;

/** Turns an authenticated external identity into the current user, including roles granted in VAdmin. */
public interface ExternalAccessResolver {
    CurrentUser resolve(ExternalIdentity identity);
}
