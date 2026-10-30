package io.github.huahill.vadmin.flow.patterns;

import static org.assertj.core.api.Assertions.assertThat;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StatusNotificationTest {
    @BeforeEach
    void setUp() {
        UI.setCurrent(new UI());
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
    }

    @Test
    void showsOperationErrorsWithTheThemeNeutralVariantAndStatusIcon() {
        var notification = OperationFeedback.showError("保存被校验拒绝");

        assertThat(notification.getThemeNames()).containsExactly("error");
        assertThat(notification.getPosition()).isEqualTo(Notification.Position.TOP_CENTER);
        assertThat(notification.getDuration()).isEqualTo(5_000);
        assertThat(notification.isOpened()).isTrue();
        assertThat(statusIcon(notification)).isEqualTo("vaadin:warning");
    }

    @Test
    void showsOperationSuccessAndWarningWithTheirStatusIcons() {
        var success = OperationFeedback.showSuccess("已保存");
        assertThat(success.getThemeNames()).containsExactly("success");
        assertThat(success.getDuration()).isEqualTo(5_000);
        assertThat(statusIcon(success)).isEqualTo("vaadin:check-circle");

        var warning = OperationFeedback.showWarning("请确认");
        assertThat(warning.getThemeNames()).containsExactly("warning");
        assertThat(statusIcon(warning)).isEqualTo("vaadin:exclamation-circle");
    }

    @Test
    void showsAdminNotificationsWithTheThemeNeutralVariantAndStatusIcon() {
        var success = AdminNotifications.showSuccess("已保存", 3_000);
        assertThat(success.getThemeNames()).containsExactly("success");
        assertThat(success.getDuration()).isEqualTo(3_000);
        assertThat(statusIcon(success)).isEqualTo("vaadin:check-circle");

        var warning = AdminNotifications.showWarning("请确认");
        assertThat(warning.getThemeNames()).containsExactly("warning");
        assertThat(warning.getDuration()).isEqualTo(4_000);
        assertThat(statusIcon(warning)).isEqualTo("vaadin:exclamation-circle");

        var error = AdminNotifications.showError("保存被校验拒绝");
        assertThat(error.getThemeNames()).containsExactly("error");
        assertThat(error.getDuration()).isEqualTo(5_000);
        assertThat(statusIcon(error)).isEqualTo("vaadin:warning");
    }

    private static String statusIcon(Notification notification) {
        return notification.getChildren()
                .flatMap(component -> component.getChildren())
                .filter(Icon.class::isInstance)
                .map(Icon.class::cast)
                .findFirst()
                .orElseThrow()
                .getElement()
                .getAttribute("icon");
    }
}
