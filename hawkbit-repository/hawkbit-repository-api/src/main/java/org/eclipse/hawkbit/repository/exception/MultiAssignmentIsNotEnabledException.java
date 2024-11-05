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
 * This exception is thrown if an operation requires multiassignments, but the
 * feature is not enabled.
 */
public class MultiAssignmentIsNotEnabledException extends AbstractServerRtException {

    private static final long serialVersionUID = 1L;
    private static final SpServerError THIS_ERROR = SpServerError.SP_MULTIASSIGNMENT_NOT_ENABLED;

    /**
     * Default constructor.
     */
    public MultiAssignmentIsNotEnabledException() {
        super(THIS_ERROR);
    }

    /**
     * Parameterized constructor.
     *
     * @param cause of the exception
     */
    public MultiAssignmentIsNotEnabledException(final Throwable cause) {
        super(THIS_ERROR, cause);
    }

    /**
     * Parameterized constructor.
     *
     * @param message of the exception
     * @param cause of the exception
     */
    public MultiAssignmentIsNotEnabledException(final String message, final Throwable cause) {
        super(message, THIS_ERROR, cause);
    }

    /**
     * Parameterized constructor.
     *
     * @param message of the exception
     */
    public MultiAssignmentIsNotEnabledException(final String message) {
        super(message, THIS_ERROR);
    }
}
