/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.test.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Provider-agnostic JDBC statement recorder used by performance/caching tests to count SQL actually sent to the DB.
 * <p>
 * Counts at the JDBC layer (see {@link QueryCountingDataSource}) rather than through a JPA-provider profiler, so the
 * same assertions hold under both EclipseLink and Hibernate. Reset it before the measured section and query the counts
 * afterwards.
 */
public class QueryCount {

    private final List<String> statements = Collections.synchronizedList(new ArrayList<>());

    /** Clears all recorded statements - call right before the section under measurement. */
    public void reset() {
        statements.clear();
    }

    /** Total number of executed statements recorded since the last {@link #reset()}. */
    public long total() {
        return statements.size();
    }

    /** Number of executed {@code SELECT} statements since the last {@link #reset()}. */
    public long selects() {
        synchronized (statements) {
            return statements.stream().filter(QueryCount::isSelect).count();
        }
    }

    /**
     * Number of executed statements whose SQL contains the given (case-insensitive) fragment - typically a table name
     * such as {@code sp_software_module_type}.
     */
    public long matching(final String sqlFragment) {
        final String needle = sqlFragment.toLowerCase(Locale.ROOT);
        synchronized (statements) {
            return statements.stream().filter(s -> s != null && s.toLowerCase(Locale.ROOT).contains(needle)).count();
        }
    }

    /** Immutable snapshot of the recorded statements - useful for debugging an unexpected count. */
    public List<String> statements() {
        synchronized (statements) {
            return List.copyOf(statements);
        }
    }

    void record(final String sql) {
        if (sql != null) {
            statements.add(sql);
        }
    }

    private static boolean isSelect(final String sql) {
        return sql != null && sql.stripLeading().regionMatches(true, 0, "select", 0, "select".length());
    }
}
