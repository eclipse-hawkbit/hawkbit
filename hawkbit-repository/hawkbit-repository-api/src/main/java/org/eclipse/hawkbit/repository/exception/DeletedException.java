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

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.repository.model.BaseEntity;

import java.io.Serial;

/**
 * Thrown if assignment quota is exceeded
 */
public class DeletedException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final SpServerError THIS_ERROR = SpServerError.SP_DELETED;

    public DeletedException(
            final Class<? extends BaseEntity> type, final Object entityId) {
        super(type.getSimpleName() + " with given identifier {" + entityId + "} is soft-deleted!",
                THIS_ERROR);
    }
}