package org.eclipse.hawkbit.ui;

import java.util.Optional;

import com.vaadin.server.UploadException;

public class UploadErrorExtractor extends AbstractUiErrorDetailsExtractor {

    @Override
    protected Optional<UiErrorDetails> processError(final Throwable error) {
        return findExceptionFrom(error, UploadException.class).map(ex -> UiErrorDetails.ignored());
    }
}
