package io.github.huahill.vadmin.administration;

import io.github.huahill.vadmin.contracts.audit.AuditEvent;
import io.github.huahill.vadmin.contracts.audit.AuditOutcome;
import io.github.huahill.vadmin.contracts.audit.AuditSink;
import io.github.huahill.vadmin.contracts.auth.AuthorizationService;
import io.github.huahill.vadmin.contracts.auth.CurrentUser;
import io.github.huahill.vadmin.contracts.auth.ExternalDirectory;
import io.github.huahill.vadmin.contracts.auth.ExternalPerson;
import io.github.huahill.vadmin.contracts.auth.PermissionCode;
import io.github.huahill.vadmin.contracts.auth.SubjectId;
import io.github.huahill.vadmin.contracts.error.BusinessFailure;
import io.github.huahill.vadmin.contracts.error.ErrorCode;
import io.github.huahill.vadmin.platform.access.ExternalGrantStore;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Grants existing VAdmin roles to people who authenticate outside VAdmin. */
@Service
@ConditionalOnProperty(prefix = "vadmin.identity", name = "mode", havingValue = "external")
public class ExternalGrantService {
    private static final PermissionCode ASSIGN = PermissionCode.of("system:user:roles");
    private final ExternalGrantStore store;
    private final ExternalDirectory directory;
    private final AuthorizationService authorization;
    private final AuditSink auditSink;

    public ExternalGrantService(ExternalGrantStore store, ExternalDirectory directory,
                                AuthorizationService authorization, AuditSink auditSink) {
        this.store = store;
        this.directory = directory;
        this.authorization = authorization;
        this.auditSink = auditSink;
    }

    public List<ExternalGrantStore.ExternalGrant> grants() {
        return store.list();
    }

    public List<ExternalPerson> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return directory.search(query.strip());
    }

    @Transactional
    public void replaceRoles(CurrentUser actor, SubjectId subject, String label, Set<UUID> roleIds) {
        authorization.requirePermission(actor, ASSIGN);
        if (subject == null) {
            throw new BusinessFailure(ErrorCode.VALIDATION_FAILED, "validation.failed", Map.of("subject", "required"));
        }
        if (store.isLastAdministrator(subject) && !store.includesAdministrator(roleIds)) {
            throw new BusinessFailure(ErrorCode.VALIDATION_FAILED, "validation.failed",
                    Map.of("subject", "last-administrator"));
        }
        store.replaceRoles(subject, label, Set.copyOf(roleIds));
        auditSink.append(new AuditEvent(actor.subject().value(), "system:user:roles", "external-subject", subject.value(),
                AuditOutcome.SUCCESS, Instant.now(), null, Map.of()));
    }
}
