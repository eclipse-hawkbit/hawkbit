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

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.repository.model.BaseEntity;

/**
 * Thrown if assignment quota is exceeded
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AssignmentQuotaExceededException extends AbstractServerRtException {

    private static final String ASSIGNMENT_QUOTA_EXCEEDED_MESSAGE = "Quota exceeded: Cannot assign %s more %s entities to %s '%s'. The maximum is %s.";
    private static final SpServerError errorType = SpServerError.SP_QUOTA_EXCEEDED;

    public AssignmentQuotaExceededException() {
        super(errorType);
    }

    public AssignmentQuotaExceededException(final String message) {
        super(errorType, message);
    }

    public AssignmentQuotaExceededException(final String message, final Throwable cause) {
        super(errorType, message, cause);
    }

    public AssignmentQuotaExceededException(final Class<? extends BaseEntity> type, final long inserted, final int quota) {
        this(type.getSimpleName(), inserted, quota);
    }

    public AssignmentQuotaExceededException(final String type, final long inserted, final int quota) {
        super(errorType, "Request contains too many entries of {" + type + "}. {" + inserted + "} is beyond the permitted {" + quota + "}.");
    }

    public AssignmentQuotaExceededException(final Class<?> type, final Class<?> parentType, final Long parentId,
            final long requested, final long quota) {
        this(type.getSimpleName(), parentType.getSimpleName(), parentId, requested, quota);
    }

    public AssignmentQuotaExceededException(
            final String type, final String parentType, final Object parentId, final long requested, final long quota) {
        super(
                errorType, String.format(
                        ASSIGNMENT_QUOTA_EXCEEDED_MESSAGE,
                        requested, type, parentType, parentId != null ? String.valueOf(parentId) : "<new>", quota)
        );
    }
}