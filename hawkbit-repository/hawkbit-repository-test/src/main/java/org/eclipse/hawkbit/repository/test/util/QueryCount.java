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
 * Provider-agnostic JDBC statement recorder used by performance/caching tests to inspect the SQL actually sent to the DB.
 * <p>
 * Records at the JDBC layer (see {@link QueryCountingDataSource}) rather than through a JPA-provider profiler, so the
 * same assertions hold under both EclipseLink and Hibernate. Reset it before the measured section, then inspect the
 * recorded statements afterwards - as a count ({@link #countSelect()} / {@link #countSelectsFromTable(String)})
 * or as the raw list ({@link #getAllStatements()}).
 */
public class QueryCount {

    private final List<String> statements = Collections.synchronizedList(new ArrayList<>());

    /** Clears all recorded statements - call right before the section under measurement. */
    public void resetQueries() {
        statements.clear();
    }

    /** Number of executed {@code SELECT} statements since the last {@link #resetQueries()}. */
    public long countSelect() {
        synchronized (statements) {
            return statements.stream().filter(QueryCount::isSelect).count();
        }
    }

    /**
     * Number of statements (since the last {@link #resetQueries()}) that read FROM the given table - i.e. contain
     * {@code "from <table>"}. Distinguishes e.g. a {@code sp_software_module_type} by-id load from a query that merely
     * has a {@code software_module_type} column.
     */
    public long countSelectsFromTable(final String table) {
        final String needle = "from " + table.toLowerCase(Locale.ROOT);
        synchronized (statements) {
            return statements.stream().filter(s -> s != null && s.toLowerCase(Locale.ROOT).contains(needle)).count();
        }
    }

    /** Immutable snapshot of the recorded statements - useful for debugging an unexpected count. */
    public List<String> getAllStatements() {
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
