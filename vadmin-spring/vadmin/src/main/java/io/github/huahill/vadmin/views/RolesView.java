package io.github.huahill.vadmin.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import io.github.huahill.vadmin.administration.AdministrationQueryService;
import io.github.huahill.vadmin.administration.RoleAdministrationService;
import io.github.huahill.vadmin.contracts.error.BusinessFailure;
import io.github.huahill.vadmin.contracts.auth.AuthorizationService;
import io.github.huahill.vadmin.contracts.auth.CurrentUserProvider;
import io.github.huahill.vadmin.contracts.auth.PermissionCatalog;
import io.github.huahill.vadmin.contracts.auth.PermissionCode;
import io.github.huahill.vadmin.flow.patterns.AdminPageFrame;
import io.github.huahill.vadmin.flow.patterns.CompactDataItem;
import io.github.huahill.vadmin.flow.patterns.DataWorkspace;
import io.github.huahill.vadmin.flow.patterns.DetailDialog;
import io.github.huahill.vadmin.flow.patterns.EditorDialog;
import io.github.huahill.vadmin.flow.patterns.OperationFeedback;
import io.github.huahill.vadmin.flow.patterns.PagedGrid;
import io.github.huahill.vadmin.flow.patterns.PageHeader;
import io.github.huahill.vadmin.flow.patterns.PageToolbar;
import io.github.huahill.vadmin.platform.access.GrantPermissionCommand;
import io.github.huahill.vadmin.platform.access.GrantPermissionUseCase;
import io.github.huahill.vadmin.flow.navigation.PermissionProtectedView;
import io.github.huahill.vadmin.flow.navigation.AdminIcon;
import io.github.huahill.vadmin.flow.navigation.AdminIconName;
import io.github.huahill.vadmin.localiam.ConditionalOnSystemAdministration;
import jakarta.annotation.security.PermitAll;
import java.util.Comparator;

@PermitAll
@org.springframework.stereotype.Component
@org.springframework.context.annotation.Scope(org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@ConditionalOnSystemAdministration
public final class RolesView extends PermissionProtectedView implements LocaleChangeObserver, HasDynamicTitle {
    public static final PermissionCode REQUIRED_PERMISSION = PermissionCode.of("system:role:read");
    private static final PermissionCode GRANT = PermissionCode.of("system:role:grant");
    private static final PermissionCode CREATE = PermissionCode.of("system:role:create");
    private final Grid<AdministrationQueryService.RoleRow> grid = new Grid<>(AdministrationQueryService.RoleRow.class, false);
    private final Grid.Column<AdministrationQueryService.RoleRow> codeColumn;
    private final Grid.Column<AdministrationQueryService.RoleRow> permissionCountColumn;
    private final Grid.Column<AdministrationQueryService.RoleRow> actionsColumn;
    private final PagedGrid<AdministrationQueryService.RoleRow> pages;
    private final Button grantAction;
    private final Button createAction;

    public RolesView(CurrentUserProvider currentUser, AuthorizationService authorization,
                     AdministrationQueryService queries, PermissionCatalog catalog, GrantPermissionUseCase grants,
                     RoleAdministrationService commands) {
        super(currentUser, authorization);
        codeColumn = grid.addColumn(AdministrationQueryService.RoleRow::code).setAutoWidth(true);
        permissionCountColumn = grid.addColumn(AdministrationQueryService.RoleRow::permissionCount);
        actionsColumn = grid.addComponentColumn(this::detailsAction).setAutoWidth(true);
        grid.setSelectionMode(Grid.SelectionMode.NONE);
        grid.setSizeFull();
        var header = PageHeader.translated("system.roles.title", "system.roles.intent");
        var workspace = new DataWorkspace<>(grid);
        workspace.setCompactItemRenderer(this::compactItem, AdministrationQueryService.RoleRow::code);
        pages = new PagedGrid<>(workspace, queries::roles, "code");
        workspace.setSelectionBarVisible(false);
        workspace.getElement().setAttribute("data-testid", "roles-workspace");
        workspace.setFooter(pages.getPaginationBar());
        grantAction = new Button();
        grantAction.addClickListener(event -> grantDialog(queries, catalog, grants, pages, new OperationFeedback()).open());
        grantAction.setVisible(authorization.hasPermission(requireCurrentUser(), GRANT));
        var toolbar = new PageToolbar();
        toolbar.getElement().setAttribute("data-testid", "roles-toolbar");
        toolbar.addAction(grantAction);
        createAction = new Button(AdminIcon.of(AdminIconName.ADD), event -> createRole(commands));
        createAction.setVisible(authorization.hasPermission(requireCurrentUser(), CREATE));
        toolbar.setPrimaryAction(createAction);

        var frame = new AdminPageFrame(header, toolbar, workspace);
        add(frame);
        expand(frame);
        updateText();
    }

    private Button detailsAction(AdministrationQueryService.RoleRow role) {
        var details = new Button(AdminIcon.of(AdminIconName.EYE), event -> showDetails(role));
        details.setTooltipText(getTranslation("system.roles.details"));
        details.setAriaLabel(getTranslation("system.roles.details-aria", role.code()));
        return details;
    }

    private CompactDataItem compactItem(AdministrationQueryService.RoleRow role) {
        var item = new CompactDataItem(role.code());
        item.addMetadata(getTranslation("system.roles.permission-count"), Long.toString(role.permissionCount()));
        var details = new Button(getTranslation("system.roles.details"), AdminIcon.of(AdminIconName.EYE),
                event -> showDetails(role));
        details.addThemeVariants(ButtonVariant.TERTIARY);
        item.addActions(details);
        return item;
    }

    private void showDetails(AdministrationQueryService.RoleRow role) {
        var dialog = DetailDialog.translated("system.roles.details-title");
        dialog.addField(getTranslation("system.roles.code"), role.code());
        dialog.addField(getTranslation("system.roles.permission-count"), Long.toString(role.permissionCount()));
        dialog.addField(getTranslation("system.roles.granted-permissions"), String.join(", ", role.permissionCodes()));
        dialog.open();
    }

    @Override
    protected PermissionCode requiredPermission() {
        return REQUIRED_PERMISSION;
    }

    private void createRole(RoleAdministrationService commands) {
        var code = new TextField(getTranslation("system.roles.code"));
        code.setRequired(true);
        code.setMaxLength(120);
        var dialog = EditorDialog.translated("system.roles.create", "system.users.save", () -> { });
        dialog.addField(code);
        dialog.getPrimaryAction().addClickListener(event -> {
            try {
                commands.create(requireCurrentUser(), code.getValue());
                dialog.close();
                pages.refresh();
            } catch (BusinessFailure failure) {
                ViewBusinessFailureHandler.handle(failure, validation -> dialog.showValidationMessage(getTranslation(
                        "already-exists".equals(validation.fieldErrors().get("code"))
                                ? "system.roles.already-exists" : "system.roles.required")));
            }
        });
        dialog.open();
    }

    private EditorDialog grantDialog(AdministrationQueryService queries, PermissionCatalog catalog,
                                     GrantPermissionUseCase grants,
                                     PagedGrid<AdministrationQueryService.RoleRow> pages, OperationFeedback feedback) {
        var role = new ComboBox<String>(getTranslation("system.roles.role"));
        role.setItems(queries.roles().stream().map(AdministrationQueryService.RoleRow::code).toList());
        var permission = new ComboBox<PermissionCode>(getTranslation("system.roles.permission"));
        permission.setItems(catalog.all().stream().sorted(Comparator.comparing(PermissionCode::value)).toList());
        permission.setItemLabelGenerator(PermissionCode::value);
        var dialog = EditorDialog.translated("system.roles.grant", "system.roles.save-grant", () -> { });
        dialog.getPrimaryAction().setEnabled(false);
        dialog.getPrimaryAction().addClickListener(event -> {
            grants.grant(requireCurrentUser(), new GrantPermissionCommand(role.getValue(), permission.getValue()));
            dialog.close();
            pages.refresh();
            feedback.success(getTranslation("system.roles.granted-success"));
        });
        role.addValueChangeListener(event -> updateGrantAvailability(dialog, role, permission));
        permission.addValueChangeListener(event -> updateGrantAvailability(dialog, role, permission));
        dialog.addField(role, permission);
        return dialog;
    }

    private void updateGrantAvailability(EditorDialog dialog, ComboBox<String> role,
                                         ComboBox<PermissionCode> permission) {
        dialog.getPrimaryAction().setEnabled(role.getValue() != null && permission.getValue() != null);
    }

    @Override public void localeChange(LocaleChangeEvent event) { updateText(); pages.refresh(); updateBrowserTitle(); }

    @Override public String getPageTitle() { return getTranslation("system.roles.title"); }

    private void updateText() {
        codeColumn.setHeader(getTranslation("system.roles.code"));
        permissionCountColumn.setHeader(getTranslation("system.roles.permission-count"));
        actionsColumn.setHeader(getTranslation("system.roles.actions"));
        grantAction.setText(getTranslation("system.roles.grant"));
        createAction.setText(getTranslation("system.roles.create"));
    }

    private void updateBrowserTitle() { getUI().ifPresent(ui -> ui.getPage().setTitle(getPageTitle())); }
}
