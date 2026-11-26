package io.github.huahill.vadmin.contracts.auth;

import java.util.List;

/** Host-provided lookup of people in an external identity system. VAdmin does not speak that system's protocol. */
public interface ExternalDirectory {
    List<ExternalPerson> search(String query);
}
