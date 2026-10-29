package io.github.huahill.vadmin.flow.navigation;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;

/**
 * Classpath entry point for production frontend scanning of dynamic administration views.
 *
 * <p>Flow follows {@code @Uses} only after it visits an entry point. A
 * {@link VaadinServiceInitListener} subtype is an entry point; a Spring Boot application class is
 * not, and neither is a view registered only through {@link AdminModule}. Subclass this type and
 * declare one {@code @Uses} on the subclass for each such view. Flow discovers the subclass by
 * type, so do not register it as a Spring bean or through {@code META-INF/services}.
 * {@link #serviceInit(ServiceInitEvent)} is empty and final so the anchor cannot become a runtime
 * hook.
 */
public abstract class AdminModuleViewAnchor implements VaadinServiceInitListener {

    @Override
    public final void serviceInit(ServiceInitEvent event) {
    }
}
