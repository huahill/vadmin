package io.github.huahill.vadmin.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

class ConsumerBomContractTest {
    private static final String MAVEN_NS = "http://maven.apache.org/POM/4.1.0";
    private static final Pattern VAADIN_VERSION = Pattern.compile("<vaadin.version>([^<]+)</vaadin.version>");

    @Test
    void consumerBomMatchesTheParentContractAndDoesNotExportInternalLines() throws Exception {
        var repositoryRoot = Path.of("").toAbsolutePath().getParent();
        var parent = managedDependencies(repositoryRoot.resolve("pom.xml"));
        var bom = managedDependencies(repositoryRoot.resolve("vadmin-bom/pom.xml"));

        assertThat(parent).containsExactlyElementsOf(bom);
        assertThat(parent).extracting(ManagedDependency::artifactId)
                .doesNotContain(
                        "spring-boot-dependencies",
                        "testcontainers-bom",
                        "flyway-core",
                        "flyway-database-postgresql");
        assertThat(bom).extracting(ManagedDependency::artifactId)
                .contains("vaadin-bom", "vadmin-spring-boot-starter")
                .doesNotContain("vadmin-bom", "vadmin-reference-app");
    }

    @Test
    void adoptionGuidesImportTheBomInsteadOfAConsumerVaadinVersion() throws Exception {
        var repositoryRoot = Path.of("").toAbsolutePath().getParent();
        var vaadinVersion = vaadinVersion(repositoryRoot.resolve("pom.xml"));
        for (var guide : List.of("docs/en/user/getting-started.md", "docs/zh-CN/user/getting-started.md")) {
            var content = Files.readString(repositoryRoot.resolve(guide));
            assertThat(content)
                    .as(guide)
                    .contains("vadmin-bom")
                    .contains("<artifactId>vaadin-dev</artifactId>")
                    .doesNotContain("${vaadin.version}")
                    .doesNotContain("<artifactId>vaadin-bom</artifactId>");
        }
        for (var guide : List.of("docs/en/user/deployment.md", "docs/zh-CN/user/deployment.md")) {
            var content = Files.readString(repositoryRoot.resolve(guide));
            assertThat(content)
                    .as(guide)
                    .contains("<artifactId>flow-maven-plugin</artifactId>")
                    .contains("<version>" + vaadinVersion + "</version>")
                    .contains("<artifactId>flow-server-production-mode</artifactId>")
                    .doesNotContain("${vaadin.version}");
        }
    }

    private static String vaadinVersion(Path pom) throws Exception {
        var matcher = VAADIN_VERSION.matcher(Files.readString(pom));
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private static List<ManagedDependency> managedDependencies(Path pom) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var document = factory.newDocumentBuilder().parse(pom.toFile());
        var management = (Element) document.getElementsByTagNameNS(MAVEN_NS, "dependencyManagement").item(0);
        var dependencies = firstChild(management, "dependencies");
        var result = new ArrayList<ManagedDependency>();
        var child = dependencies.getFirstChild();
        while (child != null) {
            if (child instanceof Element element && "dependency".equals(element.getLocalName())) {
                result.add(new ManagedDependency(
                        text(element, "groupId"),
                        text(element, "artifactId"),
                        text(element, "version"),
                        text(element, "type"),
                        text(element, "scope")));
            }
            child = child.getNextSibling();
        }
        return result;
    }

    private static Element firstChild(Element parent, String name) {
        var child = parent.getFirstChild();
        while (child != null) {
            if (child instanceof Element element && name.equals(element.getLocalName())) {
                return element;
            }
            child = child.getNextSibling();
        }
        throw new IllegalStateException("Missing " + name);
    }

    private static String text(Element dependency, String name) {
        var child = dependency.getFirstChild();
        while (child != null) {
            if (child instanceof Element element && name.equals(element.getLocalName())) {
                return element.getTextContent().trim();
            }
            child = child.getNextSibling();
        }
        return "";
    }


    private record ManagedDependency(
            String groupId, String artifactId, String version, String type, String scope) {
    }
}
