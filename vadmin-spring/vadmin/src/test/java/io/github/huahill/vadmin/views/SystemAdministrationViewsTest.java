package io.github.huahill.vadmin.views;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import io.github.huahill.vadmin.administration.AdministrationQueryService;
import io.github.huahill.vadmin.administration.RoleAdministrationService;
import io.github.huahill.vadmin.administration.UserAdministrationService;
import io.github.huahill.vadmin.contracts.auth.AuthorizationService;
import io.github.huahill.vadmin.contracts.auth.CurrentUser;
import io.github.huahill.vadmin.contracts.auth.PermissionCatalog;
import io.github.huahill.vadmin.contracts.navigation.PagedQuery;
import io.github.huahill.vadmin.contracts.navigation.PagedResult;
import io.github.huahill.vadmin.flow.patterns.PageToolbar;
import io.github.huahill.vadmin.flow.patterns.EditorDialog;
import io.github.huahill.vadmin.platform.access.GrantPermissionUseCase;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SystemAdministrationViewsTest {
    private UI ui;
    private final AdministrationQueryService queries = mock(AdministrationQueryService.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final CurrentUser actor = new CurrentUser(UUID.randomUUID(), "admin", Set.of(), 0);

    @BeforeEach
    void setUp() {
        ui = new UI();
        ui.getInternals().setSession(mock(com.vaadin.flow.server.VaadinSession.class, org.mockito.Mockito.RETURNS_DEEP_STUBS));
        UI.setCurrent(ui);
        when(queries.users(any(PagedQuery.class))).thenReturn(new PagedResult<>(List.of(), 0));
        when(queries.roles(any(PagedQuery.class))).thenReturn(new PagedResult<>(List.of(), 0));
        when(authorization.hasPermission(any(), any())).thenReturn(true);
    }

    @AfterEach
    void tearDown() { UI.setCurrent(null); }

    @Test
    void exposesCreateRoleOnlyToAuthorizedUsers() {
        var commands = mock(RoleAdministrationService.class);
        var view = new RolesView(() -> Optional.of(actor), authorization, queries,
                new PermissionCatalog(Set.of()), mock(GrantPermissionUseCase.class), commands);
        UI.getCurrent().add(view);
        PageToolbar toolbar = components(view).filter(PageToolbar.class::isInstance).map(PageToolbar.class::cast)
                .findFirst().orElseThrow();
        assertThat(toolbar.getPrimaryAction().isVisible()).isTrue();
        assertThat(toolbar.getPrimaryAction().getText()).contains("system.roles.create");
        toolbar.getPrimaryAction().click();
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        var dialog = components(UI.getCurrent()).filter(EditorDialog.class::isInstance).map(EditorDialog.class::cast)
                .findFirst().orElseThrow();
        dialog.getForm().getChildren().filter(TextField.class::isInstance).map(TextField.class::cast)
                .findFirst().orElseThrow().setValue("operators");
        dialog.getPrimaryAction().click();
        verify(commands).create(actor, "operators");
        when(authorization.hasPermission(any(), any())).thenReturn(false);
        var forbidden = new RolesView(() -> Optional.of(actor), authorization, queries,
                new PermissionCatalog(Set.of()), mock(GrantPermissionUseCase.class), mock(RoleAdministrationService.class));
        assertThat(components(forbidden).filter(PageToolbar.class::isInstance).map(PageToolbar.class::cast)
                .findFirst().orElseThrow().getPrimaryAction().isVisible()).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void exposesRoleAssignmentOnUserRows() {
        var commands = mock(RoleAdministrationService.class);
        var role = new AdministrationQueryService.RoleRow(UUID.randomUUID(), "operators", 0, List.of());
        when(queries.roles()).thenReturn(List.of(role));
        when(queries.userRoleIds(any())).thenReturn(Set.of());
        var view = new UsersView(() -> Optional.of(actor), authorization, queries,
                mock(UserAdministrationService.class), commands);
        Grid<AdministrationQueryService.UserRow> grid = (Grid<AdministrationQueryService.UserRow>) components(view)
                .filter(Grid.class::isInstance).findFirst().orElseThrow();
        var renderer = (ComponentRenderer<Component, AdministrationQueryService.UserRow>) grid.getColumns().getLast().getRenderer();
        UUID userId = UUID.randomUUID();
        Component actions = renderer.createComponent(new AdministrationQueryService.UserRow(userId, "operator", true, 0));
        UI.getCurrent().add(view, actions);
        assertThat(components(actions).filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> button.getElement().getAttribute("data-testid") != null)
                .map(button -> button.getElement().getAttribute("data-testid")))
                .contains("user-assign-roles");
        components(actions).filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> "user-assign-roles".equals(button.getElement().getAttribute("data-testid")))
                .findFirst().orElseThrow().click();
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        var dialog = components(UI.getCurrent()).filter(EditorDialog.class::isInstance).map(EditorDialog.class::cast)
                .findFirst().orElseThrow();
        var choices = (MultiSelectComboBox<AdministrationQueryService.RoleRow>) dialog.getForm().getChildren()
                .filter(MultiSelectComboBox.class::isInstance).findFirst().orElseThrow();
        choices.setValue(Set.of(role));
        dialog.getPrimaryAction().click();
        verify(commands).assignRoles(actor, userId, Set.of(role.id()));
    }

    private static Stream<Component> components(Component root) {
        return Stream.concat(Stream.of(root), root.getChildren().flatMap(SystemAdministrationViewsTest::components));
    }
}
