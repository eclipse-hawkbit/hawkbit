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
 * the {@link EntityAlreadyExistsException} is thrown when an entity is tried to be saved which already exists or which violates unique key
 * constraints.
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EntityAlreadyExistsException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final SpServerError THIS_ERROR = SpServerError.SP_REPO_ENTITY_ALREADY_EXISTS;

    /**
     * Default constructor.
     */
    public EntityAlreadyExistsException() {
        super(THIS_ERROR);
    }

    /**
     * Parameterized constructor.
     *
     * @param cause of the exception
     */
    public EntityAlreadyExistsException(final Throwable cause) {
        super(THIS_ERROR, cause);
    }

    /**
     * Parameterized constructor.
     *
     * @param message of the exception
     * @param cause of the exception
     */
    public EntityAlreadyExistsException(final String message, final Throwable cause) {
        super(message, THIS_ERROR, cause);
    }

    /**
     * Parameterized constructor.
     *
     * @param message of the exception
     */
    public EntityAlreadyExistsException(final String message) {
        super(message, THIS_ERROR);
    }
}
