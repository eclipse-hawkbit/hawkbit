/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.exception;

import static org.eclipse.hawkbit.repository.SizeConversionHelper.byteValueToReadableString;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * Thrown if storage quota is exceeded
 */
public class StorageQuotaExceededException extends AbstractServerRtException {

    private static final String MAX_ARTIFACT_SIZE_TOTAL_EXCEEDED = "Storage quota exceeded, %s left.";
    private static final SpServerError errorType = SpServerError.SP_STORAGE_QUOTA_EXCEEDED;

    private final long exceededQuotaValue;

    /**
     * Creates a new StorageQuotaExceededException with a quota value.
     *
     * @param exceededQuotaValue
     *            Value by how much the quota was exceeded
     */
    public StorageQuotaExceededException(final long exceededQuotaValue) {
        super(createQuotaErrorMessage(exceededQuotaValue), errorType);
        this.exceededQuotaValue = exceededQuotaValue;
    }

    /**
     * Get a readable string of size quota including unit
     * 
     * @return file size quota with unit
     */
    public String getExceededQuotaValueString() {
        return byteValueToReadableString(exceededQuotaValue);
    }

    private static String createQuotaErrorMessage(final long exceededQuotaValue) {
        return String.format(MAX_ARTIFACT_SIZE_TOTAL_EXCEEDED, byteValueToReadableString(exceededQuotaValue));
    }
}
