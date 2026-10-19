package io.github.huahill.vadmin.administration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.huahill.vadmin.contracts.audit.AuditSink;
import io.github.huahill.vadmin.contracts.auth.AuthorizationService;
import io.github.huahill.vadmin.contracts.auth.CurrentUser;
import io.github.huahill.vadmin.contracts.auth.PermissionCode;
import io.github.huahill.vadmin.contracts.error.BusinessFailure;
import io.github.huahill.vadmin.platform.administration.AdministrationStore;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RoleAdministrationServiceTest {
    private final AdministrationStore store = mock(AdministrationStore.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final AuditSink audit = mock(AuditSink.class);
    private final RoleAdministrationService service = new RoleAdministrationService(store, authorization, audit);
    private final CurrentUser actor = new CurrentUser(UUID.randomUUID(), "admin", Set.of(), 0);

    @Test
    void createsRoleAfterCheckingPermission() {
        when(store.insertRole(any(), eq("operators"))).thenReturn(true);
        service.create(actor, " operators ");
        verify(authorization).requirePermission(actor, PermissionCode.of("system:role:create"));
        verify(store).insertRole(any(), eq("operators"));
        verify(audit).append(any());
    }

    @Test
    void rejectsWritesWhenActorLacksPermission() {
        doThrow(new IllegalStateException("denied")).when(authorization)
                .requirePermission(actor, PermissionCode.of("system:user:roles"));
        assertThatThrownBy(() -> service.assignRoles(actor, UUID.randomUUID(), Set.of()))
                .hasMessage("denied");
        verifyNoInteractions(store, audit);
    }

    @Test
    void rejectsUnknownRoleBeforeChangingMemberships() {
        UUID userId = UUID.randomUUID();
        when(store.userExists(userId)).thenReturn(true);
        when(store.roles()).thenReturn(List.of());
        assertThatThrownBy(() -> service.assignRoles(actor, userId, Set.of(UUID.randomUUID())))
                .isInstanceOf(BusinessFailure.class);
        verifyNoInteractions(audit);
    }

    @Test
    void cannotRemoveTheLastEnabledAdministrator() {
        UUID userId = UUID.randomUUID();
        when(store.userExists(userId)).thenReturn(true);
        when(store.isLastEnabledAdministrator(userId)).thenReturn(true);
        when(store.roles()).thenReturn(List.of());
        assertThatThrownBy(() -> service.assignRoles(actor, userId, Set.of()))
                .isInstanceOf(BusinessFailure.class);
        verifyNoInteractions(audit);
    }
}
