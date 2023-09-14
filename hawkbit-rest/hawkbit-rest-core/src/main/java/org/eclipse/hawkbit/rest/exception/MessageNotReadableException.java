/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
