package io.github.huahill.vadmin.springjdbc;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings for a host-migrated IAM schema; this module never creates tables. */
@ConfigurationProperties("vadmin.jdbc")
public final class JdbcIamProperties {
    private boolean enabled;
    private String tablePrefix = "vadmin";
    private UuidStorage uuidStorage = UuidStorage.STRING;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getTablePrefix() { return tablePrefix; }
    public void setTablePrefix(String tablePrefix) { this.tablePrefix = tablePrefix; }
    public UuidStorage getUuidStorage() { return uuidStorage; }
    public void setUuidStorage(UuidStorage uuidStorage) { this.uuidStorage = uuidStorage; }
}
