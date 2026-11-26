package io.github.huahill.vadmin.contracts.auth;

import java.util.Objects;
import java.util.UUID;

/** Stable identity of a person. Local accounts use their UUID text; external accounts use the issuer's subject. */
public record SubjectId(String value) {
    public static final int MAX_LENGTH = 190;

    public SubjectId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("subject must not be blank");
        }
        value = value.strip();
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("subject exceeds " + MAX_LENGTH + " characters");
        }
    }

    public static SubjectId of(String value) {
        return new SubjectId(value);
    }

    public static SubjectId of(UUID id) {
        Objects.requireNonNull(id, "id");
        return new SubjectId(id.toString());
    }
}
