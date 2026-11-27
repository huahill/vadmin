package io.github.huahill.vadmin.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import io.github.huahill.vadmin.administration.AdministrationQueryService;
import io.github.huahill.vadmin.administration.ExternalGrantService;
import io.github.huahill.vadmin.contracts.auth.AuthorizationService;
import io.github.huahill.vadmin.contracts.auth.CurrentUserProvider;
import io.github.huahill.vadmin.contracts.auth.ExternalPerson;
import io.github.huahill.vadmin.contracts.auth.PermissionCode;
import io.github.huahill.vadmin.contracts.error.BusinessFailure;
import io.github.huahill.vadmin.flow.navigation.AdminIcon;
import io.github.huahill.vadmin.flow.navigation.AdminIconName;
import io.github.huahill.vadmin.flow.navigation.PermissionProtectedView;
import io.github.huahill.vadmin.flow.patterns.AdminPageFrame;
import io.github.huahill.vadmin.flow.patterns.ConfirmationDialog;
import io.github.huahill.vadmin.flow.patterns.EditorDialog;
import io.github.huahill.vadmin.flow.patterns.OperationFeedback;
import io.github.huahill.vadmin.flow.patterns.PageHeader;
import io.github.huahill.vadmin.flow.patterns.PageToolbar;
import io.github.huahill.vadmin.platform.access.ExternalGrantStore;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Lets an administrator grant VAdmin roles to people from the external directory. */
@PermitAll
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@ConditionalOnProperty(prefix = "vadmin.identity", name = "mode", havingValue = "external")
public final class ExternalGrantsView extends PermissionProtectedView implements LocaleChangeObserver, HasDynamicTitle {
    public static final PermissionCode REQUIRED_PERMISSION = PermissionCode.of("system:user:read");
    private static final PermissionCode ASSIGN = PermissionCode.of("system:user:roles");

    private final ExternalGrantService grants;
    private final AdministrationQueryService roles;
    private final Grid<ExternalGrantStore.ExternalGrant> assigned = new Grid<>();
    private final OperationFeedback feedback = new OperationFeedback();
    private final Button addAction;

    public ExternalGrantsView(CurrentUserProvider currentUser, AuthorizationService authorization,
                              ExternalGrantService grants, AdministrationQueryService roles) {
        super(currentUser, authorization);
        this.grants = grants;
        this.roles = roles;
        assigned.addColumn(ExternalGrantStore.ExternalGrant::label).setHeader("").setAutoWidth(true);
        assigned.addColumn(grant -> grant.subject().value()).setHeader("").setAutoWidth(true);
        assigned.addColumn(grant -> String.join(", ", grant.roleCodes())).setHeader("").setAutoWidth(true);
        assigned.addComponentColumn(grant -> assignedActions(grant, authorization)).setAutoWidth(true);
        refreshAssigned();
        assigned.setAllRowsVisible(true);
        var header = PageHeader.translated("system.external-grants.title", "system.external-grants.intent");
        var toolbar = new PageToolbar();
        addAction = new Button(AdminIcon.of(AdminIconName.ADD), event -> openAdd());
        addAction.setVisible(authorization.hasPermission(requireCurrentUser(), ASSIGN));
        toolbar.setPrimaryAction(addAction);
        var workspace = new VerticalLayout(assigned);
        workspace.setPadding(false);
        workspace.setSizeFull();
        var frame = new AdminPageFrame(header, toolbar, workspace);
        add(frame);
        expand(frame);
        updateText();
    }

    @Override
    protected PermissionCode requiredPermission() {
        return REQUIRED_PERMISSION;
    }

    @Override
    public void localeChange(LocaleChangeEvent event) {
        updateText();
    }

    @Override
    public String getPageTitle() {
        return getTranslation("system.external-grants.title");
    }

    private void updateText() {
        addAction.setText(getTranslation("system.external-grants.add"));
        addAction.setAriaLabel(getTranslation("system.external-grants.add"));
        assigned.getColumns().get(0).setHeader(getTranslation("system.external-grants.person"));
        assigned.getColumns().get(1).setHeader(getTranslation("system.external-grants.subject"));
        assigned.getColumns().get(2).setHeader(getTranslation("system.external-grants.roles"));
    }

    private void openAdd() {
        var people = new ComboBox<ExternalPerson>(getTranslation("system.external-grants.search"));
        people.setItemLabelGenerator(ExternalGrantsView::personLabel);
        people.setAllowCustomValue(false);
        people.setClearButtonVisible(true);
        people.setPageSize(20);
        people.setWidthFull();
        var choices = new MultiSelectComboBox<AdministrationQueryService.RoleRow>(
                getTranslation("system.external-grants.roles"));
        List<AdministrationQueryService.RoleRow> available = roles.roles();
        choices.setItems(available);
        choices.setItemLabelGenerator(AdministrationQueryService.RoleRow::code);
        choices.setWidthFull();
        var dialog = EditorDialog.translated("system.external-grants.add", "system.users.save", () -> { });
        dialog.getPrimaryAction().setEnabled(false);
        people.setItems(query -> searchPeople(dialog, query));
        people.addValueChangeListener(event -> updateAddAvailability(dialog, people, choices));
        choices.addValueChangeListener(event -> updateAddAvailability(dialog, people, choices));
        dialog.getPrimaryAction().addClickListener(event -> {
            ExternalPerson person = people.getValue();
            if (person == null || choices.getValue().isEmpty()) {
                dialog.showValidationMessage(getTranslation("system.external-grants.add-required"));
                return;
            }
            try {
                grants.replaceRoles(requireCurrentUser(), person.subject(), person.username(),
                        choices.getValue().stream().map(AdministrationQueryService.RoleRow::id).collect(Collectors.toSet()));
                dialog.close();
                refreshAssigned();
                feedback.success(getTranslation("system.external-grants.saved"));
            } catch (BusinessFailure failure) {
                ViewBusinessFailureHandler.handle(failure, validation -> dialog.showValidationMessage(
                        getTranslation("system.external-grants.invalid")));
            }
        });
        dialog.addField(people, choices);
        dialog.open();
    }

    private Stream<ExternalPerson> searchPeople(EditorDialog dialog, Query<ExternalPerson, String> query) {
        int limit = query.getLimit();
        int offset = query.getOffset();
        String filter = query.getFilter().orElse("").strip();
        if (offset > 0 || filter.isBlank() || limit <= 0) {
            return Stream.empty();
        }
        try {
            dialog.showValidationMessage("");
            return grants.search(filter).stream().limit(limit);
        } catch (RuntimeException failure) {
            dialog.showValidationMessage(getTranslation("system.external-grants.search-failed"));
            return Stream.empty();
        }
    }

    private static String personLabel(ExternalPerson person) {
        String name = person.displayName();
        if (name == null || name.isBlank() || name.equals(person.username())) {
            return person.username();
        }
        return name + " (" + person.username() + ")";
    }

    private void updateAddAvailability(EditorDialog dialog, ComboBox<ExternalPerson> people,
                                       MultiSelectComboBox<AdministrationQueryService.RoleRow> choices) {
        dialog.getPrimaryAction().setEnabled(people.getValue() != null && !choices.getValue().isEmpty());
    }

    private HorizontalLayout assignedActions(ExternalGrantStore.ExternalGrant grant, AuthorizationService authorization) {
        var person = new ExternalPerson(grant.subject(), grant.label(), grant.label());
        var actions = grantAction(person, authorization, Set.copyOf(grant.roleCodes()));
        var remove = new Button(getTranslation("system.external-grants.remove"), event -> confirmRemove(grant));
        remove.setVisible(authorization.hasPermission(requireCurrentUser(), ASSIGN));
        remove.setAriaLabel(getTranslation("system.external-grants.remove-aria", grant.label()));
        actions.add(remove);
        return actions;
    }

    private HorizontalLayout grantAction(ExternalPerson person, AuthorizationService authorization, Set<String> currentRoles) {
        var grant = new Button(getTranslation("system.external-grants.grant"), AdminIcon.of(AdminIconName.EDIT),
                event -> chooseRoles(person, currentRoles));
        grant.setVisible(authorization.hasPermission(requireCurrentUser(), ASSIGN));
        grant.setAriaLabel(getTranslation("system.external-grants.grant-aria", person.username()));
        return new HorizontalLayout(grant);
    }

    private void chooseRoles(ExternalPerson person, Set<String> currentRoles) {
        var choices = new MultiSelectComboBox<AdministrationQueryService.RoleRow>(
                getTranslation("system.external-grants.roles"));
        List<AdministrationQueryService.RoleRow> available = roles.roles();
        choices.setItems(available);
        choices.setItemLabelGenerator(AdministrationQueryService.RoleRow::code);
        choices.setValue(available.stream().filter(role -> currentRoles.contains(role.code())).collect(Collectors.toSet()));
        var dialog = EditorDialog.translated("system.external-grants.grant", "system.users.save", () -> { });
        dialog.getPrimaryAction().addClickListener(event -> {
            try {
                grants.replaceRoles(requireCurrentUser(), person.subject(), person.username(),
                        choices.getValue().stream().map(AdministrationQueryService.RoleRow::id).collect(Collectors.toSet()));
                dialog.close();
                refreshAssigned();
                feedback.success(getTranslation("system.external-grants.saved"));
            } catch (BusinessFailure failure) {
                ViewBusinessFailureHandler.handle(failure, validation -> dialog.showValidationMessage(
                        getTranslation(validation.fieldErrors().containsValue("last-administrator")
                                ? "system.users.last-administrator" : "system.external-grants.invalid")));
            }
        });
        dialog.addField(choices);
        dialog.open();
    }

    private void confirmRemove(ExternalGrantStore.ExternalGrant grant) {
        var confirmation = ConfirmationDialog.translated("system.external-grants.remove-title",
                "system.external-grants.remove-message", "system.external-grants.remove", () -> { });
        confirmation.getConfirmAction().addClickListener(event -> {
            try {
                grants.remove(requireCurrentUser(), grant.subject());
            } catch (BusinessFailure failure) {
                ViewBusinessFailureHandler.handle(failure, validation -> confirmation.showFailureMessage(getTranslation(
                        validation.fieldErrors().containsValue("last-administrator")
                                ? "system.users.last-administrator" : "system.external-grants.invalid")));
                return;
            }
            confirmation.close();
            refreshAssigned();
            feedback.success(getTranslation("system.external-grants.removed"));
        });
        confirmation.open();
    }

    private void refreshAssigned() {
        assigned.setItems(grants.grants());
    }
}
