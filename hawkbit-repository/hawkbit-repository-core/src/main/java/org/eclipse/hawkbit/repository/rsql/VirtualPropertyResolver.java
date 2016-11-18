/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.rsql;

import java.time.Instant;

import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;
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
public class VirtualPropertyResolver extends StrLookup<String> implements VirtualPropertyReplacer {

    private StrSubstitutor substitutor;

    @Override
    public String lookup(String rhs) {
        String resolved = null;

        if ("now_ts".equalsIgnoreCase(rhs)) {
            resolved = String.valueOf(Instant.now().toEpochMilli());
        } else if ("overdue_ts".equalsIgnoreCase(rhs)) {
            resolved = String.valueOf(TimestampCalculator.calculateOverdueTimestamp());
        }
        return resolved;
    }

    @Override
    public String replace(String input) {
        if (substitutor == null) {
            substitutor = new StrSubstitutor(this, StrSubstitutor.DEFAULT_PREFIX, StrSubstitutor.DEFAULT_SUFFIX,
                    StrSubstitutor.DEFAULT_ESCAPE);
        }
        return substitutor.replace(input);
    }

}
