package io.github.huahill.vadmin.contracts.audit;

public interface AuditSink { void append(AuditEvent event); }
