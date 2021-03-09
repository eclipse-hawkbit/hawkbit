package org.eclipse.hawkbit.ui;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public abstract class AbstractSingleUiErrorDetailsExtractor implements UiErrorDetailsExtractor {

    @Override
    public List<UiErrorDetails> extractErrorDetailsFrom(final Throwable error) {
        return findDetails(error).map(Collections::singletonList).orElseGet(Collections::emptyList);
    }

    protected abstract Optional<UiErrorDetails> findDetails(Throwable error);
}
