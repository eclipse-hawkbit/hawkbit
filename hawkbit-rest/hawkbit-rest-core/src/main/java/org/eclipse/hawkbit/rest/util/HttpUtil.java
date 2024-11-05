/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest.util;

import java.util.Arrays;

/**
 * Utility class for the Rest Source API.
 */
public final class HttpUtil {

    private HttpUtil() {

    }

    /**
     * Checks given CSV string for defined match value or wildcard.
     *
     * @param matchHeader to search through
     * @param toMatch to search for
     * @return <code>true</code> if string matches.
     */
    public static boolean matchesHttpHeader(final String matchHeader, final String toMatch) {
        final String[] matchValues = matchHeader.split("\\s*,\\s*");
        Arrays.sort(matchValues);
        return Arrays.binarySearch(matchValues, toMatch) > -1 || Arrays.binarySearch(matchValues, "*") > -1;
    }

}
