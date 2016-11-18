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
 * Thrown when force quitting an actions is not allowed. e.g. the action is not
 * active or it is not canceled before.
 *
 */
public final class ForceQuitActionNotAllowedException extends AbstractServerRtException {
    /**
    *
    */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new CancelActionNotAllowed with
     * {@link SpServerError#SP_ACTION_NOT_CANCELABLE} error.
     */
    public ForceQuitActionNotAllowedException() {
        super(SpServerError.SP_ACTION_NOT_FORCE_QUITABLE);
    }

    /**
     * @param cause
     *            for the exception
     */
    public ForceQuitActionNotAllowedException(final Throwable cause) {
        super(SpServerError.SP_ACTION_NOT_FORCE_QUITABLE, cause);
    }

    /**
     * @param message
     *            of the error
     */
    public ForceQuitActionNotAllowedException(final String message) {
        super(message, SpServerError.SP_ACTION_NOT_FORCE_QUITABLE);
    }
}
