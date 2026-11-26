package io.github.huahill.vadmin.springsecurity.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.server.VaadinService;
import io.github.huahill.vadmin.brand.AdminBrandProperties;
import io.github.huahill.vadmin.springsecurity.LocalLoginAvailability;
import io.github.huahill.vadmin.springsecurity.OidcLoginAvailability;
import io.github.huahill.vadmin.springsecurity.auth.LocalLoginAuthenticator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginViewTest {
    private VaadinService service;
    private LocalLoginAuthenticator localLoginAuthenticator;

    @BeforeEach
    void installEnglishTranslations() {
        var translations = mock(I18NProvider.class);
        when(translations.getDefaultLocale()).thenReturn(Locale.US);
        when(translations.getProvidedLocales()).thenReturn(List.of(Locale.US));
        when(translations.getTranslation("flow.login.title", Locale.US)).thenReturn("Sign in");
        when(translations.getTranslation("flow.login.sso", Locale.US)).thenReturn("Continue with single sign-on");
        when(translations.getTranslation("flow.login.denied", Locale.US)).thenReturn("Sign-in was not permitted");
        var instantiator = mock(Instantiator.class);
        when(instantiator.getI18NProvider()).thenReturn(translations);
        service = mock(VaadinService.class, CALLS_REAL_METHODS);
        when(service.getInstantiator()).thenReturn(instantiator);
        var deploymentConfiguration = mock(DeploymentConfiguration.class);
        when(deploymentConfiguration.getUrlSafeSchemes()).thenReturn(Set.of("http", "https"));
        when(service.getDeploymentConfiguration()).thenReturn(deploymentConfiguration);
        VaadinService.setCurrent(service);
        localLoginAuthenticator = mock(LocalLoginAuthenticator.class);
    }

    @AfterEach
    void clearVaadinService() {
        service.setCurrentInstances(null, null);
        VaadinService.setCurrent(null);
    }

    @Test
    void rendersOneProviderNeutralExternalEntryWhenOidcIsAvailable() {
        var view = loginView(new OidcLoginAvailability(true, "oidc"), true);

        var externalEntry = oidcEntry(view);
        assertThat(externalEntry.getAttribute("href")).isEqualTo("oauth2/authorization/oidc");
        assertThat(externalEntry.hasAttribute("router-ignore")).isTrue();
        assertThat(externalEntry.getText()).isEqualTo("Continue with single sign-on");
        assertThat(view.getChildren()).anyMatch(LoginOverlay.class::isInstance);
        assertThat(LoginView.class.getAnnotation(com.vaadin.flow.router.Route.class).autoLayout()).isFalse();
    }

    @Test
    void usesTheConfiguredRegistrationIdForTheExternalEntry() {
        var view = loginView(new OidcLoginAvailability(true, "corp-sso"), true);

        assertThat(oidcEntry(view).getAttribute("href")).isEqualTo("oauth2/authorization/corp-sso");
    }

    @Test
    void usesAPathRelativeExternalEntrySoAStrippedProxyPrefixIsPreserved() {
        var view = loginView(new OidcLoginAvailability(true, "corp-sso"), true);

        assertThat(oidcEntry(view).getAttribute("href")).isEqualTo("oauth2/authorization/corp-sso");
    }

    @Test
    void usesAPathRelativeHomeAfterLocalLogin() {
        assertThat(LoginView.postLoginBrowserLocation()).isEqualTo(".");
    }

    @Test
    void keepsTheExistingLocalLoginWithoutAnExternalEntryWhenOidcIsUnavailable() {
        var view = loginView(new OidcLoginAvailability(false, "oidc"), true);

        assertThat(loginOverlay(view).getElement().getChildren()).noneMatch(element -> "a".equals(element.getTag()));
        assertThat(loginOverlay(view).getAction()).isEmpty();
    }

    @Test
    void showsTheGenericLoginFormErrorAfterLocalCredentialsAreRejected() {
        var view = loginView(new OidcLoginAvailability(false, "oidc"), true);
        var event = mock(com.vaadin.flow.router.BeforeEnterEvent.class);
        when(event.getLocation()).thenReturn(new Location("login", QueryParameters.of("error", "")));

        view.beforeEnter(event);

        assertThat(loginOverlay(view).isError()).isTrue();
    }

    @Test
    void usesTheProductNameAsTheLocalLoginHeading() {
        var view = loginView(new OidcLoginAvailability(false, "oidc"), true, brand("  RDC CI  "));

        assertThat(view.getPageTitle()).isEqualTo("Sign in");
        assertThat(loginI18n(view)).contains("RDC CI").doesNotContain("  RDC CI  ");
        assertThat(view.getChildren()).noneMatch(H1.class::isInstance);
    }

    @Test
    void fallsBackToVadminWhenTheProductNameIsBlank() {
        var view = loginView(new OidcLoginAvailability(false, "oidc"), true, brand(" "));

        assertThat(loginI18n(view)).contains("VAdmin");
    }

    @Test
    void showsTheProductNameWhenOnlyExternalLoginIsAvailable() {
        var view = loginView(new OidcLoginAvailability(true, "identity"), false, brand("RDC CI"));

        assertThat(view.getChildren()).noneMatch(LoginOverlay.class::isInstance);
        assertThat(view.getChildren().filter(H1.class::isInstance).map(H1.class::cast).findFirst().orElseThrow().getText())
                .isEqualTo("RDC CI");
        var externalEntry = view.getChildren().filter(Anchor.class::isInstance).map(Anchor.class::cast).findFirst().orElseThrow();
        assertThat(externalEntry.getHref()).isEqualTo("oauth2/authorization/identity");
        assertThat(externalEntry.getElement().hasAttribute("router-ignore")).isTrue();
    }

    private LoginView loginView(OidcLoginAvailability oidc, boolean localLogin) {
        return loginView(oidc, localLogin, new AdminBrandProperties());
    }

    private LoginView loginView(OidcLoginAvailability oidc, boolean localLogin, AdminBrandProperties brand) {
        return new LoginView(oidc, localLoginAuthenticator, new LocalLoginAvailability(localLogin), brand);
    }

    private static AdminBrandProperties brand(String name) {
        var brand = new AdminBrandProperties();
        brand.setName(name);
        return brand;
    }

    private static String loginI18n(LoginView view) {
        return String.valueOf(loginOverlay(view).getElement().getPropertyRaw("i18n"));
    }

    private static LoginOverlay loginOverlay(LoginView view) {
        return view.getChildren().filter(LoginOverlay.class::isInstance).map(LoginOverlay.class::cast).findFirst().orElseThrow();
    }

    private static Element oidcEntry(LoginView view) {
        return loginOverlay(view).getElement().getChildren().filter(element -> "a".equals(element.getTag()))
                .findFirst().orElseThrow();
    }
}
