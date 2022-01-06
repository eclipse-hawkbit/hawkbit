/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
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
 * {@link StopRolloutException} is thrown when an error occurs while stopping
 * the rollout (due to an invalidation of distribution set). This could be
 * caused by a long ongoing creation of a rollout.
 */
public class StopRolloutException extends AbstractServerRtException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new StopRolloutException with
     * {@link SpServerError#SP_STOP_ROLLOUT_FAILED} error.
     */
    public StopRolloutException() {
        super(SpServerError.SP_STOP_ROLLOUT_FAILED);
    }

    /**
     * Creates a new StopRolloutException with
     * {@link SpServerError#SP_STOP_ROLLOUT_FAILED} error.
     *
     * @param cause
     *            for the exception
     */
    public StopRolloutException(final Throwable cause) {
        super(SpServerError.SP_STOP_ROLLOUT_FAILED, cause);
    }

    /**
     * Creates a new StopRolloutException with
     * {@link SpServerError#SP_STOP_ROLLOUT_FAILED} error.
     *
     * @param message
     *            of the error
     */
    public StopRolloutException(final String message) {
        super(message, SpServerError.SP_STOP_ROLLOUT_FAILED);
    }
}
