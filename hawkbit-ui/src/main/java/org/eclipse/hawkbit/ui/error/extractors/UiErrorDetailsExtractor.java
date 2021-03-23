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

public interface UiErrorDetailsExtractor {

    List<UiErrorDetails> extractErrorDetailsFrom(final Throwable error);

    default <T> Optional<T> findExceptionOf(final Throwable error, final Class<T> exceptionType) {
        if (error.getClass().isAssignableFrom(exceptionType)) {
            return Optional.of((T) error);
        }

        if (error.getCause() != null) {
            return findExceptionOf(error.getCause(), exceptionType);
        }

        return Optional.empty();
    }
}
