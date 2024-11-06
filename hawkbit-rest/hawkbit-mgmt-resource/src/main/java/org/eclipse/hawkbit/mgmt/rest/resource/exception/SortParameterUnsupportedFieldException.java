/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource.exception;

import java.io.Serial;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * Exception used by the REST API in case of invalid field name in the sort parameter.
 */
public class SortParameterUnsupportedFieldException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new SortParameterSyntaxErrorException with {@link SpServerError#SP_REST_SORT_PARAM_INVALID_FIELD} error.
     *
     * @param cause the cause (which is saved for later retrieval by the getCause() method). (A null value is permitted, and indicates
     *              that the cause is nonexistent or unknown.)
     */
    public SortParameterUnsupportedFieldException(final Throwable cause) {
        super(SpServerError.SP_REST_SORT_PARAM_INVALID_FIELD, cause);
    }
}