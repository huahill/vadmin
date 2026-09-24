package io.github.huahill.vadmin.flow.patterns;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import java.util.Objects;

/**
 * Shared base for VAdmin modal dialogs: footer action bar, a primary action button, a cancel
 * action button with overridable label, the translated/plain text convention, and busy state
 * management that suspends commands and closing until released.
 *
 * <p>Subclasses contribute content, the primary button variant, and title resolution through
 * {@link #resolveTitle()}.</p>
 */
abstract class AdminDialog extends Dialog implements LocaleChangeObserver {

    private final Button primaryAction;
    private final Button cancelAction;
    private final boolean translated;

    private String cancelActionLabel;
    private boolean busy;
    private boolean primaryEnabledBeforeBusy;
    private boolean cancelEnabledBeforeBusy;
    private boolean closeOnEscBeforeBusy;
    private boolean closeOnOutsideClickBeforeBusy;

    AdminDialog(boolean translated, Button primaryAction, Runnable onPrimaryAction) {
        this.translated = translated;
        this.primaryAction = Objects.requireNonNull(primaryAction);
        if (onPrimaryAction != null) {
            this.primaryAction.addClickListener(event -> onPrimaryAction.run());
        }
        this.cancelAction = new Button();
        this.cancelAction.addClickListener(event -> close());
        var footerButtons = new java.util.ArrayList<Button>();
        footerButtons.add(cancelAction);
        if (primaryAction.isVisible()) {
            footerButtons.add(primaryAction);
        }
        var footerActions = new HorizontalLayout(footerButtons.toArray(new Button[0]));
        footerActions.setPadding(false);
        footerActions.setSpacing(true);
        footerActions.setWidthFull();
        footerActions.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);
        footerActions.setWrap(true);
        getFooter().add(footerActions);
    }

    final Button primaryAction() { return primaryAction; }
    final Button cancelAction() { return cancelAction; }
    final boolean isTranslated() { return translated; }

    final String text(String value) { return translated ? getTranslation(value) : value; }

    /** Overrides the default cancel label while retaining the shared dialog action layout. */
    final void setCancelActionLabel(String label) {
        cancelActionLabel = Objects.requireNonNull(label);
        cancelAction.setText(cancelActionLabel);
    }

    final boolean isBusy() { return busy; }

    /** Temporarily disables commands and closing, then restores their previous policies. */
    final void setBusy(boolean value) {
        if (busy == value) return;
        busy = value;
        if (value) {
            primaryEnabledBeforeBusy = primaryAction.isEnabled();
            cancelEnabledBeforeBusy = cancelAction.isEnabled();
            closeOnEscBeforeBusy = isCloseOnEsc();
            closeOnOutsideClickBeforeBusy = isCloseOnOutsideClick();
            primaryAction.setEnabled(false);
            cancelAction.setEnabled(false);
            setCloseOnEsc(false);
            setCloseOnOutsideClick(false);
        } else {
            primaryAction.setEnabled(primaryEnabledBeforeBusy);
            cancelAction.setEnabled(cancelEnabledBeforeBusy);
            setCloseOnEsc(closeOnEscBeforeBusy);
            setCloseOnOutsideClick(closeOnOutsideClickBeforeBusy);
        }
    }

    final void updateSharedText() {
        var title = resolveTitle();
        setHeaderTitle(title);
        getElement().setAttribute("aria-label", title);
        primaryAction.setText(resolvePrimaryActionLabel());
        cancelAction.setText(cancelActionLabel != null ? cancelActionLabel
                : translated ? getTranslation("flow.action.cancel") : "Cancel");
    }

    @Override
    public final void localeChange(com.vaadin.flow.i18n.LocaleChangeEvent event) {
        updateSharedText();
        onLocaleChange(event);
    }

    /** Subclass hook for locale-specific content updates beyond the shared title and actions. */
    void onLocaleChange(com.vaadin.flow.i18n.LocaleChangeEvent event) {
    }

    /** Resolves the dialog header title, honoring the translated/plain convention. */
    abstract String resolveTitle();

    /** Resolves the primary action button label, honoring the translated/plain convention. */
    abstract String resolvePrimaryActionLabel();
}
