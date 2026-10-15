package io.github.huahill.vadmin.springjpa;

import java.util.Locale;
import java.util.regex.Pattern;

/// 本地 IAM 六张表的物理名。未配置时前缀为 {@code vadmin}，规范化后写成 {@code vadmin_}。
public final class AccessControlTables {
    static final String DEFAULT_CONFIGURED_PREFIX = "vadmin";
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");
    private static final String LONGEST_TABLE_SUFFIX = "role_permissions";
    private static final int POSTGRESQL_IDENTIFIER_LIMIT = 63;

    private final String prefix;

    private AccessControlTables(String prefix) {
        this.prefix = prefix;
    }

    public static AccessControlTables of(String configuredPrefix) {
        if (configuredPrefix != null && (configuredPrefix.isBlank() || !IDENTIFIER.matcher(configuredPrefix).matches())) {
            throw new IllegalArgumentException(
                    "vadmin.local-iam.table-prefix must be a SQL identifier, but was '" + configuredPrefix + "'");
        }
        var normalized = (configuredPrefix == null ? DEFAULT_CONFIGURED_PREFIX : configuredPrefix)
                .toLowerCase(Locale.ROOT);
        if (!normalized.endsWith("_")) {
            normalized = normalized + "_";
        }
        if (normalized.length() + LONGEST_TABLE_SUFFIX.length() > POSTGRESQL_IDENTIFIER_LIMIT) {
            throw new IllegalArgumentException(
                    "vadmin.local-iam.table-prefix exceeds the PostgreSQL 63-character identifier limit");
        }
        return new AccessControlTables(normalized);
    }

    public String prefix() {
        return prefix;
    }

    public String users() {
        return prefix + "users";
    }

    public String roles() {
        return prefix + "roles";
    }

    public String permissions() {
        return prefix + "permissions";
    }

    public String userRoles() {
        return prefix + "user_roles";
    }

    public String rolePermissions() {
        return prefix + "role_permissions";
    }

    public String auditEntries() {
        return prefix + "audit_entries";
    }
}
