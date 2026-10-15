package io.github.huahill.vadmin.springjpa;

import java.util.HashMap;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;

/// 把已经规范化的表前缀合并进 Flyway 占位符。SQL 只能使用 {@code ${vadminTablePrefix}}。
public final class AccessControlFlywayCustomizer implements FlywayConfigurationCustomizer {
    static final String PLACEHOLDER = "vadminTablePrefix";

    private final LocalIamProperties properties;

    public AccessControlFlywayCustomizer(LocalIamProperties properties) {
        this.properties = properties;
    }

    @Override
    public void customize(FluentConfiguration configuration) {
        var placeholders = new HashMap<String, String>();
        var existing = configuration.getPlaceholders();
        if (existing != null) {
            placeholders.putAll(existing);
        }
        placeholders.put(PLACEHOLDER, properties.tables().prefix());
        configuration.placeholders(placeholders);
    }
}
