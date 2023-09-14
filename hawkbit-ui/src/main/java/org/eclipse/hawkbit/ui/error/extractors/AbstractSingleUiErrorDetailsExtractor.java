/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.error.extractors;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.hawkbit.ui.error.UiErrorDetails;

/**
 * Base class for single UI error details extractors.
 */
public abstract class AbstractSingleUiErrorDetailsExtractor implements UiErrorDetailsExtractor {

    @Override
    public List<UiErrorDetails> extractErrorDetailsFrom(final Throwable error) {
        return findDetails(error).map(Collections::singletonList).orElseGet(Collections::emptyList);
    }

    /**
     * Extracts single ui error details from given error.
     * 
     * @param error
     *            error to extract details from
     * @return ui error details if found
     */
    protected abstract Optional<UiErrorDetails> findDetails(Throwable error);
}
