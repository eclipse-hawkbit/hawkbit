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

import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.ui.error.UiErrorDetails;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

/**
 * UI error details extractor for {@link EntityNotFoundException}.
 */
public class EntityNotFoundErrorExtractor extends AbstractSingleUiErrorDetailsExtractor {
    private final VaadinMessageSource i18n;

    /**
     * Constructor for EntityNotFoundErrorExtractor.
     *
     * @param i18n
     *            Message source used for localization
     */
    public EntityNotFoundErrorExtractor(final VaadinMessageSource i18n) {
        this.i18n = i18n;
    }

    @Override
    protected Optional<UiErrorDetails> findDetails(final Throwable error) {
        return findExceptionOf(error, EntityNotFoundException.class)
                .map(ex -> UiErrorDetails.create(i18n.getMessage("caption.entity.missing.error"), ex.getMessage()));
    }
}
