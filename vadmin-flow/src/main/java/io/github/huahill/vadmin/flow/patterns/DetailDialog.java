package io.github.huahill.vadmin.flow.patterns;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import java.util.Objects;

/** A read-only, responsive dialog for presenting an entity's already-authorized details. */
public final class DetailDialog extends AdminDialog {

    private final FormLayout form = new FormLayout();
    private final String titleKey;

    public DetailDialog(String title) { this(title, false); }

    private DetailDialog(String titleKey, boolean translated) {
        // Detail dialog has no primary action; pass an invisible button so the footer
        // only shows the close (cancel) button.
        super(translated, invisibleButton(), null);
        this.titleKey = Objects.requireNonNull(titleKey);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("40em", 2));
        add(form);
        updateSharedText();
    }

    public static DetailDialog translated(String titleKey) { return new DetailDialog(titleKey, true); }

    private static Button invisibleButton() {
        var button = new Button();
        button.setVisible(false);
        return button;
    }

    public FormLayout getForm() { return form; }

    /** One semantic term-description pair in the responsive detail surface. */
    public static final class DetailField extends FormLayout.FormItem {
        private final NativeLabel label;
        private final Span value;

        private DetailField(String label, String value) {
            this.label = new NativeLabel(Objects.requireNonNull(label));
            this.value = new Span(Objects.requireNonNull(value));
            this.value.setWidthFull();
            this.value.getStyle().set("overflow-wrap", "anywhere");
            this.value.getStyle().set("white-space", "pre-wrap");
            add(this.value);
            addToLabel(this.label);
        }

        public String getLabel() { return label.getText(); }
        public String getValue() { return value.getText(); }
    }

    public DetailField addField(String label, String value) {
        var field = new DetailField(label, value);
        form.add(field);
        return field;
    }

    /** Adds a long value on its own row across all detail columns. */
    public DetailField addFullWidthField(String label, String value) {
        var field = new DetailField(label, value);
        var row = form.addFormRow(field);
        row.setColspan(field, 2);
        return field;
    }

    public void addFormRow(DetailField... fields) { form.addFormRow(fields); }

    public Button getCloseAction() { return cancelAction(); }

    @Override
    String resolveTitle() { return text(titleKey); }

    @Override
    String resolvePrimaryActionLabel() { return ""; }
}
