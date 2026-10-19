package io.github.huahill.vadmin.localiam;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;

/** System administration defaults to local IAM, but can use a host-provided store. */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(ConditionalOnSystemAdministration.Enabled.class)
public @interface ConditionalOnSystemAdministration {
    final class Enabled extends SpringBootCondition {
        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            var environment = context.getEnvironment();
            boolean enabled = environment.getProperty("vadmin.system-admin.enabled", Boolean.class,
                    environment.getProperty("vadmin.local-iam.enabled", Boolean.class, true));
            return new ConditionOutcome(enabled, "VAdmin system administration enabled=" + enabled);
        }
    }
}
