/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

/**
 * Helper class that provides simple conversion of byte values to readable
 * strings
 */
public final class SizeConversionHelper {

    private static final String KB = "KB";
    private static final String MB = "MB";

    // do not allow to create instances
    private SizeConversionHelper() {
    }

    /**
     * Convert byte values to human readable strings with units
     *
     * @param byteValue Value to convert in bytes
     */
    public static String byteValueToReadableString(final long byteValue) {
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
