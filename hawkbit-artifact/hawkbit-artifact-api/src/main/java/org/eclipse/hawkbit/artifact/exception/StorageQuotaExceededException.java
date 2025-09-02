/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.artifact.exception;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * Thrown if storage quota is exceeded
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StorageQuotaExceededException extends AbstractServerRtException {

    private static final String MAX_ARTIFACT_SIZE_TOTAL_EXCEEDED = "Storage quota exceeded, %s left.";
    private static final SpServerError errorType = SpServerError.SP_STORAGE_QUOTA_EXCEEDED;

    private final long exceededQuotaValue;

    public StorageQuotaExceededException(final long exceededQuotaValue) {
        super(errorType, createQuotaErrorMessage(exceededQuotaValue));
        this.exceededQuotaValue = exceededQuotaValue;
    }

    private static String createQuotaErrorMessage(final long exceededQuotaValue) {
        return String.format(MAX_ARTIFACT_SIZE_TOTAL_EXCEEDED, FileSizeQuotaExceededException.byteValueToReadableString(exceededQuotaValue));
    }
}