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
 * Exception used by the REST API in case of RSQL search filter query.
 * 
 *
 *
 *
 */
public class RSQLParameterSyntaxException extends AbstractServerRtException {

    /**
    * 
    */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new RSQLSyntaxException with
     * {@link SpServerError#SP_REST_RSQL_SEARCH_PARAM_SYNTAX} error.
     */
    public RSQLParameterSyntaxException() {
        super(SpServerError.SP_REST_RSQL_SEARCH_PARAM_SYNTAX);
    }

    /**
     * Creates a new RSQLSyntaxException with
     * {@link SpServerError#SP_REST_RSQL_SEARCH_PARAM_SYNTAX} error.
     * 
     * @param cause
     *            the cause of this exception
     */
    public RSQLParameterSyntaxException(final Throwable cause) {
        super(SpServerError.SP_REST_RSQL_SEARCH_PARAM_SYNTAX, cause);
    }

    /**
     * Creates a new RSQLParameterSyntaxException with
     * {@link SpServerError#SP_REST_RSQL_SEARCH_PARAM_SYNTAX} error.
     * 
     * @param message
     *            the message of the exception
     * @param cause
     *            the cause (which is saved for later retrieval by the
     *            getCause() method). (A null value is permitted, and indicates
     *            that the cause is nonexistent or unknown.)
     */
    public RSQLParameterSyntaxException(final String message, final Throwable cause) {
        super(message, SpServerError.SP_REST_RSQL_SEARCH_PARAM_SYNTAX, cause);
    }
}
