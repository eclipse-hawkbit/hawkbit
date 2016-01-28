package org.eclipse.hawkbit.tenancy.configuration.validator;

import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.exception.SpServerRtException;

/**
 * Exception which is thrown, when the validation of the configuration value has
 * not been successful.
 *
 */
public class TenantConfigurationValidatorException extends SpServerRtException {

    private static final long serialVersionUID = 1L;
    private static final SpServerError THIS_ERROR = SpServerError.SP_CONFIGURATION_VALUE_INVALID;

    /**
     * Default constructor.
     */
    public TenantConfigurationValidatorException() {
        super(THIS_ERROR);
    }

    /**
     * Parameterized constructor.
     * 
     * @param cause
     *            of the exception
     */
    public TenantConfigurationValidatorException(final Throwable cause) {
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
    public TenantConfigurationValidatorException(final String message, final Throwable cause) {
        super(message, THIS_ERROR, cause);
    }

    /**
     * Parameterized constructor.
     * 
     * @param message
     *            of the exception
     */
    public TenantConfigurationValidatorException(final String message) {
        super(message, THIS_ERROR);
    }

}
