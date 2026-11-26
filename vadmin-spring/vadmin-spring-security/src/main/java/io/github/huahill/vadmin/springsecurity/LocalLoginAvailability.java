package io.github.huahill.vadmin.springsecurity;

/** Local username/password login is hidden when an external identity mode is selected. */
public record LocalLoginAvailability(boolean available) {
}
