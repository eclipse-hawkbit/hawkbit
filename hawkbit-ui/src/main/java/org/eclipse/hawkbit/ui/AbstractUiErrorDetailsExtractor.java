package org.eclipse.hawkbit.ui;

import java.util.Optional;

public abstract class AbstractUiErrorDetailsExtractor implements UiErrorDetailsExtractor {

    @Override
    public UiErrorDetails extractErrorDetailsFrom(final Throwable error) {
        return processError(error).orElse(UiErrorDetails.unknown());
    }

    protected abstract Optional<UiErrorDetails> processError(Throwable error);

    protected <T> Optional<T> findExceptionFrom(final Throwable error, final Class<T> exceptionType) {
        // TODO: check if isAssignableFrom is working as intended
        if (error.getClass().isAssignableFrom(exceptionType)) {
            return Optional.of((T) error);
        }

        if (error.getCause() != null) {
            return findExceptionFrom(error.getCause(), exceptionType);
        }

        return Optional.empty();
    }
}
