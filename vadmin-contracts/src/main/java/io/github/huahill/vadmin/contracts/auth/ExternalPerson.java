package io.github.huahill.vadmin.contracts.auth;

/** A person from an external directory. The subject is the only identity VAdmin stores. */
public record ExternalPerson(SubjectId subject, String username, String displayName) {
    public ExternalPerson {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        username = username.strip();
        displayName = displayName == null || displayName.isBlank() ? username : displayName.strip();
    }
}
