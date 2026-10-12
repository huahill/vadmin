package io.github.huahill.vadmin.brand;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AdminBrandPropertiesTest {
    @Test
    void usesTheConfiguredProductName() {
        var properties = new AdminBrandProperties();
        properties.setName("  RDC OpenAPI OPS  ");

        assertThat(properties.getName()).isEqualTo("RDC OpenAPI OPS");
    }

    @Test
    void fallsBackToTheVadminProductNameWhenTheConfigurationIsBlank() {
        var properties = new AdminBrandProperties();
        properties.setName(" ");

        assertThat(properties.getName()).isEqualTo("VAdmin");
    }
}
