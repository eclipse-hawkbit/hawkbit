/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
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
 * This exception is indicating that the confirmation feedback cannot be
 * processed for a specific actions for different reasons which are listed as
 * enum {@link Reason}.
 */
public class InvalidConfirmationFeedbackException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final SpServerError THIS_ERROR = SpServerError.SP_CONFIRMATION_FEEDBACK_INVALID;

    private final Reason reason;

    protected InvalidConfirmationFeedbackException(final Reason reason) {
        super(THIS_ERROR);
        this.reason = reason;
    }

    public InvalidConfirmationFeedbackException(final Reason reason, final String message) {
        super(message, THIS_ERROR);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        ACTION_CLOSED, NOT_AWAITING_CONFIRMATION
    }
}
