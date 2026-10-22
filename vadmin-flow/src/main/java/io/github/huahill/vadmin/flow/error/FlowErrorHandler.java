package io.github.huahill.vadmin.flow.error;

import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.ErrorHandler;
import io.github.huahill.vadmin.contracts.error.BusinessFailure;
import io.github.huahill.vadmin.contracts.error.ErrorCode;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FlowErrorHandler implements ErrorHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowErrorHandler.class);

    private final FlowErrorMapper mapper;
    private final FlowErrorPresenter presenter;

    public FlowErrorHandler() {
        this(FlowErrorPresenters.resolve());
    }

    FlowErrorHandler(FlowErrorPresenter presenter) {
        this(new FlowErrorMapper(), presenter);
    }

    FlowErrorHandler(FlowErrorMapper mapper, FlowErrorPresenter presenter) {
        this.mapper = mapper;
        this.presenter = presenter;
    }

    @Override
    public void error(ErrorEvent event) {
        var throwable = event.getThrowable();
        var businessFailure = findBusinessFailure(throwable);
        if (businessFailure.errorCode() == ErrorCode.INTERNAL_ERROR) {
            LOGGER.error("Unhandled Vaadin Flow exception", throwable);
        }
        presenter.present(mapper.map(businessFailure));
    }

    private BusinessFailure findBusinessFailure(Throwable throwable) {
        var candidate = throwable;
        while (candidate != null) {
            if (candidate instanceof BusinessFailure failure) {
                return failure;
            }
            candidate = candidate.getCause();
        }
        return new BusinessFailure(ErrorCode.INTERNAL_ERROR, "internal.error", Map.of());
    }
}
