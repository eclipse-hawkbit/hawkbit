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

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * the {@link RolloutIllegalStateException} is thrown when a rollout is changing
 * it's state which is not valid. E.g. trying to start a already running
 * rollout, or trying to resume a already finished rollout.
 */
public class RolloutIllegalStateException extends AbstractServerRtException {

    private static final long serialVersionUID = 1L;
    private static final SpServerError THIS_ERROR = SpServerError.SP_ROLLOUT_ILLEGAL_STATE;

    /**
     * Default constructor.
     */
    public RolloutIllegalStateException() {
        super(THIS_ERROR);
    }

    /**
     * Parameterized constructor.
     *
     * @param cause of the exception
     */
    public RolloutIllegalStateException(final Throwable cause) {
        super(THIS_ERROR, cause);
    }

    /**
     * Parameterized constructor.
     *
     * @param message of the exception
     * @param cause of the exception
     */
    public RolloutIllegalStateException(final String message, final Throwable cause) {
        super(message, THIS_ERROR, cause);
    }

    /**
     * Parameterized constructor.
     *
     * @param message of the exception
     */
    public RolloutIllegalStateException(final String message) {
        super(message, THIS_ERROR);
    }
}
