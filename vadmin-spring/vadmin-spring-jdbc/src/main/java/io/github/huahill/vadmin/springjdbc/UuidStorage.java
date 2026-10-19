package io.github.huahill.vadmin.springjdbc;

import java.util.UUID;

/** Bind UUIDs as native PostgreSQL UUIDs or MySQL CHAR(36) strings. */
public enum UuidStorage {
    NATIVE, STRING;

    public Object parameter(UUID id) {
        return this == NATIVE ? id : id.toString();
    }
}
