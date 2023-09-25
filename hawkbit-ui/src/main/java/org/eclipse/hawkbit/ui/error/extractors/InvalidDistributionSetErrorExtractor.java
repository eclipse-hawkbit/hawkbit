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

import org.eclipse.hawkbit.repository.exception.InvalidDistributionSetException;
import org.eclipse.hawkbit.ui.error.UiErrorDetails;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

/**
 * UI error details extractor for {@link InvalidDistributionSetException}.
 */
public class InvalidDistributionSetErrorExtractor extends AbstractSingleUiErrorDetailsExtractor {
    private final VaadinMessageSource i18n;

    /**
     * Constructor for {@link InvalidDistributionSetException}.
     *
     * @param i18n
     *            Message source used for localization
     */
    public InvalidDistributionSetErrorExtractor(final VaadinMessageSource i18n) {
        this.i18n = i18n;
    }

    @Override
    protected Optional<UiErrorDetails> findDetails(final Throwable error) {
        return findExceptionOf(error, InvalidDistributionSetException.class)
                .map(ex -> UiErrorDetails.create(i18n.getMessage(UIMessageIdProvider.CAPTION_ERROR),
                        i18n.getMessage(UIMessageIdProvider.MESSAGE_ERROR_DISTRIBUTIONSET_INVALID, "")));
    }
}
