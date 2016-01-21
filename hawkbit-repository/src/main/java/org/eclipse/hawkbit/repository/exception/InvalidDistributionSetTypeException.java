package org.eclipse.hawkbit.repository.exception;

import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.exception.SpServerRtException;

/**
 * This Exception is thrown, when the user wants to set a distribution set which
 * does not exist,
 *
 */
public class InvalidDistributionSetTypeException extends SpServerRtException {
    /**
    *
    */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new CancelActionNotAllowed with
     * {@link SpServerError#SP_ACTION_NOT_CANCELABLE} error.
     */
    public InvalidDistributionSetTypeException() {
        super(SpServerError.SP_REST_CONFIG_INVALID_DS_TYPE);
    }

    /**
     * @param cause
     *            for the exception
     */
    public InvalidDistributionSetTypeException(final Throwable cause) {
        super(SpServerError.SP_REST_CONFIG_INVALID_DS_TYPE, cause);
    }

    /**
     * @param message
     *            of the error
     */
    public InvalidDistributionSetTypeException(final String message) {
        super(message, SpServerError.SP_REST_CONFIG_INVALID_DS_TYPE);
    }
}
