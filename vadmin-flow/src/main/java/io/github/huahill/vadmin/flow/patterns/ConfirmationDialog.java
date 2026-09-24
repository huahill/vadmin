package io.github.huahill.vadmin.flow.patterns;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import java.util.Objects;

/**
 * A confirmation surface that executes a command only from its explicit confirm action.
 * The consequence paragraph and a failure message slot are shown above the shared footer.
 */
public final class ConfirmationDialog extends AdminDialog {

    private final Paragraph consequence = new Paragraph();
    private final Div failure = new Div();
    private final String titleKey;
    private final String consequenceKey;
    private final String confirmActionKey;

    public ConfirmationDialog(String title, String consequence, String confirmActionLabel, Runnable onConfirm) {
        this(title, consequence, confirmActionLabel, onConfirm, false);
    }

    private ConfirmationDialog(String titleKey, String consequenceKey, String confirmActionKey, Runnable onConfirm,
                               boolean translated) {
        super(translated, new Button(), onConfirm);
        this.titleKey = Objects.requireNonNull(titleKey);
        this.consequenceKey = Objects.requireNonNull(consequenceKey);
        this.confirmActionKey = Objects.requireNonNull(confirmActionKey);
        consequence.addClassName("admin-confirmation-consequence");
        failure.getElement().setAttribute("role", "alert");
        failure.setVisible(false);
        add(consequence, failure);
        updateSharedText();
    }

    /** Creates a confirmation surface whose static text follows the active UI locale. */
    public static ConfirmationDialog translated(String titleKey, String consequenceKey, String confirmActionKey,
                                                Runnable onConfirm) {
        return new ConfirmationDialog(titleKey, consequenceKey, confirmActionKey, onConfirm, true);
    }

    public Button getConfirmAction() { return primaryAction(); }
    public Button getCancelAction() { return cancelAction(); }

    public String getFailureMessage() { return failure.getText(); }

    public void showFailureMessage(String message) {
        failure.setText(Objects.requireNonNull(message));
        failure.setVisible(!message.isBlank());
    }

    @Override
    void onLocaleChange(com.vaadin.flow.i18n.LocaleChangeEvent event) {
        consequence.setText(text(consequenceKey));
    }

    @Override
    String resolveTitle() { return text(titleKey); }

    @Override
    String resolvePrimaryActionLabel() { return text(confirmActionKey); }
}
