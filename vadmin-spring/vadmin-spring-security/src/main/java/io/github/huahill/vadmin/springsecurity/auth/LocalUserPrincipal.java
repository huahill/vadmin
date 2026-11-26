package io.github.huahill.vadmin.springsecurity.auth;

import io.github.huahill.vadmin.contracts.auth.CurrentUser;
import io.github.huahill.vadmin.contracts.auth.LocalUserAccount;
import java.util.Collection;
import java.util.Objects;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class LocalUserPrincipal implements UserDetails {
    private final CurrentUser currentUser;
    private final String password;
    private final boolean enabled;

    public LocalUserPrincipal(LocalUserAccount account) {
        this(new CurrentUser(account.userId(), account.username(), account.permissions(), account.authVersion()),
                account.passwordHash(), account.enabled());
    }

    public LocalUserPrincipal(CurrentUser currentUser) {
        this(currentUser, "", true);
    }

    private LocalUserPrincipal(CurrentUser currentUser, String password, boolean enabled) {
        this.currentUser = Objects.requireNonNull(currentUser, "currentUser");
        this.password = password == null ? "" : password;
        this.enabled = enabled;
    }

    public CurrentUser currentUser() {
        return currentUser;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return currentUser.permissions().stream()
                .map(permission -> new SimpleGrantedAuthority(permission.value()))
                .toList();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return currentUser.username();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
