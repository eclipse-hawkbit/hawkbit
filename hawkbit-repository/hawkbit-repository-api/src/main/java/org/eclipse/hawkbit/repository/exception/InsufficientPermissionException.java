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
 * Exception which is thrown in case the current security context object does
 * not hold a required authority/permission.
 * 
 *
 *
 */
public class InsufficientPermissionException extends AbstractServerRtException {

    private static final long serialVersionUID = 1L;

    /**
     * creates new InsufficientPermissionException.
     * 
     * @param cause
     *            the cause of the exception
     */
    public InsufficientPermissionException(final Throwable cause) {
        super(SpServerError.SP_INSUFFICIENT_PERMISSION, cause);
    }

    /**
     * creates new InsufficientPermissionException.
     */
    public InsufficientPermissionException() {
        this(null);
    }
}
