/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.exception;

import static org.eclipse.hawkbit.repository.SizeConversionHelper.byteValueToReadableString;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * Thrown if file size quota is exceeded
 */
public class FileSizeQuotaExceededException extends AbstractServerRtException {

    private static final String MAX_ARTIFACT_SIZE_EXCEEDED = "Maximum artifact size (%s) exceeded.";
    private static final SpServerError errorType = SpServerError.SP_FILE_SIZE_QUOTA_EXCEEDED;

    private final long exceededQuotaValue;

    /**
     * Creates a new FileSizeQuotaExceededException with a quota value.
     *
     * @param exceededQuotaValue Value by how much the quota was exceeded
     */
    public FileSizeQuotaExceededException(final long exceededQuotaValue) {
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
        return String.format(MAX_ARTIFACT_SIZE_EXCEEDED, byteValueToReadableString(exceededQuotaValue));
    }
}
