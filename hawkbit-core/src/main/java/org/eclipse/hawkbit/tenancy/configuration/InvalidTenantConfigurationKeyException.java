package org.eclipse.hawkbit.tenancy.configuration;

import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.exception.SpServerRtException;

/**
 * The {@link #InvalidTenantConfigurationKeyException} is thrown when an invalid
 * configuration key is used.
 *
 */
public class InvalidTenantConfigurationKeyException extends SpServerRtException {

    private static final long serialVersionUID = 1L;
    private static final SpServerError THIS_ERROR = SpServerError.SP_CONFIGURATION_KEY_INVALID;

    /**
     * Default constructor.
     */
    public InvalidTenantConfigurationKeyException() {
        super(THIS_ERROR);
    }

    /**
     * Parameterized constructor.
     * 
     * @param cause
     *            of the exception
     */
    public InvalidTenantConfigurationKeyException(final Throwable cause) {
        super(THIS_ERROR, cause);
    }

    /**
     * Parameterized constructor.
     * 
     * @param message
     *            of the exception
     * @param cause
     *            of the exception
     */
    public InvalidTenantConfigurationKeyException(final String message, final Throwable cause) {
        super(message, THIS_ERROR, cause);
    }

    /**
     * Parameterized constructor.
     * 
     * @param message
     *            of the exception
     */
    public InvalidTenantConfigurationKeyException(final String message) {
        super(message, THIS_ERROR);
    }

}
