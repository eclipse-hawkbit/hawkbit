/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.exception;

import java.io.Serial;
import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Generic Custom Exception to wrap the Runtime and checked exception
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractServerRtException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final SpServerError error;
    private final transient Map<String, Object> info;

    protected AbstractServerRtException(final SpServerError error) {
        this(error, error.getMessage());
    }

    protected AbstractServerRtException(final SpServerError error, final String message) {
        this(error, message, (Map<String, Object>) null);
    }

    protected AbstractServerRtException(final SpServerError error, final String message, final Map<String, Object> info) {
        this(error, message, info, null);
    }

    protected AbstractServerRtException(final SpServerError error, final String message, final Throwable cause) {
        this(error, message, null, cause);
    }

    protected AbstractServerRtException(final SpServerError error, final Throwable cause) {
        this(error, error.getMessage(), null, cause);
    }

    protected AbstractServerRtException(
            final SpServerError error, final String message, final Map<String, Object> info, final Throwable cause) {
        super(message, cause);
        this.error = error;
        this.info = info;
    }
}