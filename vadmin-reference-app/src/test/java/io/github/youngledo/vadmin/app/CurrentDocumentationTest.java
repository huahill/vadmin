package io.github.youngledo.vadmin.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class CurrentDocumentationTest {
    private static final List<String> CURRENT_GUIDES = List.of(
            "README.md",
            "docs/dev/requirements.md",
            "docs/dev/contributing.md",
            "docs/en/user/getting-started.md",
            "docs/en/user/modules.md",
            "docs/en/user/configuration.md",
            "docs/en/user/appearance.md",
            "docs/en/user/security.md",
            "docs/en/user/deployment.md",
            "docs/en/user/upgrade.md",
            "docs/en/dev/architecture.md",
            "docs/en/dev/release-guide.md",
            "docs/en/dev/theme-tokens.md",
            "docs/zh-CN/user/getting-started.md",
            "docs/zh-CN/user/modules.md",
            "docs/zh-CN/user/configuration.md",
            "docs/zh-CN/user/appearance.md",
            "docs/zh-CN/user/security.md",
            "docs/zh-CN/user/deployment.md",
            "docs/zh-CN/user/upgrade.md",
            "docs/zh-CN/dev/architecture.md",
            "docs/zh-CN/dev/release-guide.md",
            "docs/zh-CN/dev/theme-tokens.md");

    @Test
    void currentAdoptionGuidesDescribeTheStarterInsteadOfRetiredExamplesOrARequiredCustomShell() throws IOException {
        var repositoryRoot = Path.of("").toAbsolutePath().getParent();
        for (var guide : CURRENT_GUIDES) {
            var content = Files.readString(repositoryRoot.resolve(guide));
            assertThat(content)
                    .as(guide)
                    .doesNotContain("admin-example-orders", "CustomersView", "/orders", "/customers")
                    .doesNotContain("must create `MainLayout`", "必须创建 `MainLayout`",
                            "InventoryViewProductionAnchor")
                    .doesNotContain("comfortable/compact", "舒适/紧凑", "semantic `--admin-*` tokens",
                            "语义主题 token")
                    .contains("VAdmin", "io.github.youngledo", "vadmin-spring-boot-starter")
                    .doesNotContain("Vaadin Admin Starter", "vaadin-admin-starter",
                            "io.github.vaadinadminstarter", "admin-spring-starter");
        }
    }
}
