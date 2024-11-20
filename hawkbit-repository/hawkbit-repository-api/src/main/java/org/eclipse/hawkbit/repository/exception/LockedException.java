/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.exception;

import java.io.Serial;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.repository.model.BaseEntity;

/**
 * Thrown if there is attempt to functionally modify a locked entity
 */
public class LockedException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final SpServerError THIS_ERROR = SpServerError.SP_LOCKED;

    public LockedException(
            final Class<? extends BaseEntity> type, final Object entityId, final String operation) {
        super(type.getSimpleName() + " with given identifier {" + entityId + "} is locked and " + operation +
                        " is forbidden!",
                THIS_ERROR);
    }

    public LockedException(
            final Class<? extends BaseEntity> type, final Object entityId, final String operation, final String reason) {
        super(type.getSimpleName() + " with given identifier {" + entityId + "} is locked and " + operation +
                        " is forbidden! Reason: " + reason,
                THIS_ERROR);
    }
}