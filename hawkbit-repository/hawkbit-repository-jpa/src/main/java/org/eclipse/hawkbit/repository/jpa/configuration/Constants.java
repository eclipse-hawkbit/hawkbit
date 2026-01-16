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

    /**
     * Defines the maximum entries for SQL in-statement. Many database have
     * limit of elements within an SQL in-statement, e.g. Oracle with maximum
     * 1000 elements. So everywhere we are using in-statements in our SQL
     * queries we need to split these elements for the in-statements by this
     * number.
     */
    public static final int MAX_ENTRIES_IN_STATEMENT = 999;

    /**
     * See {@link Retryable#maxRetries()}
     */
    public static final String RETRY_MAX = "${org.eclipse.hawkbit.repository.jpa.retry-max:10}";
    /**
     * See {@link Retryable#delayString()}
     */
    public static final String RETRY_DELAY = "${org.eclipse.hawkbit.repository.jpa.retry-delay:100}";
}