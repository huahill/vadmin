package io.github.huahill.vadmin.springjpa.access;

import io.github.huahill.vadmin.springjpa.IdentityUuid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "vadmin_users")
public class JpaUserAccountEntity {
    @Id
    @IdentityUuid
    @Column(length = 36)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "auth_version", nullable = false)
    private long authVersion;

    protected JpaUserAccountEntity() { }

    public UUID id() {
        return id;
    }

    public String username() {
        return username;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public boolean enabled() {
        return enabled;
    }

    public long authVersion() {
        return authVersion;
    }
}
