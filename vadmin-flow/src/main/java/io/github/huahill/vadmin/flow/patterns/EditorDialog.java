package io.github.huahill.vadmin.flow.patterns;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import java.util.Objects;

/** A modal editor surface with responsive fields, validation feedback, and standard commands. */
public final class EditorDialog extends AdminDialog {

    private final FormLayout form = new FormLayout();
    private final Div validation = new Div();
    private final String titleKey;
    private final String primaryActionKey;

    public EditorDialog(String title, String primaryActionLabel, Runnable onPrimaryAction) {
        this(title, primaryActionLabel, onPrimaryAction, false);
    }

    private EditorDialog(String titleKey, String primaryActionKey, Runnable onPrimaryAction, boolean translated) {
        super(translated, new Button(), onPrimaryAction);
        this.titleKey = Objects.requireNonNull(titleKey);
        this.primaryActionKey = Objects.requireNonNull(primaryActionKey);
        form.setAutoResponsive(true);
        form.setColumnWidth("18rem");
        form.setExpandFields(true);
        validation.getElement().setAttribute("role", "alert");
        validation.setVisible(false);
        primaryAction().addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add(form, validation);
        updateSharedText();
    }

    public static EditorDialog translated(String titleKey, String primaryActionKey, Runnable onPrimaryAction) {
        return new EditorDialog(titleKey, primaryActionKey, onPrimaryAction, true);
    }

    public FormLayout getForm() { return form; }
    public void addField(Component... fields) { form.add(fields); }
    public void addFormRow(Component... fields) { form.addFormRow(fields); }
    public Button getPrimaryAction() { return primaryAction(); }
    public Button getCancelAction() { return cancelAction(); }

    public String getValidationMessage() { return validation.getText(); }
    public void showValidationMessage(String message) {
        validation.setText(Objects.requireNonNull(message));
        validation.setVisible(!message.isBlank());
    }

    @Override
    String resolveTitle() { return text(titleKey); }

    @Override
    String resolvePrimaryActionLabel() { return text(primaryActionKey); }
}
