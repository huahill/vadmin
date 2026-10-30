package io.github.huahill.vadmin.flow.patterns;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import java.util.Objects;

/** Presents non-blocking administration feedback through Vaadin's native notification component. */
public final class AdminNotifications {
    private static final int SUCCESS_DURATION_MS = 3_000;
    private static final int WARNING_DURATION_MS = 4_000;
    private static final int ERROR_DURATION_MS = 5_000;

    private AdminNotifications() {
    }

    /** Shows a successful operation at the shared top-centre location. */
    public static void success(String message) {
        success(message, SUCCESS_DURATION_MS);
    }

    /** Shows a successful operation at the shared top-centre location for the requested duration. */
    public static void success(String message, int durationMs) {
        showSuccess(message, durationMs);
    }

    /** Shows a warning at the shared top-centre location. */
    public static void warning(String message) {
        showWarning(message);
    }

    /** Shows an error at the shared top-centre location. */
    public static void error(String message) {
        showError(message);
    }

    static Notification showSuccess(String message, int durationMs) {
        return show(message, durationMs, NotificationVariant.SUCCESS, VaadinIcon.CHECK_CIRCLE);
    }

    static Notification showWarning(String message) {
        return show(message, WARNING_DURATION_MS, NotificationVariant.WARNING, VaadinIcon.EXCLAMATION_CIRCLE);
    }

    static Notification showError(String message) {
        return show(message, ERROR_DURATION_MS, NotificationVariant.ERROR, VaadinIcon.WARNING);
    }

    private static Notification show(String message, int durationMs, NotificationVariant variant, VaadinIcon icon) {
        return StatusNotification.show(Objects.requireNonNull(message), durationMs, variant, icon);
    }
}
