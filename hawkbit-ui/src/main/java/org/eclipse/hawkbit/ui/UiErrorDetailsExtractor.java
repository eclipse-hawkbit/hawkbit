package org.eclipse.hawkbit.ui;

import java.util.List;
import java.util.Optional;

public interface UiErrorDetailsExtractor {

    List<UiErrorDetails> extractErrorDetailsFrom(final Throwable error);

    default <T> Optional<T> findExceptionOf(final Throwable error, final Class<T> exceptionType) {
        // TODO: check if isAssignableFrom is working as intended
        if (error.getClass().isAssignableFrom(exceptionType)) {
            return Optional.of((T) error);
        }

        if (error.getCause() != null) {
            return findExceptionOf(error.getCause(), exceptionType);
        }

        return Optional.empty();
    }
}
