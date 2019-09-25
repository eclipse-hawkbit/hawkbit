/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
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
 * This exception is thrown if multi assignments is enabled and an target
 * distribution set assignment request does not contain a weight value
 * 
 */
public class NoWeightProvidedInMultiAssignmentModeException extends AbstractServerRtException {


    private static final long serialVersionUID = 1L;
    private static final SpServerError THIS_ERROR = SpServerError.SP_NO_WEIGHT_PROVIDED_IN_MULTIASSIGNMENT_MODE;

    /**
     * Default constructor.
     */
    public NoWeightProvidedInMultiAssignmentModeException() {
        super(THIS_ERROR);
    }

    /**
     * Parameterized constructor.
     * 
     * @param cause
     *            of the exception
     */
    public NoWeightProvidedInMultiAssignmentModeException(final Throwable cause) {
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
    public NoWeightProvidedInMultiAssignmentModeException(final String message, final Throwable cause) {
        super(message, THIS_ERROR, cause);
    }

    /**
     * Parameterized constructor.
     * 
     * @param message
     *            of the exception
     */
    public NoWeightProvidedInMultiAssignmentModeException(final String message) {
        super(message, THIS_ERROR);
    }
}
