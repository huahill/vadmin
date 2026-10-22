package io.github.huahill.vadmin.springflow;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import io.github.huahill.vadmin.flow.error.FlowErrorPresenter;
import io.github.huahill.vadmin.flow.error.FlowErrorPresenters;

/**
 * Installs the presenter during service initialization, before Vaadin creates a session.
 * The service-loader listener resolves it when that session's error handler is created.
 */
final class FlowErrorPresenterInstallation implements VaadinServiceInitListener {
    private final FlowErrorPresenter presenter;

    FlowErrorPresenterInstallation(FlowErrorPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        install();
    }

    void install() {
        FlowErrorPresenters.install(presenter);
    }
}
