/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

public class InvalidDistributionSetException extends AbstractServerRtException {
    /**
    *
    */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new InvalidDistributionSetException with
     * {@link SpServerError#SP_DS_INVALID} error.
     */
    public InvalidDistributionSetException() {
        super(SpServerError.SP_DS_INVALID);
    }

    /**
     * @param cause
     *            for the exception
     */
    public InvalidDistributionSetException(final Throwable cause) {
        super(SpServerError.SP_DS_INVALID, cause);
    }

    /**
     * @param message
     *            of the error
     */
    public InvalidDistributionSetException(final String message) {
        super(message, SpServerError.SP_DS_INVALID);
    }

}
