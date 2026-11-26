/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.configuration;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.resilience.annotation.Retryable;

/**
 * A constant class which holds only static constants used within the SP server.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Constants {

    private static final String PROP_PREFIX = "org.eclipse.hawkbit.repository.jpa.";

    /**
     * Defines the maximum entries for SQL in-statement. Many database have limit of elements within an SQL in-statement.
     * So everywhere we are using in-statements in our SQL queries we need to split these elements for the in-statements by this number.
     */
    public static final int MAX_ENTRIES_IN_STATEMENT = Math.max(1, Integer.getInteger(PROP_PREFIX + "max-entries-in-statement", 999));

    /**
     * In order to fix concurrency exception we could try to split pages. We do it only if the page size has at least
     * {@link #MIN_ENTRIES_TO_SPLIT} entries.
     */
    public static final int MIN_ENTRIES_TO_SPLIT = Math.max(2, Integer.getInteger(PROP_PREFIX + "min-entries-to-split", 2));

    /**
     * See {@link Retryable#maxRetries()}
     */
    public static final String RETRY_MAX = "${" + PROP_PREFIX + "retry-max:10}";
    /**
     * See {@link Retryable#delayString()}
     */
    public static final String RETRY_DELAY = "${" + PROP_PREFIX + "retry-delay:100}";
}
