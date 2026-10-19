package io.github.huahill.vadmin.platform.administration;

import io.github.huahill.vadmin.contracts.navigation.PagedQuery;
import io.github.huahill.vadmin.contracts.navigation.PagedResult;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Storage for shared system administration, independent of SQL and ORM. */
public interface AdministrationStore {
    PagedResult<UserRow> users(PagedQuery query);
    PagedResult<RoleRow> roles(PagedQuery query);
    List<RoleRow> roles();
    PagedResult<PermissionRow> permissions(PagedQuery query);
    PagedResult<AuditRow> audit(PagedQuery query);
    Set<UUID> userRoleIds(UUID userId);
    boolean userExists(UUID userId);
    boolean insertUser(UUID id, String username, String passwordHash);
    boolean insertRole(UUID id, String code);
    void setEnabled(UUID userId, boolean enabled);
    void replaceUserRoles(UUID userId, Set<UUID> roleIds);

    /** Serialize administrator membership/enablement changes in the caller's transaction. */
    void lockAdministratorRole();
    boolean isLastEnabledAdministrator(UUID userId);

    record UserRow(UUID id, String username, boolean enabled, long authVersion) { }
    record RoleRow(UUID id, String code, long permissionCount, List<String> permissionCodes) {
        public RoleRow { permissionCodes = List.copyOf(permissionCodes); }
    }
    record PermissionRow(String code, boolean systemManaged) { }
    record AuditRow(Instant occurredAt, String action, String targetType, String targetId, String outcome,
                    String correlationId) { }
}
