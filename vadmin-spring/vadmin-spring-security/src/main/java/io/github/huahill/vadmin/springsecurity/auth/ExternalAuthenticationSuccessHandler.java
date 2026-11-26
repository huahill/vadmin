package io.github.huahill.vadmin.springsecurity.auth;

import io.github.huahill.vadmin.contracts.auth.ExternalAccessResolver;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/** Accepts an external login without looking up a local account. Permissions come from VAdmin grants. */
public final class ExternalAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final ExternalAccessResolver resolver;
    private final OidcExternalIdentityFactory identityFactory;
    private final SecurityContextRepository securityContextRepository;
    private final AuthenticationSuccessHandler successHandler;
    private final OidcAccessDeniedFailureHandler deniedFailureHandler;

    public ExternalAuthenticationSuccessHandler(ExternalAccessResolver resolver,
                                                AuthenticationSuccessHandler successHandler) {
        this(resolver, new OidcExternalIdentityFactory(), new HttpSessionSecurityContextRepository(), successHandler,
                new OidcAccessDeniedFailureHandler());
    }

    ExternalAuthenticationSuccessHandler(ExternalAccessResolver resolver, OidcExternalIdentityFactory identityFactory,
                                         SecurityContextRepository securityContextRepository,
                                         AuthenticationSuccessHandler successHandler,
                                         OidcAccessDeniedFailureHandler deniedFailureHandler) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.identityFactory = Objects.requireNonNull(identityFactory, "identityFactory");
        this.securityContextRepository = Objects.requireNonNull(securityContextRepository, "securityContextRepository");
        this.successHandler = Objects.requireNonNull(successHandler, "successHandler");
        this.deniedFailureHandler = Objects.requireNonNull(deniedFailureHandler, "deniedFailureHandler");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            deniedFailureHandler.deny(request, response);
            return;
        }
        var principal = new LocalUserPrincipal(resolver.resolve(identityFactory.from(oidcUser)));
        var localAuthentication = UsernamePasswordAuthenticationToken.authenticated(principal, null,
                principal.getAuthorities());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(localAuthentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        successHandler.onAuthenticationSuccess(request, response, localAuthentication);
    }
}
