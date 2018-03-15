/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * Thrown if too many entries are added to repository.
 *
 */
public final class AssignmentQuotaExceededException extends AbstractServerRtException {

    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "Quota exceeded: Cannot assign %s more %s entities to %s '%s'.";

    /**
     * Creates a new QuotaExceededException with
     * {@link SpServerError#SP_QUOTA_EXCEEDED} error.
     */
    public AssignmentQuotaExceededException() {
        super(SpServerError.SP_QUOTA_EXCEEDED);
    }

    public AssignmentQuotaExceededException(final Class<?> type, final Class<?> parentType, final long parentId,
            final int requested) {
        this(type.getSimpleName(), parentType.getSimpleName(), parentId, requested);
    }

    private AssignmentQuotaExceededException(final String type, final String parentType, final long parentId,
            final int requested) {
        super(String.format(MESSAGE, requested, type, parentType, parentId), SpServerError.SP_QUOTA_EXCEEDED);
    }

}
