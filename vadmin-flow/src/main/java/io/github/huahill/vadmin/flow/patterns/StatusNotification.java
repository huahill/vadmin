package io.github.huahill.vadmin.flow.patterns;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * Opens Vaadin's documented status notification: a theme-neutral variant and the matching status icon.
 * Aura colors that icon through the notification theme; it does not appear for a text-only notification.
 */
final class StatusNotification {
    private StatusNotification() {
    }

    static Notification show(String message, int durationMs, NotificationVariant variant, VaadinIcon icon) {
        var notification = new Notification();
        notification.addThemeVariants(variant);
        notification.setPosition(Notification.Position.TOP_CENTER);
        notification.setDuration(durationMs);
        var content = new HorizontalLayout(icon.create(), new Text(message));
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        notification.add(content);
        notification.open();
        return notification;
    }
}
