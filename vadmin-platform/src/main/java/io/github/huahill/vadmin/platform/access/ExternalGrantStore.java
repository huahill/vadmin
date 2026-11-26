package io.github.huahill.vadmin.platform.access;

import io.github.huahill.vadmin.contracts.auth.PermissionCode;
import io.github.huahill.vadmin.contracts.auth.SubjectId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Role grants for external subjects. This is not a copy of the external user directory. */
public interface ExternalGrantStore {
    ExternalAccess access(SubjectId subject);

    Set<UUID> roleIds(SubjectId subject);

    boolean isLastAdministrator(SubjectId subject);

    boolean includesAdministrator(Set<UUID> roleIds);

    boolean hasAdministrator();

    void replaceRoles(SubjectId subject, String label, Set<UUID> roleIds);

    void ensureAdministrator(SubjectId subject);

    List<ExternalGrant> list();

    record ExternalAccess(Set<PermissionCode> permissions, long authVersion) {
        public ExternalAccess {
            permissions = Set.copyOf(permissions);
        }
    }

    record ExternalGrant(SubjectId subject, String label, long authVersion, List<String> roleCodes) {
        public ExternalGrant {
            roleCodes = List.copyOf(roleCodes);
        }
    }
}
