package io.github.huahill.vadmin.app.auth;

import java.net.URI;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("vadmin.identity.oidc")
public record OidcIdentityLinkProperties(List<Link> links) {
    public OidcIdentityLinkProperties {
        links = links == null ? List.of() : List.copyOf(links);
    }

    public record Link(URI issuer, String subject, String username) {
    }
}
