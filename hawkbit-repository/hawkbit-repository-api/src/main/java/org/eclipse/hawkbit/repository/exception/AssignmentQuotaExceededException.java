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
import org.eclipse.hawkbit.repository.model.BaseEntity;

/**
 * Thrown if assignment quota is exceeded
 */
public class AssignmentQuotaExceededException extends AbstractServerRtException {

    private static final String ASSIGNMENT_QUOTA_EXCEEDED_MESSAGE = "Quota exceeded: Cannot assign %s more %s entities to %s '%s'. The maximum is %s.";
    private static final SpServerError errorType = SpServerError.SP_QUOTA_EXCEEDED;

    /**
     * Creates a new AssignmentQuotaExceededException with
     * {@link SpServerError#SP_QUOTA_EXCEEDED} error.
     */
    public AssignmentQuotaExceededException() {
        super(errorType);
    }

    /**
     * Creates a new AssignmentQuotaExceededException with a custom error
     * message.
     *
     * @param message
     *            The custom error message.
     */
    public AssignmentQuotaExceededException(final String message) {
        super(message, errorType);
    }

    /**
     * Creates a AssignmentQuotaExceededException with a custom error message
     * and a root cause.
     *
     * @param message
     *            The custom error message.
     * @param cause
     *            for the exception
     */
    public AssignmentQuotaExceededException(final String message, final Throwable cause) {
        super(message, errorType, cause);
    }

    /**
     * @param type
     *            that hit quota
     * @param inserted
     *            cause for the hit
     * @param quota
     *            that is defined by the repository
     */
    public AssignmentQuotaExceededException(final Class<? extends BaseEntity> type, final long inserted,
            final int quota) {
        this(type.getSimpleName(), inserted, quota);
    }

    /**
     *
     * @param type
     *            that hit quota
     * @param inserted
     *            cause for the hit
     * @param quota
     *            that is defined by the repository
     */
    public AssignmentQuotaExceededException(final String type, final long inserted, final int quota) {
        super("Request contains too many entries of {" + type + "}. {" + inserted + "} is beyond the permitted {"
                + quota + "}.", errorType);
    }

    /**
     * Creates a AssignmentQuotaExceededException which is to be thrown when an
     * assignment quota is exceeded.
     *
     * @param type
     *            The type of the entities that shall be assigned to the
     *            specified parent entity.
     * @param parentType
     *            The type of the parent entity.
     * @param parentId
     *            The ID of the parent entity.
     * @param requested
     *            The number of entities that shall be assigned to the specified
     *            parent entity.
     * @param quota
     *            The maximum number of entities that can be assigned to the
     *            parent entity.
     */
    public AssignmentQuotaExceededException(final Class<?> type, final Class<?> parentType, final Long parentId,
            final long requested, final long quota) {
        this(type.getSimpleName(), parentType.getSimpleName(), parentId, requested, quota);
    }

    /**
     * Creates a AssignmentQuotaExceededException which is to be thrown when an
     * assignment quota is exceeded.
     *
     * @param type
     *            The type of the entities that shall be assigned to the
     *            specified parent entity.
     * @param parentType
     *            The type of the parent entity.
     * @param parentId
     *            The ID of the parent entity.
     * @param requested
     *            The number of entities that shall be assigned to the specified
     *            parent entity.
     * @param quota
     *            The maximum number of entities that can be assigned to the
     *            parent entity.
     */
    public AssignmentQuotaExceededException(final String type, final String parentType, final Object parentId,
            final long requested, final long quota) {
        super(String.format(ASSIGNMENT_QUOTA_EXCEEDED_MESSAGE, requested, type, parentType,
                parentId != null ? String.valueOf(parentId) : "<new>", quota), errorType);
    }
}
