/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * Exception which is supposed to be thrown if a property value is valid but
 * cannot be set in the current context.
 */
public class TenantConfigurationValueChangeNotAllowedException extends AbstractServerRtException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new exception for the
     * {@link SpServerError#SP_CONFIGURATION_VALUE_CHANGE_NOT_ALLOWED} error
     * case.
     */
    public TenantConfigurationValueChangeNotAllowedException() {
        super(SpServerError.SP_CONFIGURATION_VALUE_CHANGE_NOT_ALLOWED);
    }

    /**
     * Creates a new exception for the
     * {@link SpServerError#SP_CONFIGURATION_VALUE_CHANGE_NOT_ALLOWED} error
     * case.
     * 
     * @param message
     *            A custom error message.
     */
    public TenantConfigurationValueChangeNotAllowedException(final String message) {
        super(message, SpServerError.SP_CONFIGURATION_VALUE_CHANGE_NOT_ALLOWED);
    }

}
