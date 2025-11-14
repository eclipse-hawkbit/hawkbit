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

    public RSQLParameterSyntaxException() {
        super(SpServerError.SP_REST_RSQL_SEARCH_PARAM_SYNTAX);
    }

    public RSQLParameterSyntaxException(final String message) {
        super(SpServerError.SP_REST_RSQL_SEARCH_PARAM_SYNTAX, message);
    }

    public RSQLParameterSyntaxException(final Throwable cause) {
        super(SpServerError.SP_REST_RSQL_SEARCH_PARAM_SYNTAX, cause);
    }

    public RSQLParameterSyntaxException(final String message, final Throwable cause) {
        super(SpServerError.SP_REST_RSQL_SEARCH_PARAM_SYNTAX, message, cause);
    }
}