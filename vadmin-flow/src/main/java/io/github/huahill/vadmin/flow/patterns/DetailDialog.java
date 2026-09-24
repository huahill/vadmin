package io.github.huahill.vadmin.flow.patterns;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.DescriptionList;
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
        form.setAutoResponsive(true);
        form.setColumnWidth("18rem");
        form.setExpandFields(true);
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
    public static final class DetailField extends DescriptionList {
        private final Term term;
        private final Description description;

        private DetailField(String label, String value) {
            term = new Term(Objects.requireNonNull(label));
            description = new Description(Objects.requireNonNull(value));
            getStyle().set("margin", "0");
            description.getStyle().set("margin-inline-start", "0");
            description.getStyle().set("overflow-wrap", "anywhere");
            add(term, description);
        }

        public String getLabel() { return term.getText(); }
        public String getValue() { return description.getText(); }
    }

    public DetailField addField(String label, String value) {
        var field = new DetailField(label, value);
        form.add(field);
        return field;
    }

    public void addFormRow(DetailField... fields) { form.addFormRow(fields); }

    public Button getCloseAction() { return cancelAction(); }

    @Override
    String resolveTitle() { return text(titleKey); }

    @Override
    String resolvePrimaryActionLabel() { return ""; }
}
