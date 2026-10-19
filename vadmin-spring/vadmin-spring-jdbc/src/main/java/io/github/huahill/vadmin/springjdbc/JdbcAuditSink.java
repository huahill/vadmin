package io.github.huahill.vadmin.springjdbc;

import io.github.huahill.vadmin.contracts.audit.AuditEvent;
import io.github.huahill.vadmin.contracts.audit.AuditMetadataRedactor;
import io.github.huahill.vadmin.contracts.audit.AuditSink;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Timestamp;
import java.util.UUID;

/** Appends redacted audit metadata in the same JDBC transaction as the operation. */
public final class JdbcAuditSink implements AuditSink {

    private final JdbcTemplate jdbcTemplate;
    private final JdbcIamTables tables;
    private final UuidStorage uuidStorage;
    private final AuditMetadataRedactor redactor;
    private final JsonMapper json = JsonMapper.builder().build();

    public JdbcAuditSink(JdbcTemplate jdbcTemplate, String tablePrefix, UuidStorage uuidStorage,
                         AuditMetadataRedactor redactor) {
        this.jdbcTemplate = jdbcTemplate;
        this.tables = new JdbcIamTables(tablePrefix);
        this.uuidStorage = uuidStorage;
        this.redactor = redactor;
    }

    @Override
    public void append(AuditEvent event) {
        jdbcTemplate.update("""
                        INSERT INTO %s
                            (id, actor_user_id, action_code, target_type, target_id, outcome,
                             occurred_at, correlation_id, metadata)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, %s)
                        """.formatted(tables.audit(), uuidStorage == UuidStorage.NATIVE ? "CAST(? AS JSONB)" : "?"),
                uuidStorage.parameter(UUID.randomUUID()), uuidStorage.parameter(event.actorUserId()), event.actionCode(),
                event.targetType(), event.targetId(), event.outcome().name(),
                Timestamp.from(event.occurredAt()), event.correlationId(),
                json.writeValueAsString(redactor.redact(event.metadata())));
    }
}
