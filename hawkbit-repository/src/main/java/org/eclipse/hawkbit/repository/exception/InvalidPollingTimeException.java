package org.eclipse.hawkbit.repository.exception;

import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.exception.SpServerRtException;

public class InvalidPollingTimeException extends SpServerRtException {
    /**
    *
    */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new CancelActionNotAllowed with
     * {@link SpServerError#SP_ACTION_NOT_CANCELABLE} error.
     */
    public InvalidPollingTimeException() {
        super(SpServerError.SP_REST_CONFIG_POLLING_TIME_WRONG_FOMRATTED);
    }

    /**
     * @param cause
     *            for the exception
     */
    public InvalidPollingTimeException(final Throwable cause) {
        super(SpServerError.SP_REST_CONFIG_POLLING_TIME_WRONG_FOMRATTED, cause);
    }

    /**
     * @param message
     *            of the error
     */
    public InvalidPollingTimeException(final String message) {
        super(message, SpServerError.SP_REST_CONFIG_POLLING_TIME_WRONG_FOMRATTED);
    }
}
