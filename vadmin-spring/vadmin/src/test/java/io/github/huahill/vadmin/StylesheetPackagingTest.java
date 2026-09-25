package io.github.huahill.vadmin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StylesheetPackagingTest {
    @Test
    void doesNotShipAVadminVisualLanguageOverlay() {
        var classLoader = getClass().getClassLoader();

        assertThat(classLoader.getResource("META-INF/resources/vadmin/ant.css")).isNull();
        assertThat(classLoader.getResource("META-INF/resources/vadmin/icons/users.svg")).isNull();
    }
}
