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

import java.util.Optional;

import org.eclipse.hawkbit.ui.error.UiErrorDetails;

import com.vaadin.server.UploadException;

/**
 * UI error details extractor for {@link UploadException}.
 */
public class UploadErrorExtractor extends AbstractSingleUiErrorDetailsExtractor {

    @Override
    protected Optional<UiErrorDetails> findDetails(final Throwable error) {
        // UploadException is ignored as it is handled explicitly
        return findExceptionOf(error, UploadException.class).map(ex -> UiErrorDetails.empty());
    }
}
