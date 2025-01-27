/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.rsql;

import java.io.Serial;

import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookupFactory;
import org.eclipse.hawkbit.repository.TimestampCalculator;

/**
 * Adds macro capabilities to RSQL expressions that are used to filter for
 * targets.
 * <p>
 * Some (virtual) properties do not have a representation in the database (in
 * general these properties are time-related, or more explicitly, they deal with
 * time intervals).<br>
 * Such a virtual property needs to be calculated on Java-side before it may be
 * used in a target filter query that is passed to the database. Therefore a
 * placeholder is used in the RSQL expression that is expanded when the RSQL is
 * parsed
 * <p>
 * A virtual property may either be a system value like the current date (aka
 * <em>now_ts</em>) or a value derived from (tenant-specific) system
 * configuration (e.g. <em>overdue_ts</em>).
 * <p>
 * Known values are:<br>
 * <ul>
 * <li><em>now_ts</em>: maps to system UTC time in milliseconds since Unix epoch
 * as long value</li>
 * <li><em>overdue_ts</em>: is a calculated value: <em>overdue_ts = now_ts -
 * pollingInterval - pollingOverdueInterval</em>; pollingInterval and
 * pollingOverdueInterval are retrieved from tenant-specific system
 * configuration.</li>
 * </ul>
 */
public class VirtualPropertyResolver implements VirtualPropertyReplacer {

    @Serial
    private static final long serialVersionUID = 1L;

    private transient StringSubstitutor substitutor;

    @Override
    public String replace(final String input) {
        if (substitutor == null) {
            substitutor = new StringSubstitutor(
                    StringLookupFactory.builder().get().functionStringLookup(this::lookup),
                    StringSubstitutor.DEFAULT_PREFIX, StringSubstitutor.DEFAULT_SUFFIX, StringSubstitutor.DEFAULT_ESCAPE);
        }
        return substitutor.replace(input);
    }

    private String lookup(final String rhs) {
        if ("now_ts".equalsIgnoreCase(rhs)) {
            return String.valueOf(System.currentTimeMillis());
        } else if ("overdue_ts".equalsIgnoreCase(rhs)) {
            return String.valueOf(TimestampCalculator.calculateOverdueTimestamp());
        } else {
            return null;
        }
    }
}