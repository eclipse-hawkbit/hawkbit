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

    /**
     * Parameterized constructor.
     *
     * @param error detail
     */
    protected AbstractServerRtException(final SpServerError error) {
        super(error.getMessage());
        this.error = error;
        this.info = null;
    }

    /**
     * Parameterized constructor.
     *
     * @param message custom error message
     * @param error detail
     */
    protected AbstractServerRtException(final String message, final SpServerError error) {
        this(message, error, (Map<String, Object>) null);
    }

    /**
     * Parameterized constructor.
     *
     * @param message custom error message
     * @param error detail
     */
    protected AbstractServerRtException(final String message, final SpServerError error, final Map<String, Object> info) {
        super(message);
        this.error = error;
        this.info = info;
    }

    /**
     * Parameterized constructor.
     *
     * @param message custom error message
     * @param error detail
     * @param cause of the exception
     */
    protected AbstractServerRtException(final String message, final SpServerError error, final Throwable cause) {
        super(message, cause);
        this.error = error;
        this.info = null;
    }

    /**
     * Parameterized constructor.
     *
     * @param error detail
     * @param cause of the exception
     */
    protected AbstractServerRtException(final SpServerError error, final Throwable cause) {
        super(error.getMessage(), cause);
        this.error = error;
        this.info = null;
    }

    /**
     * Parameterized constructor.
     *
     * @param message custom error message
     * @param error detail
     * @param cause of the exception
     */
    protected AbstractServerRtException(final String message, final SpServerError error, final Throwable cause, final Map<String, Object> info) {
        super(message, cause);
        this.error = error;
        this.info = info;
    }
}