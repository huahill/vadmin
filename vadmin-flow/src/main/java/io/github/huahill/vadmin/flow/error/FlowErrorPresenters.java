package io.github.huahill.vadmin.flow.error;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Host-replaceable presenter used by the Vaadin session error handler.
 * Install during service initialization, before any session is created.
 */
public final class FlowErrorPresenters {
    private static final AtomicReference<FlowErrorPresenter> INSTALLED = new AtomicReference<>();

    private FlowErrorPresenters() {
    }

    public static void install(FlowErrorPresenter presenter) {
        INSTALLED.set(Objects.requireNonNull(presenter));
    }

    public static FlowErrorPresenter resolve() {
        var installed = INSTALLED.get();
        return installed != null ? installed : vaadin();
    }

    public static FlowErrorPresenter vaadin() {
        return new VaadinFlowErrorPresenter();
    }

    static void reset() {
        INSTALLED.set(null);
    }
}
