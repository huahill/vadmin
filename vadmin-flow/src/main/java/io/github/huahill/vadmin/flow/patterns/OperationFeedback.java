package io.github.huahill.vadmin.flow.patterns;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import io.github.huahill.vadmin.contracts.error.BusinessFailure;
import io.github.huahill.vadmin.contracts.error.ErrorCode;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.function.Consumer;

/** Presents successful commands locally while preserving Flow's global failure handling boundary. */
public final class OperationFeedback implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // Presenters can be arbitrary lambdas; restore the built-in variants after a Flow session is deserialized.
    private transient Consumer<String> successPresenter;
    private transient Consumer<String> errorPresenter;

    public OperationFeedback() {
        this(OperationFeedback::showSuccess, OperationFeedback::showError);
    }

    public OperationFeedback(Consumer<String> successPresenter) {
        this(successPresenter, OperationFeedback::showError);
    }

    public OperationFeedback(Consumer<String> successPresenter, Consumer<String> errorPresenter) {
        this.successPresenter = Objects.requireNonNull(successPresenter);
        this.errorPresenter = Objects.requireNonNull(errorPresenter);
    }

    public void success(String message) {
        successPresenter.accept(Objects.requireNonNull(message));
    }

    /** Presents a non-blocking warning using the shared operation-feedback placement. */
    public void warning(String message) {
        showWarning(Objects.requireNonNull(message));
    }

    static Notification showSuccess(String message) {
        return show(message, NotificationVariant.SUCCESS, VaadinIcon.CHECK_CIRCLE);
    }

    public void error(String message) {
        errorPresenter.accept(Objects.requireNonNull(message));
    }

    static Notification showError(String message) {
        return show(message, NotificationVariant.ERROR, VaadinIcon.WARNING);
    }

    static Notification showWarning(String message) {
        return show(message, NotificationVariant.WARNING, VaadinIcon.EXCLAMATION_CIRCLE);
    }

    private static Notification show(String message, NotificationVariant variant, VaadinIcon icon) {
        return StatusNotification.show(message, 5_000, variant, icon);
    }

    @Serial
    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        successPresenter = OperationFeedback::showSuccess;
        errorPresenter = OperationFeedback::showError;
    }

    /**
     * Delegates only validation failures to the local command surface. Every other exception is
     * rethrown unchanged so Flow's global error handler retains ownership.
     */
    public void handleFailure(Throwable failure, Consumer<BusinessFailure> validationHandler) {
        Objects.requireNonNull(failure);
        if (failure instanceof BusinessFailure businessFailure
                && businessFailure.errorCode() == ErrorCode.VALIDATION_FAILED) {
            Objects.requireNonNull(validationHandler).accept(businessFailure);
            return;
        }
        rethrow(failure);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void rethrow(Throwable failure) throws T {
        throw (T) failure;
    }
}
