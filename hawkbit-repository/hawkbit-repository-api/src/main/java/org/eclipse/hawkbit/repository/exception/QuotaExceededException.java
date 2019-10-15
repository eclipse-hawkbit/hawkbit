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
 * Thrown if quota is exceeded (too many entries, file size, storage size).
 *
 */
public final class QuotaExceededException extends AbstractServerRtException {

    private static final long serialVersionUID = 1L;

    private static final String KB = "KB";
    private static final String MB = "MB";

    private static final String ASSIGNMENT_QUOTA_EXCEEDED_MESSAGE = "Quota exceeded: Cannot assign %s more %s entities to %s '%s'. The maximum is %s.";
    private static final String MAX_ARTIFACT_SIZE_EXCEEDED = "Maximum artifact size (%s) exceeded.";
    private static final String MAX_ARTIFACT_SIZE_TOTAL_EXCEEDED = "Storage quota exceeded, %s left.";

    private final QuotaType quotaType;

    private final long exceededQuotaValue;

    /**
     * Enum that describes the types of quota that may be exceeded
     */
    public enum QuotaType {
        STORAGE_QUOTA(SpServerError.SP_STORAGE_QUOTA_EXCEEDED, "message.upload.storageQuota"),
        SIZE_QUOTA(SpServerError.SP_FILE_SIZE_QUOTA_EXCEEDED, "message.upload.fileSizeQuota"),
        ASSIGNMENT_QUOTA(SpServerError.SP_QUOTA_EXCEEDED, "message.upload.quota");

        private final SpServerError errorType;
        public final String messageId;

        QuotaType(final SpServerError errorType, final String messageId) {
            this.errorType = errorType;
            this.messageId = messageId;
        }
    }

    /**
     * Creates a new QuotaExceededException with
     * {@link SpServerError#SP_QUOTA_EXCEEDED} error.
     */
    public QuotaExceededException() {
        super(QuotaType.ASSIGNMENT_QUOTA.errorType);
        this.quotaType = QuotaType.ASSIGNMENT_QUOTA;
        this.exceededQuotaValue = 0;
    }

    /**
     * Creates a new QuotaExceededException with a custom error message.
     *
     * @param message
     *            The custom error message.
     */
    public QuotaExceededException(final String message) {
        super(message, QuotaType.ASSIGNMENT_QUOTA.errorType);
        this.quotaType = QuotaType.ASSIGNMENT_QUOTA;
        this.exceededQuotaValue = 0;
    }

    /**
     * Creates a new QuotaExceededException with a custom error message and
     * quota type.
     *
     * @param quotaType
     *            {@link QuotaType} that will lead to the connected error type
     * @param exceededQuotaValue
     *            Value by how much the quota was exceeded
     */
    public QuotaExceededException(final QuotaType quotaType, final long exceededQuotaValue) {
        super(createQuotaErrorMessage(quotaType, exceededQuotaValue), quotaType.errorType);
        this.quotaType = quotaType;
        this.exceededQuotaValue = exceededQuotaValue;
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
        super(message, QuotaType.ASSIGNMENT_QUOTA.errorType, cause);
        this.quotaType = QuotaType.ASSIGNMENT_QUOTA;
        this.exceededQuotaValue = 0;
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
                + quota + "}.", QuotaType.ASSIGNMENT_QUOTA.errorType);
        this.quotaType = QuotaType.ASSIGNMENT_QUOTA;
        this.exceededQuotaValue = quota;
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
    public QuotaExceededException(final String type, final String parentType, final Object parentId,
            final long requested, final long quota) {
        super(String.format(ASSIGNMENT_QUOTA_EXCEEDED_MESSAGE, requested, type, parentType,
                parentId != null ? String.valueOf(parentId) : "<new>", quota), QuotaType.ASSIGNMENT_QUOTA.errorType);
        this.quotaType = QuotaType.ASSIGNMENT_QUOTA;
        this.exceededQuotaValue = quota;
    }

    public QuotaType getQuotaType() {
        return quotaType;
    }

    public String getExceededQuotaValueString() {
        return byteValueToReadableString(exceededQuotaValue);
    }

    private static String createQuotaErrorMessage(final QuotaType quotaType, final long exceededQuotaValue) {
        if (quotaType == QuotaExceededException.QuotaType.STORAGE_QUOTA) {
            return String.format(MAX_ARTIFACT_SIZE_TOTAL_EXCEEDED, byteValueToReadableString(exceededQuotaValue));
        } else {
            return String.format(MAX_ARTIFACT_SIZE_EXCEEDED, byteValueToReadableString(exceededQuotaValue));
        }
    }

    /**
     * Convert byte values to human readable strings with units
     */
    private static String byteValueToReadableString(long byteValue) {
        double outputValue = byteValue / 1024.0;
        String unit = KB;
        if (outputValue >= 1024) {
            outputValue = outputValue / 1024.0;
            unit = MB;
        }
        // We cut decimal places to avoid localization issues
        return (long) outputValue + " " + unit;
    }
}
