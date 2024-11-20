/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.exception;

import java.io.Serial;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * Thrown if a distribution set is assigned to a a target that is incomplete
 * (i.e. mandatory modules are missing).
 */
public final class IncompleteDistributionSetException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new IncompleteDistributionSetException with
     * {@link SpServerError#SP_DS_INCOMPLETE} error.
     */
    public IncompleteDistributionSetException() {
        super(SpServerError.SP_DS_INCOMPLETE);
    }

    /**
     * @param cause for the exception
     */
    public IncompleteDistributionSetException(final Throwable cause) {
        super(SpServerError.SP_DS_INCOMPLETE, cause);
    }

    /**
     * @param message of the error
     */
    public IncompleteDistributionSetException(final String message) {
        super(message, SpServerError.SP_DS_INCOMPLETE);
    }
}
