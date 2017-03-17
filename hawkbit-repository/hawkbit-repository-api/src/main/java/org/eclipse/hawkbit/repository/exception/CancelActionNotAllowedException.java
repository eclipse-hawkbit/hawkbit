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
 * Thrown if cancellation of action is requested where the action cannot be
 * cancelled (e.g. the action is not active or is already a canceled action) or
 * controller provides cancellation feedback on an action that is actually not
 * in canceling state.
 *
 */
public final class CancelActionNotAllowedException extends AbstractServerRtException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new CancelActionNotAllowed with
     * {@link SpServerError#SP_ACTION_NOT_CANCELABLE} error.
     */
    public CancelActionNotAllowedException() {
        super(SpServerError.SP_ACTION_NOT_CANCELABLE);
    }

    /**
     * @param cause
     *            for the exception
     */
    public CancelActionNotAllowedException(final Throwable cause) {
        super(SpServerError.SP_ACTION_NOT_CANCELABLE, cause);
    }

    /**
     * @param message
     *            of the error
     */
    public CancelActionNotAllowedException(final String message) {
        super(message, SpServerError.SP_ACTION_NOT_CANCELABLE);
    }
}
