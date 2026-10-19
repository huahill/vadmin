package io.github.huahill.vadmin.administration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.huahill.vadmin.contracts.audit.AuditSink;
import io.github.huahill.vadmin.contracts.auth.AuthorizationService;
import io.github.huahill.vadmin.contracts.auth.CurrentUser;
import io.github.huahill.vadmin.contracts.auth.PermissionCode;
import io.github.huahill.vadmin.contracts.error.BusinessFailure;
import io.github.huahill.vadmin.platform.administration.AdministrationStore;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserAdministrationServiceTest {
    @Test
    void createsUserOnlyAfterCheckingCreatePermission() {
        var store = mock(AdministrationStore.class);
        var authorization = mock(AuthorizationService.class);
        var auditSink = mock(AuditSink.class);
        var passwordEncoder = mock(PasswordEncoder.class);
        var service = new UserAdministrationService(store, authorization, auditSink, passwordEncoder);
        var actor = new CurrentUser(UUID.randomUUID(), "administrator",
                Set.of(PermissionCode.of("system:user:create")), 0);

        org.mockito.Mockito.when(passwordEncoder.encode("initial-password")).thenReturn("encoded-password");
        org.mockito.Mockito.when(store.insertUser(any(), eq("operator"), eq("encoded-password"))).thenReturn(true);
        service.create(actor, "operator", "initial-password");

        verify(authorization).requirePermission(actor, PermissionCode.of("system:user:create"));
        verify(store).insertUser(any(UUID.class), eq("operator"), eq("encoded-password"));
        verify(auditSink).append(any());
    }

    @Test
    void reportsDuplicateUsernameAsFieldValidation() {
        var store = mock(AdministrationStore.class);
        var service = new UserAdministrationService(store, mock(AuthorizationService.class), mock(AuditSink.class),
                mock(PasswordEncoder.class));
        var actor = new CurrentUser(UUID.randomUUID(), "administrator",
                Set.of(PermissionCode.of("system:user:create")), 0);

        assertThatThrownBy(() -> service.create(actor, "operator", "initial-password"))
                .isInstanceOfSatisfying(BusinessFailure.class, failure -> {
                    assertThat(failure.detailKey()).isEqualTo("validation.failed");
                    assertThat(failure.fieldErrors()).containsEntry("username", "already-exists");
                });
    }

    @Test
    void preservesWhitespaceInPasswords() {
        var store = mock(AdministrationStore.class);
        var encoder = mock(PasswordEncoder.class);
        org.mockito.Mockito.when(encoder.encode(" secret ")).thenReturn("hash");
        org.mockito.Mockito.when(store.insertUser(any(), eq("operator"), eq("hash"))).thenReturn(true);
        new UserAdministrationService(store, mock(AuthorizationService.class), mock(AuditSink.class), encoder)
                .create(new CurrentUser(UUID.randomUUID(), "admin", Set.of(), 0), "operator", " secret ");
        verify(encoder).encode(" secret ");
    }
}
