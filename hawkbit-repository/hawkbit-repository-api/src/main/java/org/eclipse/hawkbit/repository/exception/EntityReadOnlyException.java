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
 * the {@link EntityReadOnlyException} is thrown when a entity is in read only
 * mode and a user tries to change it.
 */
public class EntityReadOnlyException extends AbstractServerRtException {

    private static final long serialVersionUID = 1L;
    private static final SpServerError THIS_ERROR = SpServerError.SP_REPO_ENTITY_READ_ONLY;

    /**
     * Default constructor.
     */
    public EntityReadOnlyException() {
        super(THIS_ERROR);
    }

    /**
     * Parameterized constructor.
     * 
     * @param cause
     *            of the exception
     */
    public EntityReadOnlyException(final Throwable cause) {
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
    public EntityReadOnlyException(final String message, final Throwable cause) {
        super(message, THIS_ERROR, cause);
    }

    /**
     * Parameterized constructor.
     * 
     * @param message
     *            of the exception
     */
    public EntityReadOnlyException(final String message) {
        super(message, THIS_ERROR);
    }
}
