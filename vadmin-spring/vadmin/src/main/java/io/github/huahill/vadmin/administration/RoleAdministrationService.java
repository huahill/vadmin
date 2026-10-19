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
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnSystemAdministration
public class RoleAdministrationService {
    private final AdministrationStore store;
    private final AuthorizationService authorization;
    private final AuditSink audit;

    public RoleAdministrationService(AdministrationStore store, AuthorizationService authorization, AuditSink audit) {
        this.store = store;
        this.authorization = authorization;
        this.audit = audit;
    }

    @Transactional
    public void create(CurrentUser actor, String code) {
        authorization.requirePermission(actor, PermissionCode.of("system:role:create"));
        if (code == null || code.isBlank() || code.strip().length() > 120) {
            throw invalid("code", "invalid");
        }
        UUID id = UUID.randomUUID();
        if (!store.insertRole(id, code.strip())) {
            throw invalid("code", "already-exists");
        }
        audit.append(event(actor, "system:role:create", "role", id, Map.of()));
    }

    @Transactional
    public void assignRoles(CurrentUser actor, UUID userId, Set<UUID> roleIds) {
        authorization.requirePermission(actor, PermissionCode.of("system:user:roles"));
        if (userId == null || roleIds == null || roleIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw invalid("roles", "invalid");
        }
        Set<UUID> selected = Set.copyOf(roleIds);
        store.lockAdministratorRole();
        if (!store.userExists(userId)) {
            throw invalid("user", "not-found");
        }
        var roles = store.roles();
        var knownIds = roles.stream().map(AdministrationStore.RoleRow::id).collect(Collectors.toSet());
        if (!knownIds.containsAll(selected)) {
            throw invalid("roles", "not-found");
        }
        boolean keepsAdministrator = roles.stream()
                .anyMatch(role -> role.code().equals("administrator") && selected.contains(role.id()));
        if (!keepsAdministrator && store.isLastEnabledAdministrator(userId)) {
            throw invalid("roles", "last-administrator");
        }
        store.replaceUserRoles(userId, selected);
        audit.append(event(actor, "system:user:roles", "user", userId,
                Map.of("roleIds", selected.stream().map(UUID::toString).sorted().collect(Collectors.joining(",")))));
    }

    private static BusinessFailure invalid(String field, String reason) {
        return new BusinessFailure(ErrorCode.VALIDATION_FAILED, "validation.failed", Map.of(field, reason));
    }

    private static AuditEvent event(CurrentUser actor, String action, String type, UUID id, Map<String, String> metadata) {
        return new AuditEvent(actor.userId(), action, type, id.toString(), AuditOutcome.SUCCESS,
                Instant.now(), null, metadata);
    }
}
