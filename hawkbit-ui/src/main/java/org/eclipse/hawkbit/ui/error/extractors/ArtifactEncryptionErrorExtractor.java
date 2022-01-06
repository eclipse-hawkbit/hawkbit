/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.error.extractors;

import java.util.Optional;

import org.eclipse.hawkbit.repository.exception.ArtifactEncryptionFailedException;
import org.eclipse.hawkbit.repository.exception.ArtifactEncryptionUnsupportedException;
import org.eclipse.hawkbit.ui.error.UiErrorDetails;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

/**
 * UI error details extractor for {@link ArtifactEncryptionUnsupportedException}
 * and {@link ArtifactEncryptionFailedException}.
 */
public class ArtifactEncryptionErrorExtractor extends AbstractSingleUiErrorDetailsExtractor {
    private final VaadinMessageSource i18n;

    /**
     * Constructor for {@link ArtifactEncryptionErrorExtractor}.
     *
     * @param i18n
     *            Message source used for localization
     */
    public ArtifactEncryptionErrorExtractor(final VaadinMessageSource i18n) {
        this.i18n = i18n;
    }

    @Override
    protected Optional<UiErrorDetails> findDetails(final Throwable error) {
        return findExceptionOf(error, ArtifactEncryptionUnsupportedException.class)
                .map(ex -> UiErrorDetails.create(i18n.getMessage(UIMessageIdProvider.CAPTION_ERROR),
                        i18n.getMessage(UIMessageIdProvider.MESSAGE_ERROR_ENCRYPTION_NOT_SUPPORTED)))
                .map(Optional::of)
                .orElseGet(() -> findExceptionOf(error, ArtifactEncryptionFailedException.class)
                        .map(ex -> UiErrorDetails.create(i18n.getMessage(UIMessageIdProvider.CAPTION_ERROR),
                                getEncryptionFailedDetailsMsg(ex))));
    }

    private String getEncryptionFailedDetailsMsg(final ArtifactEncryptionFailedException ex) {
        switch (ex.getEncryptionOperation()) {
        case GENERATE_SECRETS:
            return i18n.getMessage(UIMessageIdProvider.MESSAGE_ERROR_ENCRYPTION_SECRETS_FAILED);
        case ENCRYPT:
            return i18n.getMessage(UIMessageIdProvider.MESSAGE_ERROR_ENCRYPTION_FAILED);
        case DECRYPT:
            return i18n.getMessage(UIMessageIdProvider.MESSAGE_ERROR_DECRYPTION_FAILED);
        default:
            return i18n.getMessage(UIMessageIdProvider.MESSAGE_ERROR_ENCRYPTION_FAILED);
        }
    }
}
