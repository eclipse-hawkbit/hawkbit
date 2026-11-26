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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provider-agnostic JDBC statement recorder used by performance/caching tests to inspect the SQL actually sent to the DB.
 * <p>
 * Records at the JDBC layer (see {@link QueryCountingDataSource}) rather than through a JPA-provider profiler, so the
 * same assertions hold under both EclipseLink and Hibernate. Reset it before the measured section, then inspect the
 * recorded statements afterwards - as a count ({@link #countSelect()} / {@link #countSelectsFromTable(String)})
 * or as the raw list ({@link #getAllStatements()}), or grouped by statement {@link Kind} and primary table
 * ({@link #snapshot()}).
 */
public class QueryCount {

    /** Statement kind, taken from the leading SQL keyword. */
    public enum Kind {
        SELECT, INSERT, UPDATE, DELETE, OTHER
    }

    // primary table of a statement: the first table after FROM (SELECT, DELETE), INTO (INSERT) or UPDATE
    private static final Pattern PRIMARY_TABLE = Pattern.compile(
            "^\\s*(?:select\\b.*?\\bfrom|delete\\s+from|insert\\s+into|update)\\s+([\\w.\"`]+)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

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

    /**
     * Immutable counts by kind and primary table of the statements recorded since the last {@link #resetQueries()}.
     * A JDBC batch counts as one statement (one round trip).
     */
    public Snapshot snapshot() {
        final Map<Kind, Map<String, Long>> counts = new EnumMap<>(Kind.class);
        for (final String sql : getAllStatements()) {
            counts.computeIfAbsent(kindOf(sql), k -> new HashMap<>()).merge(primaryTableOf(sql), 1L, Long::sum);
        }
        final Map<Kind, Map<String, Long>> copy = new EnumMap<>(Kind.class);
        counts.forEach((kind, byTable) -> copy.put(kind, Map.copyOf(byTable)));
        return new Snapshot(Collections.unmodifiableMap(copy));
    }

    /**
     * Statement counts by {@link Kind} and primary table (lower case, empty string if not determinable).
     *
     * @param counts kind -> primary table -> count
     */
    public record Snapshot(Map<Kind, Map<String, Long>> counts) {

        /** Number of statements of the given kind. */
        public long count(final Kind kind) {
            return byTable(kind).values().stream().mapToLong(Long::longValue).sum();
        }

        /** Number of statements of the given kind whose primary table is the given one. */
        public long count(final Kind kind, final String table) {
            return byTable(kind).getOrDefault(table.toLowerCase(Locale.ROOT), 0L);
        }

        /**
         * Number of statements of the given kind whose primary table is none of the given ones - e.g. to exclude
         * {@code sp_tenant_configuration} reads, which are not cached in tests.
         */
        public long countExcluding(final Kind kind, final String... tables) {
            final Set<String> excluded = Stream.of(tables).map(t -> t.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
            return byTable(kind).entrySet().stream()
                    .filter(e -> !excluded.contains(e.getKey()))
                    .mapToLong(Map.Entry::getValue).sum();
        }

        private Map<String, Long> byTable(final Kind kind) {
            return counts.getOrDefault(kind, Map.of());
        }
    }

    void record(final String sql) {
        if (sql != null) {
            statements.add(sql);
        }
    }

    private static Kind kindOf(final String sql) {
        final String stripped = sql.stripLeading();
        for (final Kind kind : Kind.values()) {
            if (kind != Kind.OTHER && stripped.regionMatches(true, 0, kind.name(), 0, kind.name().length())) {
                return kind;
            }
        }
        return Kind.OTHER;
    }

    private static String primaryTableOf(final String sql) {
        final Matcher matcher = PRIMARY_TABLE.matcher(sql);
        return matcher.find() ? matcher.group(1).replace("\"", "").replace("`", "").toLowerCase(Locale.ROOT) : "";
    }

    private static boolean isSelect(final String sql) {
        return sql != null && sql.stripLeading().regionMatches(true, 0, "select", 0, "select".length());
    }
}
