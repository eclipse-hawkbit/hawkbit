/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * Exception which is thrown in case an request body is not well formaned and
 * cannot be parsed.
 * 
 *
 *
 *
 */
public class MessageNotReadableException extends AbstractServerRtException {

    /**
    * 
    */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new MessageNotReadableException with
     * {@link SpServerError#SP_REST_BODY_NOT_READABLE} error.
     */
    public MessageNotReadableException() {
        super(SpServerError.SP_REST_BODY_NOT_READABLE);
    }
}
