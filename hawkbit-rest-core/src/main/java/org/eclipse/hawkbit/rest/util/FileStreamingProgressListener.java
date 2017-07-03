package org.eclipse.hawkbit.rest.util;

@FunctionalInterface
public interface FileStreamingProgressListener {
    void progress(Long requestedBytes, Long shippedBytesSinceLast, Long shippedBytesOverall);
}
