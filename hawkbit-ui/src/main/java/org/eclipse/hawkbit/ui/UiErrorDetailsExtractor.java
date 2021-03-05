package org.eclipse.hawkbit.ui;

public interface UiErrorDetailsExtractor {

    UiErrorDetails extractErrorDetailsFrom(final Throwable error);
}
