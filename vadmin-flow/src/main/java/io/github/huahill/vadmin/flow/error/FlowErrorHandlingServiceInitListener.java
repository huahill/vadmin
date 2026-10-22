package io.github.huahill.vadmin.flow.error;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;

public final class FlowErrorHandlingServiceInitListener implements VaadinServiceInitListener {
    @Override
    public void serviceInit(ServiceInitEvent event) {
        // The handler resolves the presenter when the session starts, after service init can replace it.
        event.getSource().addSessionInitListener(sessionEvent ->
                sessionEvent.getSession().setErrorHandler(new FlowErrorHandler()));
    }
}
