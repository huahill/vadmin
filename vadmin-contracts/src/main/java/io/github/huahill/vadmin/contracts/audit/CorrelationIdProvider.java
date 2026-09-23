package io.github.huahill.vadmin.contracts.audit;

@FunctionalInterface
public interface CorrelationIdProvider {
    String currentCorrelationId();
}
