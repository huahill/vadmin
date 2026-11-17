package io.github.huahill.vadmin.springjpa.access;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "vadmin_permissions")
public class JpaPermissionEntity {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "system_managed", nullable = false)
    private boolean systemManaged;

    protected JpaPermissionEntity() { }

    public JpaPermissionEntity(UUID id, String code, boolean systemManaged) {
        this.id = id;
        this.code = code;
        this.systemManaged = systemManaged;
    }

    public UUID id() {
        return id;
    }

    public String code() {
        return code;
    }

    public boolean systemManaged() {
        return systemManaged;
    }
}
