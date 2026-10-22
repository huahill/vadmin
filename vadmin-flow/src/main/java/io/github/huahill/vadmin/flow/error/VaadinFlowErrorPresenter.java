package io.github.huahill.vadmin.flow.error;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import io.github.huahill.vadmin.flow.patterns.AdminNotifications;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

final class VaadinFlowErrorPresenter implements FlowErrorPresenter {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final String SYSTEM_FAILURE_NOTIFICATION = "flow.system-failure.notification";

    // The notifier can be a test double; a replicated Flow session always restores the shared notification.
    private transient Consumer<String> errorNotifier;

    VaadinFlowErrorPresenter() {
        this(AdminNotifications::error);
    }

    VaadinFlowErrorPresenter(Consumer<String> errorNotifier) {
        this.errorNotifier = Objects.requireNonNull(errorNotifier);
    }

    @Serial
    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        errorNotifier = AdminNotifications::error;
    }

    @Override
    public void present(FlowError error) {
        var ui = UI.getCurrent();
        if (ui == null) {
            return;
        }
        switch (error.presentation()) {
            case FIELD_VALIDATION -> Notification.show(ui.getTranslation("flow.error.validation"));
            case ACCESS_DENIED -> ui.navigate(AccessDeniedView.class);
            case FAILURE -> errorNotifier.accept(failureMessage(ui, error.messageKey()));
        }
    }

    private static String failureMessage(UI ui, String messageKey) {
        if (messageKey == null || messageKey.isBlank() || "internal.error".equals(messageKey)) {
            return ui.getTranslation(SYSTEM_FAILURE_NOTIFICATION);
        }
        var translated = ui.getTranslation(messageKey);
        if (isUnresolved(translated, messageKey, ui.getLocale())) {
            return ui.getTranslation(SYSTEM_FAILURE_NOTIFICATION);
        }
        return translated;
    }

    private static boolean isUnresolved(String translated, String messageKey, Locale locale) {
        if (translated == null || translated.isBlank() || translated.equals(messageKey)) {
            return true;
        }
        if (("!" + messageKey + "!").equals(translated)) {
            return true;
        }
        var languageTag = locale == null ? "" : locale.toLanguageTag();
        return ("!" + languageTag + ": " + messageKey + "!").equals(translated);
    }
}
