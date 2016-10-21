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
 * the {@link RolloutVerificationException} is thrown when a rollout or
 * its groups get created or modified with a configuration that is
 * not valid or can't be verified
 * 
 */
public class RolloutVerificationException extends AbstractServerRtException {

    private static final long serialVersionUID = 1L;
    private static final SpServerError THIS_ERROR = SpServerError.SP_ROLLOUT_VERIFICATION_FAILED;

    /**
     * Default constructor.
     */
    public RolloutVerificationException() {
        super(THIS_ERROR);
    }

    /**
     * Parameterized constructor.
     *
     * @param cause
     *            of the exception
     */
    public RolloutVerificationException(final Throwable cause) {
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
    public RolloutVerificationException(final String message, final Throwable cause) {
        super(message, THIS_ERROR, cause);
    }

    /**
     * Parameterized constructor.
     *
     * @param message
     *            of the exception
     */
    public RolloutVerificationException(final String message) {
        super(message, THIS_ERROR);
    }
}
