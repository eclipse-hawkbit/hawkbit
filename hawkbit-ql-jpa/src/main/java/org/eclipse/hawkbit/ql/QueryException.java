/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ql;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Exception used by the REST API in case of RSQL search filter query.
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QueryException extends RuntimeException {

    public enum ErrorCode {
        INVALID_SYNTAX,
        UNSUPPORTED_FIELD,
        GENERIC // an other
    }

    @Getter
    private final ErrorCode errorCode;

    public QueryException(final ErrorCode errorCode, final String message) {
        this(errorCode, message, null);
    }

    public QueryException(final ErrorCode errorCode, final Throwable cause) {
        this(errorCode, null, cause);
    }

    public QueryException(final ErrorCode errorCode, final String message, final Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}