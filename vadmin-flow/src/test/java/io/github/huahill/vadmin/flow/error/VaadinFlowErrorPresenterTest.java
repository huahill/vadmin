package io.github.huahill.vadmin.flow.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class VaadinFlowErrorPresenterTest {
    @AfterEach
    void clearCurrentUi() {
        UI.setCurrent(null);
    }

    @Test
    void showsAChineseSystemFailureNotificationWithoutLeavingThePage() {
        var notifications = new ArrayList<String>();
        var ui = new RecordingUi(Locale.SIMPLIFIED_CHINESE);
        UI.setCurrent(ui);

        new VaadinFlowErrorPresenter(notifications::add).present(new FlowError(
                FlowErrorPresentation.FAILURE, 500, "internal.error", Map.of()));

        assertThat(ui.navigatedView).isNull();
        assertThat(notifications).containsExactly("系统异常，请稍后再试");
        assertThat(ResourceBundle.getBundle("i18n.flow", Locale.SIMPLIFIED_CHINESE)
                .getString("flow.system-failure.notification")).isEqualTo("系统异常，请稍后再试");
    }

    @Test
    void showsAnEnglishSystemFailureNotificationWithoutLeavingThePage() {
        var notifications = new ArrayList<String>();
        var ui = new RecordingUi(Locale.US);
        UI.setCurrent(ui);

        new VaadinFlowErrorPresenter(notifications::add).present(new FlowError(
                FlowErrorPresentation.FAILURE, 500, "internal.error", Map.of()));

        assertThat(ui.navigatedView).isNull();
        assertThat(notifications).containsExactly("A system error occurred. Please try again later.");
        assertThat(ResourceBundle.getBundle("i18n.flow", Locale.US)
                .getString("flow.system-failure.notification"))
                .isEqualTo("A system error occurred. Please try again later.");
    }

    @Test
    void usesTheSystemFailureNotificationWhenTheFailureKeyHasNoTranslation() {
        var notifications = new ArrayList<String>();
        var ui = new RecordingUi(Locale.SIMPLIFIED_CHINESE);
        UI.setCurrent(ui);

        new VaadinFlowErrorPresenter(notifications::add).present(new FlowError(
                FlowErrorPresentation.FAILURE, 404, "resource.not_found", Map.of()));

        assertThat(ui.navigatedView).isNull();
        assertThat(notifications).containsExactly("系统异常，请稍后再试");
    }

    @Test
    void usesTheSystemFailureNotificationWhenTheFailureKeyIsBlank() {
        var notifications = new ArrayList<String>();
        var ui = new RecordingUi(Locale.SIMPLIFIED_CHINESE);
        UI.setCurrent(ui);

        new VaadinFlowErrorPresenter(notifications::add).present(new FlowError(
                FlowErrorPresentation.FAILURE, 500, " ", Map.of()));

        assertThat(notifications).containsExactly("系统异常，请稍后再试");
    }

    @Test
    void showsATranslatedBusinessFailureInsteadOfTheSystemFailureNotification() {
        var notifications = new ArrayList<String>();
        var ui = new RecordingUi(Locale.SIMPLIFIED_CHINESE);
        UI.setCurrent(ui);

        new VaadinFlowErrorPresenter(notifications::add).present(new FlowError(
                FlowErrorPresentation.FAILURE, 404, "flow.workspace.load-failed", Map.of()));

        assertThat(ui.navigatedView).isNull();
        assertThat(notifications).containsExactly("无法加载数据");
    }

    @Test
    void usesTheSystemFailureNotificationWhenVaadinReportsTheKeyAsMissing() {
        var notifications = new ArrayList<String>();
        var ui = new RecordingUi(Locale.US, true);
        UI.setCurrent(ui);

        new VaadinFlowErrorPresenter(notifications::add).present(new FlowError(
                FlowErrorPresentation.FAILURE, 409, "conflict.version", Map.of()));

        assertThat(notifications).containsExactly("A system error occurred. Please try again later.");
    }

    @Test
    void stillOpensTheAccessDeniedViewWithoutANotification() {
        var notifications = new ArrayList<String>();
        var ui = new RecordingUi(Locale.SIMPLIFIED_CHINESE);
        UI.setCurrent(ui);

        new VaadinFlowErrorPresenter(notifications::add).present(new FlowError(
                FlowErrorPresentation.ACCESS_DENIED, 403, "authorization.denied", Map.of()));

        assertThat(ui.navigatedView).isEqualTo(AccessDeniedView.class);
        assertThat(notifications).isEmpty();
    }

    @Test
    void keepsAValidationFailureOnTheCurrentPage() {
        var notifications = new ArrayList<String>();
        var ui = new RecordingUi(Locale.SIMPLIFIED_CHINESE);
        UI.setCurrent(ui);

        new VaadinFlowErrorPresenter(notifications::add).present(new FlowError(
                FlowErrorPresentation.FIELD_VALIDATION, 400, "validation.failed", Map.of()));

        assertThat(ui.navigatedView).isNull();
        assertThat(notifications).isEmpty();
    }

    private static final class RecordingUi extends UI {
        private final Locale locale;
        private final boolean vaadinMissingMarker;
        private Class<?> navigatedView;

        private RecordingUi(Locale locale) {
            this(locale, false);
        }

        private RecordingUi(Locale locale, boolean vaadinMissingMarker) {
            this.locale = locale;
            this.vaadinMissingMarker = vaadinMissingMarker;
        }

        @Override
        public Locale getLocale() {
            return locale;
        }

        @Override
        public <T extends Component> Optional<T> navigate(Class<T> navigationTarget) {
            navigatedView = navigationTarget;
            return Optional.empty();
        }

        @Override
        public String getTranslation(String key, Object... params) {
            try {
                var bundle = ResourceBundle.getBundle("i18n.flow", locale);
                if (bundle.containsKey(key)) {
                    return bundle.getString(key);
                }
            } catch (MissingResourceException ignored) {
                // Fall through to the same missing-translation marker used by the admin provider.
            }
            if (vaadinMissingMarker) {
                return "!" + key + "!";
            }
            return "!" + locale.toLanguageTag() + ": " + key + "!";
        }
    }
}
