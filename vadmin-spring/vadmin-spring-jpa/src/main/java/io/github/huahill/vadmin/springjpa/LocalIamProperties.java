package io.github.huahill.vadmin.springjpa;

import org.springframework.boot.context.properties.ConfigurationProperties;

/// 本地 IAM 的表名前缀。未配置时为 {@code vadmin}，物理表名写成 {@code vadmin_users} 这一组。
@ConfigurationProperties("vadmin.local-iam")
public final class LocalIamProperties {
    private String tablePrefix = AccessControlTables.DEFAULT_CONFIGURED_PREFIX;

    public String getTablePrefix() {
        return tablePrefix;
    }

    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }

    public AccessControlTables tables() {
        return AccessControlTables.of(tablePrefix);
    }
}
