/**
 * Copyright (c) 2025 Bosch Digital GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.artifact.exception;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * Thrown if file size quota is exceeded
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FileSizeQuotaExceededException extends AbstractServerRtException {

    private static final String MAX_ARTIFACT_SIZE_EXCEEDED = "Maximum artifact size (%s) exceeded.";
    private static final SpServerError errorType = SpServerError.SP_FILE_SIZE_QUOTA_EXCEEDED;

    private static final String KB = "KB";
    private static final String MB = "MB";

    private final long exceededQuotaValue;

    public FileSizeQuotaExceededException(final long exceededQuotaValue) {
        super(errorType, createQuotaErrorMessage(exceededQuotaValue));
        this.exceededQuotaValue = exceededQuotaValue;
    }

    private static String createQuotaErrorMessage(final long exceededQuotaValue) {
        return String.format(MAX_ARTIFACT_SIZE_EXCEEDED, byteValueToReadableString(exceededQuotaValue));
    }

    static String byteValueToReadableString(final long byteValue) {
        double outputValue = byteValue / 1024.0;
        String unit = KB;
        if (outputValue >= 1024) {
            outputValue = outputValue / 1024.0;
            unit = MB;
        }
        // We cut decimal places to avoid localization handling
        return (long) outputValue + " " + unit;
    }
}
