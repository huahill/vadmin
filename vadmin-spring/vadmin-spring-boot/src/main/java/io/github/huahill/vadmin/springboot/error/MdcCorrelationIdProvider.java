package io.github.huahill.vadmin.springboot.error;

import io.github.huahill.vadmin.contracts.audit.CorrelationIdProvider;
import org.slf4j.MDC;

public final class MdcCorrelationIdProvider implements CorrelationIdProvider {
    @Override
    public String currentCorrelationId() {
        return MDC.get(CorrelationIdFilter.MDC_KEY);
    }
}
