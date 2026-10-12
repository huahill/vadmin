package io.github.huahill.vadmin.shell;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AdminShellPropertiesTest {
    @Test
    void enablesTheWorkplaceByDefault() {
        assertThat(new AdminShellProperties().isWorkplaceEnabled()).isTrue();
    }

    @Test
    void acceptsDisablingTheWorkplaceNavigationEntry() {
        var properties = new AdminShellProperties();

        properties.setWorkplaceEnabled(false);

        assertThat(properties.isWorkplaceEnabled()).isFalse();
    }
}
