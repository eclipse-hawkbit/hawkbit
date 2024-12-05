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

import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for the Rest Source API.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HttpUtil {

    /**
     * Checks given CSV string for defined match value or wildcard.
     *
     * @param matchHeader to search through
     * @param toMatch to search for
     * @return <code>true</code> if string matches.
     */
    public static boolean matchesHttpHeader(final String matchHeader, final String toMatch) {
        return Stream.of(matchHeader.split(",")).map(String::trim).anyMatch(chunk -> chunk.equals(toMatch) || chunk.equals("*"));
    }
}