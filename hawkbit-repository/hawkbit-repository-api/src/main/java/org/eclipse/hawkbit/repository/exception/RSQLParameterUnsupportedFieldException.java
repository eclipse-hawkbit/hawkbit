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
 * Exception used by the REST API in case of invalid field name in the rsql search parameter.
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RSQLParameterUnsupportedFieldException extends QueryException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RSQLParameterUnsupportedFieldException() {
        super(SpServerError.SP_REST_RSQL_PARAM_INVALID_FIELD);
    }

    public RSQLParameterUnsupportedFieldException(final String message) {
        super(SpServerError.SP_REST_RSQL_PARAM_INVALID_FIELD, message);
    }

    public RSQLParameterUnsupportedFieldException(final Throwable cause) {
        super(SpServerError.SP_REST_RSQL_PARAM_INVALID_FIELD, cause);
    }

    public RSQLParameterUnsupportedFieldException(final String message, final Throwable cause) {
        super(SpServerError.SP_REST_RSQL_PARAM_INVALID_FIELD, message, cause);
    }
}