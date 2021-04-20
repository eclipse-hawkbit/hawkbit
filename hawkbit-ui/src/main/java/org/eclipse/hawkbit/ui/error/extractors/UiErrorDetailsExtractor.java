/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.error.extractors;

import java.util.List;
import java.util.Optional;

import org.eclipse.hawkbit.ui.error.UiErrorDetails;

/**
 * Base interface for extracting ui error details from given error.
 */
@FunctionalInterface
public interface UiErrorDetailsExtractor {

    /**
     * Extracts ui error details from given error.
     * 
     * @param error
     *            error to extract details from
     * @return ui error details
     */
    List<UiErrorDetails> extractErrorDetailsFrom(final Throwable error);

    /**
     * Tries to find out if error matches the given exception type.
     * 
     * @param error
     *            error to match
     * @param exceptionType
     *            the type of exception to match
     * @return casted error if matched
     */
    default <T> Optional<T> findExceptionOf(final Throwable error, final Class<T> exceptionType) {
        if (exceptionType.isAssignableFrom(error.getClass())) {
            return Optional.of((T) error);
        }

        if (error.getCause() != null) {
            return findExceptionOf(error.getCause(), exceptionType);
        }

        return Optional.empty();
    }
}
