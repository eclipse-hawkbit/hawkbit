/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
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
 * Thrown if target type is assigned
 */
public class TargetTypeInUseException extends AbstractServerRtException {

    private static final long serialVersionUID = 1L;
    private static final SpServerError THIS_ERROR = SpServerError.SP_TARGET_TYPE_IN_USE;

    /**
     * Default constructor.
     */
    public TargetTypeInUseException() {
        super(THIS_ERROR);
    }

    /**
     * Parameterized constructor.
     *
     * @param cause
     *            of the exception
     */
    public TargetTypeInUseException(final Throwable cause) {
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
    public TargetTypeInUseException(final String message, final Throwable cause) {
        super(message, THIS_ERROR, cause);
    }

    /**
     * Parameterized constructor.
     *
     * @param message
     *            of the exception
     */
    public TargetTypeInUseException(final String message) {
        super(message, THIS_ERROR);
    }
}

