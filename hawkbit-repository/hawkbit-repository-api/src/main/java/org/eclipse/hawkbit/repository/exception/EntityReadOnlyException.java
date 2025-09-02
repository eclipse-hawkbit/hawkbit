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
 * the {@link EntityReadOnlyException} is thrown when an entity is in read only mode and a user tries to change it.
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EntityReadOnlyException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final SpServerError THIS_ERROR = SpServerError.SP_REPO_ENTITY_READ_ONLY;

    public EntityReadOnlyException() {
        super(THIS_ERROR);
    }

    public EntityReadOnlyException(final Throwable cause) {
        super(THIS_ERROR, cause);
    }

    public EntityReadOnlyException(final String message, final Throwable cause) {
        super(THIS_ERROR, message, cause);
    }

    public EntityReadOnlyException(final String message) {
        super(THIS_ERROR, message);
    }
}