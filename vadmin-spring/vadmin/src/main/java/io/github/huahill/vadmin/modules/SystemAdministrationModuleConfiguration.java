package io.github.huahill.vadmin.modules;

import io.github.huahill.vadmin.contracts.auth.PermissionCode;
import io.github.huahill.vadmin.contracts.auth.AuthorizationService;
import io.github.huahill.vadmin.contracts.auth.PermissionCatalog;
import io.github.huahill.vadmin.contracts.audit.AuditSink;
import io.github.huahill.vadmin.contracts.audit.CorrelationIdProvider;
import io.github.huahill.vadmin.platform.access.AccessControlRepository;
import io.github.huahill.vadmin.platform.access.GrantPermissionService;
import io.github.huahill.vadmin.platform.access.GrantPermissionUseCase;
import io.github.huahill.vadmin.platform.administration.AdministrationStore;
import io.github.huahill.vadmin.administration.AdministrationQueryService;
import io.github.huahill.vadmin.administration.UserAdministrationService;
import io.github.huahill.vadmin.administration.RoleAdministrationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import io.github.huahill.vadmin.flow.navigation.AdminMessageBundle;
import io.github.huahill.vadmin.flow.navigation.AdminModule;
import io.github.huahill.vadmin.flow.navigation.AdminNavigationGroup;
import io.github.huahill.vadmin.flow.navigation.AdminPage;
import io.github.huahill.vadmin.localiam.ConditionalOnSystemAdministration;
import io.github.huahill.vadmin.views.AuditView;
import io.github.huahill.vadmin.views.PermissionsView;
import io.github.huahill.vadmin.views.RolesView;
import io.github.huahill.vadmin.views.UsersView;
import java.util.List;
import java.util.Set;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@AutoConfiguration
@AutoConfigureAfter(name = {"io.github.huahill.vadmin.springjpa.JpaAccessControlConfiguration",
        "io.github.huahill.vadmin.springjdbc.JdbcIamAutoConfiguration"})
@ConditionalOnSystemAdministration
public class SystemAdministrationModuleConfiguration {
    @Bean
    @ConditionalOnBean(AdministrationStore.class)
    @ConditionalOnMissingBean
    AdministrationQueryService administrationQueryService(AdministrationStore store) {
        return new AdministrationQueryService(store);
    }

    @Bean
    @ConditionalOnBean(AdministrationStore.class)
    @ConditionalOnMissingBean
    UserAdministrationService userAdministrationService(AdministrationStore store, AuthorizationService authorization,
                                                       AuditSink audit, PasswordEncoder passwords) {
        return new UserAdministrationService(store, authorization, audit, passwords);
    }

    @Bean
    @ConditionalOnBean(AdministrationStore.class)
    @ConditionalOnMissingBean
    RoleAdministrationService roleAdministrationService(AdministrationStore store, AuthorizationService authorization,
                                                       AuditSink audit) {
        return new RoleAdministrationService(store, authorization, audit);
    }

    @Bean
    @ConditionalOnBean(AccessControlRepository.class)
    @ConditionalOnMissingBean(GrantPermissionUseCase.class)
    GrantPermissionUseCase grantPermissionUseCase(AuthorizationService authorization, AccessControlRepository repository,
                                                 PermissionCatalog catalog, AuditSink audit,
                                                 ObjectProvider<CorrelationIdProvider> correlationIds,
                                                 PlatformTransactionManager transactionManager) {
        var delegate = new GrantPermissionService(authorization, repository, catalog, audit,
                correlationIds.getIfAvailable(() -> () -> null));
        var transaction = new TransactionTemplate(transactionManager);
        return (actor, command) -> transaction.executeWithoutResult(status -> delegate.grant(actor, command));
    }

    @Bean
    public AdminModule systemAdministration() {
        return AdminModule.of("system",
                List.of(new AdminNavigationGroup("system", "system.navigation", 100)),
                List.of(
                        page("system.users", "system.users.title", "system.users.intent", "users", 100, "users",
                                UsersView.REQUIRED_PERMISSION, UsersView.class),
                        page("system.roles", "system.roles.title", "system.roles.intent", "shield", 200, "roles",
                                RolesView.REQUIRED_PERMISSION, RolesView.class),
                        page("system.permissions", "system.permissions.title", "system.permissions.intent", "key", 300,
                                "permissions", PermissionsView.REQUIRED_PERMISSION, PermissionsView.class),
                        page("system.audit", "system.audit.title", "system.audit.intent", "clock", 400, "audit",
                                AuditView.REQUIRED_PERMISSION, AuditView.class)),
                Set.of(
                        UsersView.REQUIRED_PERMISSION,
                        PermissionCode.of("system:user:create"),
                        PermissionCode.of("system:user:update"),
                        PermissionCode.of("system:user:roles"),
                        RolesView.REQUIRED_PERMISSION,
                        PermissionCode.of("system:role:create"),
                        PermissionCode.of("system:role:grant"),
                        PermissionsView.REQUIRED_PERMISSION,
                        AuditView.REQUIRED_PERMISSION),
                List.of(new AdminMessageBundle("system", "i18n.system")));
    }

    private static AdminPage page(String pageId, String titleKey, String intentKey, String iconKey, int order,
                                  String route, PermissionCode permission,
                                  Class<? extends com.vaadin.flow.component.Component> viewType) {
        return new AdminPage(pageId, "system", titleKey, intentKey, iconKey, order, route, permission, viewType);
    }
}
