package io.github.huahill.vadmin.springjpa.access;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "vadmin_roles")
public class JpaRoleEntity {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    protected JpaRoleEntity() { }

    public JpaRoleEntity(UUID id, String code) {
        this.id = id;
        this.code = code;
    }

    public UUID id() {
        return id;
    }

    public String code() {
        return code;
    }
}
