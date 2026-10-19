package io.github.huahill.vadmin.springjdbc;

record JdbcIamTables(String prefix) {
    JdbcIamTables {
        if (prefix != null && !prefix.matches("[A-Za-z][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("IAM table prefix must be a SQL identifier");
        }
        prefix = (prefix == null ? "vadmin" : prefix).toLowerCase(java.util.Locale.ROOT);
        if (!prefix.endsWith("_")) {
            prefix += "_";
        }
        if (prefix.length() + "role_permissions".length() > 63) {
            throw new IllegalArgumentException("IAM table prefix exceeds the 63-character identifier limit");
        }
    }

    String users() { return prefix + "users"; }
    String roles() { return prefix + "roles"; }
    String permissions() { return prefix + "permissions"; }
    String userRoles() { return prefix + "user_roles"; }
    String rolePermissions() { return prefix + "role_permissions"; }
    String audit() { return prefix + "audit_entries"; }
}
