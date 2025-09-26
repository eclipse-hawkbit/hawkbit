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
public abstract class QueryException extends AbstractServerRtException {

    QueryException(final SpServerError error) {
        super(error);
    }

    QueryException(final SpServerError error, final String message) {
        super(error, message);
    }

    QueryException(final SpServerError error, final Throwable cause) {
        super(error, cause);
    }

    QueryException(final SpServerError error, final String message, final Throwable cause) {
        super(error, message, cause);
    }
}