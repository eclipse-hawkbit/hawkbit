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
 * Thrown if cancellation of action is requested where the action cannot be
 * cancelled (e.g. the action is not active or is already a canceled action) or
 * controller provides cancellation feedback on an action that is actually not
 * in canceling state.
 */
public final class CancelActionNotAllowedException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new CancelActionNotAllowed with
     * {@link SpServerError#SP_ACTION_NOT_CANCELABLE} error.
     */
    public CancelActionNotAllowedException() {
        super(SpServerError.SP_ACTION_NOT_CANCELABLE);
    }

    /**
     * @param cause for the exception
     */
    public CancelActionNotAllowedException(final Throwable cause) {
        super(SpServerError.SP_ACTION_NOT_CANCELABLE, cause);
    }

    /**
     * @param message of the error
     */
    public CancelActionNotAllowedException(final String message) {
        super(message, SpServerError.SP_ACTION_NOT_CANCELABLE);
    }
}
