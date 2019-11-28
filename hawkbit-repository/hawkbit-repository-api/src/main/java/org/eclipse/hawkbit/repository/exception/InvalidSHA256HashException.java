package org.eclipse.hawkbit.repository.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * Thrown if SHA256 checksum check fails.
 */
public class InvalidSHA256HashException extends AbstractServerRtException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new FileUploadFailedException with
     * {@link SpServerError#SP_ARTIFACT_UPLOAD_FAILED_SHA1_MATCH} error.
     */
    public InvalidSHA256HashException() {
        super(SpServerError.SP_ARTIFACT_UPLOAD_FAILED_SHA256_MATCH);
    }

    /**
     * @param message
     *            of the error
     * @param cause
     *            for the exception
     */
    public InvalidSHA256HashException(final String message, final Throwable cause) {
        super(message, SpServerError.SP_ARTIFACT_UPLOAD_FAILED_SHA256_MATCH, cause);
    }

    /**
     * @param message
     *            of the error
     */
    public InvalidSHA256HashException(final String message) {
        super(message, SpServerError.SP_ARTIFACT_UPLOAD_FAILED_SHA256_MATCH);
    }

}
