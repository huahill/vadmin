package io.github.huahill.vadmin.springjpa;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.hibernate.annotations.JavaType;

/// Marks a VAdmin identity UUID. The JDBC type depends on the dialect.
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@JavaType(IdentityUuidJavaType.class)
public @interface IdentityUuid {
}
