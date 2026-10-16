package io.github.huahill.vadmin.flow.patterns;

import static org.assertj.core.api.Assertions.assertThat;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.junit.jupiter.api.Test;

class DetailDialogTest {
    @Test
    void usesFormItemsForLabeledDetailValues() {
        var dialog = new DetailDialog("User details");

        var username = dialog.addField("Username", "ada");

        assertThat(username).isInstanceOf(FormLayout.FormItem.class);
        assertThat(username.getElement().getTag()).isEqualTo("vaadin-form-item");
        assertThat(username.getLabel()).isEqualTo("Username");
        assertThat(username.getValue()).isEqualTo("ada");
    }

    @Test
    void boundsDialogWidthForLongDetailValues() {
        var dialog = new DetailDialog("User details");

        assertThat(dialog.getWidth()).isEqualTo("52rem");
        assertThat(dialog.getMaxWidth()).isEqualTo("calc(100vw - 2rem)");
    }

    @Test
    void canPlaceLongDetailValueAcrossTheForm() {
        var dialog = new DetailDialog("User details");

        var config = dialog.addFullWidthField("Config", "{}");

        var row = dialog.getForm().getChildren().findFirst().orElseThrow();
        assertThat(row.getChildren()).containsExactly(config);
        assertThat(dialog.getForm().getColspan(config)).isEqualTo(2);
    }

    @Test
    void keepsResponsiveStepsForStableDetailRows() {
        var dialog = new DetailDialog("User details");

        assertThat(dialog.getForm().getElement().getProperty("autoResponsive", false)).isFalse();
        assertThat(dialog.getForm().getElement().getPropertyRaw("responsiveSteps").toString())
                .contains("40em")
                .contains("\"columns\":2");
    }

    @Test
    void composesSemanticDescriptionsInAResponsiveDetailSurface() {
        var dialog = new DetailDialog("User details");

        var username = dialog.addField("Username", "ada");
        var status = dialog.addField("Status", "Enabled");

        assertThat(dialog.getForm().getChildren()).containsExactly(username, status);
        assertThat(username.getLabel()).isEqualTo("Username");
        assertThat(username.getValue()).isEqualTo("ada");
        assertThat(status.getLabel()).isEqualTo("Status");
        assertThat(status.getValue()).isEqualTo("Enabled");
    }

    @Test
    void closesFromItsStandardAccessibleFooterAction() {
        var dialog = new DetailDialog("User details");

        dialog.getCloseAction().click();

        assertThat(dialog.isOpened()).isFalse();
    }

    @Test
    void usesTheNativeWrappingFooterLayout() {
        var dialog = new DetailDialog("User details");
        var footerActions = (HorizontalLayout) dialog.getFooter().getElement().getChildren()
                .findFirst().flatMap(element -> element.getComponent()).orElseThrow();

        assertThat(footerActions.isWrap()).isTrue();
    }
}
