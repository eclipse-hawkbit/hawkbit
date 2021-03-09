package org.eclipse.hawkbit.ui;

import java.util.Optional;

import com.vaadin.server.UploadException;

public class UploadErrorExtractor extends AbstractSingleUiErrorDetailsExtractor {

    @Override
    protected Optional<UiErrorDetails> findDetails(final Throwable error) {
        return findExceptionOf(error, UploadException.class).map(ex -> UiErrorDetails.empty());
    }
}
