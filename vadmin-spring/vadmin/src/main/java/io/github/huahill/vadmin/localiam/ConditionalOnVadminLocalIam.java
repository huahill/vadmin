package io.github.huahill.vadmin.localiam;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Enables a VAdmin local IAM contribution unless the host explicitly delegates identity ownership.
 *
 * <p>Hosts with an existing authentication and authorization system set
 * {@code vadmin.local-iam.enabled=false}. In that mode they provide VAdmin's user and authorization
 * contracts themselves. System pages default to disabled, but hosts may enable
 * {@code vadmin.system-admin.enabled} with their own administration store.</p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnProperty(prefix = "vadmin.local-iam", name = "enabled", havingValue = "true", matchIfMissing = true)
public @interface ConditionalOnVadminLocalIam {
}
