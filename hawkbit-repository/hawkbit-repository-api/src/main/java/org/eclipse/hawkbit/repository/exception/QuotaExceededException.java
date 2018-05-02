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
 * Thrown if too many entries are added to repository.
 *
 */
public final class QuotaExceededException extends AbstractServerRtException {

    private static final long serialVersionUID = 1L;

    private static final String ASSIGNMENT_QUOTA_EXCEEDED_MESSAGE = "Quota exceeded: Cannot assign %s more %s entities to %s '%s'. The maximum is %s.";

    /**
     * Creates a new QuotaExceededException with
     * {@link SpServerError#SP_QUOTA_EXCEEDED} error.
     */
    public QuotaExceededException() {
        super(SpServerError.SP_QUOTA_EXCEEDED);
    }

    /**
     * Creates a new QuotaExceededException with a custom error message.
     * 
     * @param message
     *            The custom error message.
     */
    public QuotaExceededException(final String message) {
        super(message, SpServerError.SP_QUOTA_EXCEEDED);
    }

    /**
     * Creates a QuotaExceededException with a custom error message and a root
     * cause.
     * 
     * @param message
     *            The custom error message.
     * @param cause
     *            for the exception
     */
    public QuotaExceededException(final String message, final Throwable cause) {
        super(message, SpServerError.SP_QUOTA_EXCEEDED, cause);
    }

    /**
     * @param type
     *            that hit quota
     * @param inserted
     *            cause for the hit
     * @param quota
     *            that is defined by the repository
     */
    public QuotaExceededException(final Class<? extends BaseEntity> type, final long inserted, final int quota) {
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
    public QuotaExceededException(final String type, final long inserted, final int quota) {
        super("Request contains too many entries of {" + type + "}. {" + inserted + "} is beyond the permitted {"
                + quota + "}.", SpServerError.SP_QUOTA_EXCEEDED);
    }

    /**
     * Creates a QuotaExceededException which is to be thrown when an assignment
     * quota is exceeded.
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
    public QuotaExceededException(final Class<?> type, final Class<?> parentType, final Long parentId,
            final long requested, final long quota) {
        this(type.getSimpleName(), parentType.getSimpleName(), parentId, requested, quota);
    }

    /**
     * Creates a QuotaExceededException which is to be thrown when an assignment
     * quota is exceeded.
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
    public QuotaExceededException(final String type, final String parentType, final Long parentId, final long requested,
            final long quota) {
        super(String.format(ASSIGNMENT_QUOTA_EXCEEDED_MESSAGE, requested, type, parentType,
                parentId != null ? String.valueOf(parentId) : "<new>", quota), SpServerError.SP_QUOTA_EXCEEDED);
    }

}
