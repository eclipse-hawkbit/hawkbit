/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * Thrown if a distribution set is assigned to a a target that is incomplete
 * (i.e. mandatory modules are missing).
 *
 *
 *
 *
 */
public final class IncompleteDistributionSetException extends AbstractServerRtException {
    /**
    *
    */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new IncompleteDistributionSetException with
     * {@link SpServerError#SP_DS_INCOMPLETE} error.
     */
    public IncompleteDistributionSetException() {
        super(SpServerError.SP_DS_INCOMPLETE);
    }

    /**
     * @param cause
     *            for the exception
     */
    public IncompleteDistributionSetException(final Throwable cause) {
        super(SpServerError.SP_DS_INCOMPLETE, cause);
    }

    /**
     * @param message
     *            of the error
     */
    public IncompleteDistributionSetException(final String message) {
        super(message, SpServerError.SP_DS_INCOMPLETE);
    }
}
