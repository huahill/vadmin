package io.github.huahill.vadmin.contracts.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class DependencyUpdateVisibilityTest {

    private static final Set<String> IGNORED_VERSION_PROPERTIES = Set.of("project.version");

    @Test
    void rootPomExposesEveryVersionPropertyDependabotCanSee() throws Exception {
        Path rootPom = rootPom();
        String rootContent = Files.readString(rootPom);
        List<String> hidden = new ArrayList<>();
        for (Path pom : reactorPoms(rootPom)) {
            for (String property : versionProperties(pom)) {
                String declaration = "<version>${" + property + "}</version>";
                if (!rootContent.contains(declaration)) {
                    hidden.add(pom + " uses ${" + property + "}");
                }
            }
        }

        assertThat(hidden).isEmpty();
    }

    @Test
    void internalLinesStayOutOfTheExportedBomAndTheDefaultBuild() throws Exception {
        Element project = document(rootPom()).getDocumentElement();
        Element profile = directChildren(project, "profiles").stream()
                .flatMap(profiles -> directChildren(profiles, "profile").stream())
                .filter(candidate -> "dependency-updates".equals(directText(candidate, "id")))
                .findFirst()
                .orElseThrow();
        Element activation = directChildren(profile, "activation").getFirst();

        assertThat(directChildren(activation, "activeByDefault")).isEmpty();
        assertThat(directText(directChildren(activation, "property").getFirst(), "name"))
                .isEqualTo("vadmin.dependencyUpdateScan");
        assertThat(directText(directChildren(activation, "property").getFirst(), "value")).isEqualTo("true");
        assertThat(managedArtifactIds(profile))
                .containsExactlyInAnyOrder(
                        "flyway-core", "flyway-database-postgresql", "testcontainers-bom");
        assertThat(managedArtifactIds(project))
                .doesNotContain(
                        "spring-boot-dependencies",
                        "testcontainers-bom",
                        "flyway-core",
                        "flyway-database-postgresql");
    }

    private static List<String> versionProperties(Path pom) throws Exception {
        List<String> properties = new ArrayList<>();
        Element project = document(pom).getDocumentElement();
        collectVersionProperties(project, properties);
        return properties;
    }

    private static void collectVersionProperties(Element parent, List<String> properties) {
        for (Element child : directChildren(parent, "dependency")) {
            recordVersionProperty(child, properties);
        }
        for (Element child : directChildren(parent, "plugin")) {
            recordVersionProperty(child, properties);
        }
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element element) {
                collectVersionProperties(element, properties);
            }
        }
    }

    private static void recordVersionProperty(Element declaration, List<String> properties) {
        if (directChildren(declaration, "version").isEmpty()) {
            return;
        }
        String version = directText(declaration, "version");
        if (!version.startsWith("${") || !version.endsWith("}")) {
            return;
        }
        String property = version.substring(2, version.length() - 1);
        if (!IGNORED_VERSION_PROPERTIES.contains(property)) {
            properties.add(property);
        }
    }

    private static List<String> managedArtifactIds(Element container) {
        return directChildren(container, "dependencyManagement").stream()
                .flatMap(management -> directChildren(management, "dependencies").stream())
                .flatMap(dependencies -> directChildren(dependencies, "dependency").stream())
                .map(dependency -> directText(dependency, "artifactId"))
                .toList();
    }

    private static List<Path> reactorPoms(Path rootPom) throws Exception {
        List<Path> pomFiles = new ArrayList<>();
        pomFiles.add(rootPom);
        for (String module : subprojects(rootPom)) {
            Path modulePom = rootPom.getParent().resolve(module).resolve("pom.xml");
            pomFiles.add(modulePom);
            for (String child : subprojects(modulePom)) {
                pomFiles.add(modulePom.getParent().resolve(child).resolve("pom.xml"));
            }
        }
        return pomFiles;
    }

    private static List<String> subprojects(Path pom) throws Exception {
        return directChildren(document(pom).getDocumentElement(), "subprojects").stream()
                .flatMap(subprojects -> directChildren(subprojects, "subproject").stream())
                .map(Element::getTextContent)
                .map(String::strip)
                .toList();
    }

    private static Path rootPom() {
        return Path.of("..").toAbsolutePath().normalize().resolve("pom.xml");
    }

    private static org.w3c.dom.Document document(Path pom) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(pom.toFile());
    }

    private static String directText(Element parent, String name) {
        return directChildren(parent, name).getFirst().getTextContent().strip();
    }

    private static List<Element> directChildren(Element parent, String name) {
        NodeList children = parent.getChildNodes();
        List<Element> matches = new ArrayList<>();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element element && name.equals(element.getLocalName())) {
                matches.add(element);
            }
        }
        return matches;
    }
}
