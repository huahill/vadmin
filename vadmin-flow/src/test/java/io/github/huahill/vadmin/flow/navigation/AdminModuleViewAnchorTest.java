package io.github.huahill.vadmin.flow.navigation;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;

class AdminModuleViewAnchorTest {

    @Test
    void keepsTheAnchorOutOfRuntimeListenerDiscovery() throws Exception {
        var serviceInit = AdminModuleViewAnchor.class.getMethod("serviceInit", ServiceInitEvent.class);

        assertThat(Modifier.isAbstract(AdminModuleViewAnchor.class.getModifiers())).isTrue();
        assertThat(Modifier.isFinal(serviceInit.getModifiers())).isTrue();
        assertThat(ServiceLoader.load(VaadinServiceInitListener.class).stream()
                .map(ServiceLoader.Provider::type)
                .filter(AdminModuleViewAnchor.class::isAssignableFrom)).isEmpty();

        new AnchoredView().serviceInit(null);
    }

    @Test
    void exposesADynamicViewFrontendModuleOnlyFromTheAnchorSubtype() {
        var anchored = scan(AnchoredView.class);
        var plain = scan(PlainUses.class);

        assertThat(modules(anchored)).anyMatch(module -> module.contains("vaadin-text-area.js"));
        assertThat(anchored.getClasses()).contains(TextArea.class.getName(), AnchoredView.class.getName());
        assertThat(plain.getClasses()).doesNotContain(TextArea.class.getName(), PlainUses.class.getName());
    }

    private static FrontendDependencies scan(Class<?> type) {
        return new FrontendDependencies(new ClassFinder.DefaultClassFinder(Set.of(type)), false,
                new FeatureFlags(emptyLookup()), false);
    }

    private static List<String> modules(FrontendDependencies dependencies) {
        return dependencies.getModules().values().stream().flatMap(Collection::stream).toList();
    }

    private static Lookup emptyLookup() {
        return new Lookup() {
            @Override
            public <T> T lookup(Class<T> serviceClass) {
                return null;
            }

            @Override
            public <T> Collection<T> lookupAll(Class<T> serviceClass) {
                return List.of();
            }
        };
    }

    @Uses(TextArea.class)
    public static final class AnchoredView extends AdminModuleViewAnchor {
    }

    @Uses(TextArea.class)
    public static final class PlainUses {
    }
}
