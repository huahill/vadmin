package io.github.huahill.vadmin.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
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
import io.github.huahill.vadmin.flow.patterns.EditorDialog;
import io.github.huahill.vadmin.flow.patterns.OperationFeedback;
import io.github.huahill.vadmin.flow.patterns.PageHeader;
import io.github.huahill.vadmin.flow.patterns.PageToolbar;
import io.github.huahill.vadmin.platform.access.ExternalGrantStore;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import java.util.stream.Collectors;
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
    private final TextField query = new TextField();
    private final Grid<ExternalPerson> people = new Grid<>();
    private final Grid<ExternalGrantStore.ExternalGrant> assigned = new Grid<>();
    private final OperationFeedback feedback = new OperationFeedback();
    private final Button searchAction;

    public ExternalGrantsView(CurrentUserProvider currentUser, AuthorizationService authorization,
                              ExternalGrantService grants, AdministrationQueryService roles) {
        super(currentUser, authorization);
        this.grants = grants;
        this.roles = roles;
        query.setClearButtonVisible(true);
        people.addColumn(ExternalPerson::username).setAutoWidth(true);
        people.addColumn(ExternalPerson::displayName).setAutoWidth(true);
        people.addComponentColumn(person -> grantAction(person, authorization));
        people.setAllRowsVisible(true);
        assigned.addColumn(ExternalGrantStore.ExternalGrant::label).setAutoWidth(true);
        assigned.addColumn(grant -> grant.subject().value()).setAutoWidth(true);
        assigned.addColumn(grant -> String.join(", ", grant.roleCodes())).setAutoWidth(true);
        assigned.setItems(grants.grants());
        assigned.setAllRowsVisible(true);
        var header = PageHeader.translated("system.external-grants.title", "system.external-grants.intent");
        var toolbar = new PageToolbar();
        toolbar.addFilter(query);
        searchAction = new Button(AdminIcon.of(AdminIconName.USERS), event -> search());
        toolbar.setPrimaryAction(searchAction);
        var workspace = new VerticalLayout(people, assigned);
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
        query.setLabel(getTranslation("system.external-grants.search"));
        searchAction.setAriaLabel(getTranslation("system.external-grants.search-action"));
        searchAction.setTooltipText(getTranslation("system.external-grants.search-action"));
    }

    private void search() {
        try {
            people.setItems(grants.search(query.getValue()));
        } catch (RuntimeException failure) {
            feedback.error(getTranslation("system.external-grants.search-failed"));
        }
    }

    private HorizontalLayout grantAction(ExternalPerson person, AuthorizationService authorization) {
        var grant = new Button(AdminIcon.of(AdminIconName.EDIT), event -> chooseRoles(person));
        grant.setVisible(authorization.hasPermission(requireCurrentUser(), ASSIGN));
        grant.setAriaLabel(getTranslation("system.external-grants.grant-aria", person.username()));
        return new HorizontalLayout(grant);
    }

    private void chooseRoles(ExternalPerson person) {
        var choices = new MultiSelectComboBox<AdministrationQueryService.RoleRow>(
                getTranslation("system.external-grants.roles"));
        List<AdministrationQueryService.RoleRow> available = roles.roles();
        choices.setItems(available);
        choices.setItemLabelGenerator(AdministrationQueryService.RoleRow::code);
        var dialog = EditorDialog.translated("system.external-grants.grant", "system.users.save", () -> { });
        dialog.getPrimaryAction().addClickListener(event -> {
            try {
                grants.replaceRoles(requireCurrentUser(), person.subject(), person.username(),
                        choices.getValue().stream().map(AdministrationQueryService.RoleRow::id).collect(Collectors.toSet()));
                dialog.close();
                assigned.setItems(grants.grants());
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
}
