package io.github.huahill.vadmin.modules;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.huahill.vadmin.views.AuditView;
import io.github.huahill.vadmin.views.PermissionsView;
import io.github.huahill.vadmin.views.RolesView;
import io.github.huahill.vadmin.views.UsersView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SystemAdministrationModuleConfigurationTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SystemAdministrationModuleConfiguration.class));

    @Test
    void contributesAllSystemPagesAndTheirDeclaredPermissions() {
        var module = new SystemAdministrationModuleConfiguration().systemAdministration();

        assertThat(module.moduleId()).isEqualTo("system");
        assertThat(module.pages()).extracting(page -> page.route())
                .containsExactly("users", "roles", "permissions", "audit");
        assertThat(module.pages()).extracting(page -> page.viewType().getName())
                .containsExactly(UsersView.class.getName(), RolesView.class.getName(), PermissionsView.class.getName(),
                        AuditView.class.getName());
        assertThat(module.permissions()).contains(
                UsersView.REQUIRED_PERMISSION, RolesView.REQUIRED_PERMISSION,
                PermissionsView.REQUIRED_PERMISSION, AuditView.REQUIRED_PERMISSION);
        assertThat(module.permissions()).contains(
                io.github.huahill.vadmin.contracts.auth.PermissionCode.of("system:role:create"),
                io.github.huahill.vadmin.contracts.auth.PermissionCode.of("system:user:roles"));
        assertThat(module.messageBundles()).extracting(bundle -> bundle.baseName()).containsExactly("i18n.system");
    }

    @Test
    void backsOffWhenTheHostUsesExternalIdentityManagement() {
        context.withPropertyValues("vadmin.local-iam.enabled=false")
                .run(application -> assertThat(application).doesNotHaveBean("systemAdministration"));
    }

    @Test
    void supportsSystemPagesWithHostProvidedIdentityStorage() {
        context.withPropertyValues("vadmin.local-iam.enabled=false", "vadmin.system-admin.enabled=true")
                .run(application -> assertThat(application).hasBean("systemAdministration"));
    }

    @Test
    void canDisableSystemPagesWithoutDisablingLocalLogin() {
        context.withPropertyValues("vadmin.system-admin.enabled=false")
                .run(application -> assertThat(application).doesNotHaveBean("systemAdministration"));
    }

    @Test
    void assemblesPermissionGrantsForHostProvidedStorage() {
        context.withPropertyValues("vadmin.local-iam.enabled=false", "vadmin.system-admin.enabled=true")
                .withBean(io.github.huahill.vadmin.platform.access.AccessControlRepository.class,
                        () -> org.mockito.Mockito.mock(io.github.huahill.vadmin.platform.access.AccessControlRepository.class))
                .withBean(io.github.huahill.vadmin.contracts.auth.AuthorizationService.class,
                        () -> org.mockito.Mockito.mock(io.github.huahill.vadmin.contracts.auth.AuthorizationService.class))
                .withBean(io.github.huahill.vadmin.contracts.audit.AuditSink.class,
                        () -> org.mockito.Mockito.mock(io.github.huahill.vadmin.contracts.audit.AuditSink.class))
                .withBean(io.github.huahill.vadmin.contracts.auth.PermissionCatalog.class,
                        () -> new io.github.huahill.vadmin.contracts.auth.PermissionCatalog(java.util.Set.of()))
                .withBean(org.springframework.transaction.PlatformTransactionManager.class,
                        () -> org.mockito.Mockito.mock(org.springframework.transaction.PlatformTransactionManager.class))
                .run(application -> assertThat(application)
                        .hasSingleBean(io.github.huahill.vadmin.platform.access.GrantPermissionUseCase.class));
    }
}
