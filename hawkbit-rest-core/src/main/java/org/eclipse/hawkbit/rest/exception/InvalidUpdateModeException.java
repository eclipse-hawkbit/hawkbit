/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * the {@link InvalidUpdateModeException} is thrown when the client sends an
 * invalid update mode.
 */
public class InvalidUpdateModeException extends AbstractServerRtException {

    private static final long serialVersionUID = 1L;
    private static final SpServerError THIS_ERROR = SpServerError.SP_REST_UPDATE_MODE_INVALID;

    private static final String MESSAGE_TEMPLATE = "The update mode '%s' is invalid.";

    /**
     * Default constructor.
     */
    public InvalidUpdateModeException(final String invalidMode) {
        super(String.format(MESSAGE_TEMPLATE, invalidMode), THIS_ERROR);
    }

    /**
     * Parameterized constructor.
     * 
     * @param cause
     *            of the exception
     */
    public InvalidUpdateModeException(final String invalidMode, final Throwable cause) {
        super(String.format(MESSAGE_TEMPLATE, invalidMode), THIS_ERROR, cause);
    }

}
