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
 * Exception used by the REST API in case of invalid sort parameter direction
 * name.
 */
public class SortParameterUnsupportedDirectionException extends AbstractServerRtException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new SortParameterSyntaxErrorException.
     */
    public SortParameterUnsupportedDirectionException() {
        super(SpServerError.SP_REST_SORT_PARAM_INVALID_DIRECTION);
    }

    /**
     * Creates a new SortParameterSyntaxErrorException with
     * {@link SpServerError#SP_REST_SORT_PARAM_INVALID_DIRECTION} error.
     *
     * @param cause the cause (which is saved for later retrieval by the
     *         getCause() method). (A null value is permitted, and indicates
     *         that the cause is nonexistent or unknown.)
     */
    public SortParameterUnsupportedDirectionException(final Throwable cause) {
        super(SpServerError.SP_REST_SORT_PARAM_INVALID_DIRECTION, cause);
    }
}
