package io.github.huahill.vadmin.administration;

import io.github.huahill.vadmin.contracts.navigation.PagedQuery;
import io.github.huahill.vadmin.contracts.navigation.PagedResult;
import io.github.huahill.vadmin.localiam.ConditionalOnSystemAdministration;
import io.github.huahill.vadmin.platform.administration.AdministrationStore;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnSystemAdministration
@Transactional(readOnly = true)
public class AdministrationQueryService {
    private final AdministrationStore store;

    public AdministrationQueryService(AdministrationStore store) {
        this.store = store;
    }

    public List<UserRow> users(String filter) {
        return users(new PagedQuery(0, 100, "username", true, Map.of("q", filter == null ? "" : filter))).items();
    }

    public PagedResult<UserRow> users(PagedQuery query) {
        return map(store.users(query), row -> new UserRow(row.id(), row.username(), row.enabled(), row.authVersion()));
    }

    public List<RoleRow> roles() {
        return store.roles().stream().map(AdministrationQueryService::role).toList();
    }

    public PagedResult<RoleRow> roles(PagedQuery query) {
        return map(store.roles(query), AdministrationQueryService::role);
    }

    public Set<UUID> userRoleIds(UUID userId) {
        return store.userRoleIds(userId);
    }

    public List<PermissionRow> permissions() {
        return permissions(new PagedQuery(0, Integer.MAX_VALUE, "code", true, Map.of())).items();
    }

    public PagedResult<PermissionRow> permissions(PagedQuery query) {
        return map(store.permissions(query), row -> new PermissionRow(row.code(), row.systemManaged()));
    }

    public List<AuditRow> audit() {
        return audit(new PagedQuery(0, 100, "occurredAt", false, Map.of())).items();
    }

    public PagedResult<AuditRow> audit(PagedQuery query) {
        return map(store.audit(query), row -> new AuditRow(row.occurredAt(), row.action(), row.targetType(),
                row.targetId(), row.outcome(), row.correlationId()));
    }

    private static RoleRow role(AdministrationStore.RoleRow row) {
        return new RoleRow(row.id(), row.code(), row.permissionCount(), row.permissionCodes());
    }

    private static <T, R> PagedResult<R> map(PagedResult<T> result, Function<T, R> mapper) {
        return new PagedResult<>(result.items().stream().map(mapper).toList(), result.total());
    }

    public record UserRow(UUID id, String username, boolean enabled, long authVersion) { }
    public record RoleRow(UUID id, String code, long permissionCount, List<String> permissionCodes) {
        public RoleRow { permissionCodes = List.copyOf(permissionCodes); }
    }
    public record PermissionRow(String code, boolean systemManaged) { }
    public record AuditRow(Instant occurredAt, String action, String targetType, String targetId, String outcome,
                           String correlationId) { }
}
