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
 * the {@link ConstraintViolationException} is thrown when an entity is tried to
 * be saved which has constraint violations
 *
 */
public class ConstraintViolationException extends AbstractServerRtException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor for {@link ConstraintViolationException}
     * 
     * @param message
     *            the message to be displayed as exception message
     */
    public ConstraintViolationException(final String message) {
        super(message, SpServerError.SP_REPO_CONSTRAINT_VIOLATION);
    }

}
