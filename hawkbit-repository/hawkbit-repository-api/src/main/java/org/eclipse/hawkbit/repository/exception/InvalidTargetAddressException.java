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
 * Exception which is thrown when trying to set an invalid target address.
 */
public class InvalidTargetAddressException extends AbstractServerRtException {
    private static final long serialVersionUID = 1L;

    /**
     * @param message
     *            the message for this exception
     */
    public InvalidTargetAddressException(final String message) {
        super(message, SpServerError.SP_REPO_INVALID_TARGET_ADDRESS);
    }

    /**
     * 
     * @param message
     *            the message for this exception
     * @param cause
     *            the cause for this exception
     */
    public InvalidTargetAddressException(final String message, final Throwable cause) {
        super(message, SpServerError.SP_REPO_INVALID_TARGET_ADDRESS, cause);
    }
}
