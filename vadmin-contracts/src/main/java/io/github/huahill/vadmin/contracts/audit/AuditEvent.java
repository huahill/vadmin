package io.github.huahill.vadmin.contracts.audit;

import java.time.Instant;
import java.util.Map;

public record AuditEvent(String actorSubject, String actionCode, String targetType, String targetId,
                         AuditOutcome outcome, Instant occurredAt, String correlationId,
                         Map<String, String> metadata) {
    public AuditEvent {
        if (actorSubject == null || actorSubject.isBlank()) {
            throw new IllegalArgumentException("actorSubject must not be blank");
        }
        metadata = Map.copyOf(metadata);
    }
}
