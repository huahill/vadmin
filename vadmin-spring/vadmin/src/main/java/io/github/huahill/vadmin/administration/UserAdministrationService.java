package io.github.huahill.vadmin.administration;

import io.github.huahill.vadmin.contracts.audit.AuditEvent;
import io.github.huahill.vadmin.contracts.audit.AuditOutcome;
import io.github.huahill.vadmin.contracts.audit.AuditSink;
import io.github.huahill.vadmin.contracts.auth.AuthorizationService;
import io.github.huahill.vadmin.contracts.auth.CurrentUser;
import io.github.huahill.vadmin.contracts.auth.PermissionCode;
import io.github.huahill.vadmin.contracts.error.BusinessFailure;
import io.github.huahill.vadmin.contracts.error.ErrorCode;
import io.github.huahill.vadmin.localiam.ConditionalOnSystemAdministration;
import io.github.huahill.vadmin.platform.administration.AdministrationStore;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnSystemAdministration
public class UserAdministrationService {
    private static final PermissionCode CREATE = PermissionCode.of("system:user:create");
    private static final PermissionCode UPDATE = PermissionCode.of("system:user:update");
    private final AdministrationStore store;
    private final AuthorizationService authorization;
    private final AuditSink auditSink;
    private final PasswordEncoder passwordEncoder;

    public UserAdministrationService(AdministrationStore store, AuthorizationService authorization, AuditSink auditSink,
                                     PasswordEncoder passwordEncoder) {
        this.store = store;
        this.authorization = authorization;
        this.auditSink = auditSink;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void create(CurrentUser actor, String username, String password) {
        authorization.requirePermission(actor, CREATE);
        var userId = UUID.randomUUID();
        String name = required("username", username);
        if (name.length() > 120) {
            throw new BusinessFailure(ErrorCode.VALIDATION_FAILED, "validation.failed", Map.of("username", "too-long"));
        }
        if (password == null || password.isBlank()) {
            throw new BusinessFailure(ErrorCode.VALIDATION_FAILED, "validation.failed", Map.of("password", "required"));
        }
        if (!store.insertUser(userId, name, passwordEncoder.encode(password))) {
            throw new BusinessFailure(ErrorCode.VALIDATION_FAILED, "validation.failed",
                    Map.of("username", "already-exists"));
        }
        auditSink.append(new AuditEvent(actor.userId(), "system:user:create", "user", userId.toString(),
                AuditOutcome.SUCCESS, Instant.now(), null, Map.of()));
    }

    @Transactional
    public void setEnabled(CurrentUser actor, UUID userId, boolean enabled) {
        setEnabled(actor, Set.of(userId), enabled);
    }

    @Transactional
    public void setEnabled(CurrentUser actor, Set<UUID> userIds, boolean enabled) {
        authorization.requirePermission(actor, UPDATE);
        store.lockAdministratorRole();
        Set.copyOf(userIds).forEach(userId -> {
            if (!store.userExists(userId)) {
                throw new BusinessFailure(ErrorCode.VALIDATION_FAILED, "validation.failed", Map.of("user", "not-found"));
            }
            if (!enabled && store.isLastEnabledAdministrator(userId)) {
                throw new BusinessFailure(ErrorCode.VALIDATION_FAILED, "validation.failed", Map.of("user", "last-administrator"));
            }
            store.setEnabled(userId, enabled);
            auditSink.append(new AuditEvent(actor.userId(), "system:user:update", "user", userId.toString(),
                    AuditOutcome.SUCCESS, Instant.now(), null, Map.of("enabled", Boolean.toString(enabled))));
        });
    }

    private String required(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessFailure(ErrorCode.VALIDATION_FAILED, "validation.failed", Map.of(field, "required"));
        }
        return value.strip();
    }
}
