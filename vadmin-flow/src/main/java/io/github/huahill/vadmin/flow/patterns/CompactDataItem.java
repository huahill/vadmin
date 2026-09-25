package io.github.huahill.vadmin.flow.patterns;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.DescriptionList;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.Objects;

/** A compact, semantic entity row for narrow data workspaces. */
public final class CompactDataItem extends VerticalLayout {
    private final Span primaryText = new Span();
    private final Span status = new Span();
    private final HorizontalLayout metadata = new HorizontalLayout();
    private final HorizontalLayout actions = new HorizontalLayout();
    private boolean selectionMode;

    public CompactDataItem(String primaryText) {
        this.primaryText.setText(Objects.requireNonNull(primaryText));
        this.primaryText.getStyle().set("font-weight", "600").set("overflow-wrap", "anywhere");
        status.getStyle().set("font-size", "var(--vaadin-font-size-s)")
                .set("color", "var(--vaadin-text-color-secondary)");
        status.setVisible(false);

        var header = new HorizontalLayout(this.primaryText, status);
        header.setWidthFull();
        header.setPadding(false);
        header.setSpacing(true);
        header.setWrap(true);
        header.setAlignItems(Alignment.BASELINE);
        header.setFlexGrow(1, this.primaryText);
        header.setFlexShrink(0, status);

        metadata.setWidthFull();
        metadata.setPadding(false);
        metadata.setSpacing(true);
        metadata.setWrap(true);
        metadata.setVisible(false);

        actions.setWidthFull();
        actions.setPadding(false);
        actions.setSpacing(true);
        actions.setWrap(true);
        actions.setVisible(false);

        setWidthFull();
        setPadding(true);
        setSpacing(true);
        getStyle().set("border-bottom", "1px solid var(--vaadin-border-color-secondary)");
        add(header, metadata, actions);
    }

    public String getPrimaryText() {
        return primaryText.getText();
    }

    public void setStatus(String value) {
        status.setText(Objects.requireNonNull(value));
        status.setVisible(!value.isBlank());
    }

    public String getStatus() {
        return status.getText();
    }

    /** Adds one semantic label-value pair to the wrapping metadata row. */
    public DescriptionList addMetadata(String label, String value) {
        var field = new DescriptionList();
        var term = new DescriptionList.Term(Objects.requireNonNull(label));
        var description = new DescriptionList.Description(Objects.requireNonNull(value));
        field.getStyle().set("display", "inline-flex")
                .set("gap", "var(--vaadin-gap-xs)")
                .set("margin", "0")
                .set("font-size", "var(--vaadin-font-size-s)")
                .set("color", "var(--vaadin-text-color-secondary)");
        description.getStyle().set("margin", "0");
        description.getStyle().set("overflow-wrap", "anywhere");
        field.add(term, description);
        metadata.add(field);
        metadata.setVisible(true);
        return field;
    }

    public void addActions(Component... commands) {
        actions.add(commands);
        updateActionsVisibility();
    }

    /** Hides row-level commands while the containing workspace is selecting entities. */
    public void setSelectionMode(boolean selectionMode) {
        this.selectionMode = selectionMode;
        updateActionsVisibility();
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public HorizontalLayout getMetadata() {
        return metadata;
    }

    public HorizontalLayout getActions() {
        return actions;
    }

    private void updateActionsVisibility() {
        actions.setVisible(actions.getComponentCount() > 0 && !selectionMode);
    }
}
