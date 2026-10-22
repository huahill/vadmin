package io.github.huahill.vadmin.flow.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.vaadin.flow.server.ErrorEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class FlowErrorPresentersTest {
    @AfterEach
    void clearInstalledPresenter() {
        FlowErrorPresenters.reset();
    }

    @Test
    void usesAnInstalledPresenterForUnhandledSessionExceptions() {
        var presenter = new CapturingPresenter();
        FlowErrorPresenters.install(presenter);

        new FlowErrorHandler().error(new ErrorEvent(new IllegalStateException("unexpected")));

        assertThat(presenter.error.presentation()).isEqualTo(FlowErrorPresentation.FAILURE);
        assertThat(presenter.error.messageKey()).isEqualTo("internal.error");
    }

    @Test
    void resolvesTheVaadinPresenterWhenNothingIsInstalled() {
        assertThat(FlowErrorPresenters.resolve()).isInstanceOf(VaadinFlowErrorPresenter.class);
    }

    @Test
    void forgetsAnInstalledPresenterAfterReset() {
        FlowErrorPresenters.install(new CapturingPresenter());

        FlowErrorPresenters.reset();

        assertThat(FlowErrorPresenters.resolve()).isInstanceOf(VaadinFlowErrorPresenter.class);
    }

    private static final class CapturingPresenter implements FlowErrorPresenter {
        private FlowError error;

        @Override
        public void present(FlowError error) {
            this.error = error;
        }
    }
}
