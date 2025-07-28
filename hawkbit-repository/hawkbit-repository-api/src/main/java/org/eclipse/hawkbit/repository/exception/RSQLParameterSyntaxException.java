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

import java.io.Serial;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * Exception used by the REST API in case of RSQL search filter query.
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RSQLParameterSyntaxException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new RSQLSyntaxException with
     * {@link SpServerError#SP_REST_RSQL_SEARCH_PARAM_SYNTAX} error.
     */
    public RSQLParameterSyntaxException() {
        super(SpServerError.SP_REST_RSQL_SEARCH_PARAM_SYNTAX);
    }

    /**
     * Creates a new RSQLParameterSyntaxException with
     * {@link SpServerError#SP_REST_RSQL_SEARCH_PARAM_SYNTAX} error.
     *
     * @param message the message of the exception
     */
    public RSQLParameterSyntaxException(final String message) {
        super(message, SpServerError.SP_REST_RSQL_SEARCH_PARAM_SYNTAX);
    }

    /**
     * Creates a new RSQLSyntaxException with
     * {@link SpServerError#SP_REST_RSQL_SEARCH_PARAM_SYNTAX} error.
     *
     * @param cause the cause of this exception
     */
    public RSQLParameterSyntaxException(final Throwable cause) {
        super(SpServerError.SP_REST_RSQL_SEARCH_PARAM_SYNTAX, cause);
    }

    /**
     * Creates a new RSQLParameterSyntaxException with
     * {@link SpServerError#SP_REST_RSQL_SEARCH_PARAM_SYNTAX} error.
     *
     * @param message the message of the exception
     * @param cause the cause (which is saved for later retrieval by the
     *         getCause() method). (A null value is permitted, and indicates
     *         that the cause is nonexistent or unknown.)
     */
    public RSQLParameterSyntaxException(final String message, final Throwable cause) {
        super(message, SpServerError.SP_REST_RSQL_SEARCH_PARAM_SYNTAX, cause);
    }
}
