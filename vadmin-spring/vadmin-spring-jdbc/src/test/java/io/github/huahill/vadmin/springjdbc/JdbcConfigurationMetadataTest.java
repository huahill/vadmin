package io.github.huahill.vadmin.springjdbc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcConfigurationMetadataTest {
    @Test
    void publishesJdbcAndBootstrapPropertiesForIdeCompletion() throws Exception {
        var classes = Path.of(JdbcIamProperties.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        var metadata = classes.resolve("META-INF/spring-configuration-metadata.json");
        assertThat(metadata).isRegularFile();

        var document = JsonMapper.builder().build().readTree(Files.readString(metadata));
        var properties = new HashMap<String, JsonNode>();
        document.get("properties").forEach(property -> properties.put(property.get("name").asString(), property));
        assertThat(properties).containsKeys("vadmin.jdbc.enabled", "vadmin.jdbc.table-prefix",
                "vadmin.jdbc.uuid-storage", "vadmin.bootstrap.password");
        assertThat(properties.get("vadmin.jdbc.enabled").get("defaultValue").asBoolean()).isFalse();
        assertThat(properties.get("vadmin.jdbc.table-prefix").get("defaultValue").asString()).isEqualTo("vadmin");
        assertThat(properties.get("vadmin.jdbc.uuid-storage").get("type").asString())
                .isEqualTo(UuidStorage.class.getName());
        assertThat(properties.get("vadmin.bootstrap.password").get("type").asString()).isEqualTo("java.lang.String");
    }
}
