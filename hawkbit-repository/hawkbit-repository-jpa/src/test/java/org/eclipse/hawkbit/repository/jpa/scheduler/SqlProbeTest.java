/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManagerFactory;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.AutoAssignmentManagement.Create;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.test.util.QueryCount;
import org.eclipse.hawkbit.repository.test.util.QueryCountConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * SQL-count guards for the auto-assign and rollout schedulers, on a small fixed scenario (10 targets, 1 DS with
 * 3 modules / 3 module types, 2 groups). Each test asserts the <em>exact per-table SELECT footprint</em> of a scheduler
 * tick (a {table -> select count} map, covering every table touched), so any query added or removed by an
 * implementation change breaks the build and the failure names the exact table - not just a moved total. Both the
 * assigning tick and the steady tick (nothing to do) are asserted for each scheduler. Each assertion is preceded by a
 * {@code dump()} of every statement to ease debugging; the commented {@code print*} methods are assertion-free
 * per-phase dumps kept for ad-hoc inspection.
 * <p>
 * NOT provider-agnostic: the exact per-table counts and the recorded SQL (e.g. EclipseLink's {@code {oj ...}} JDBC
 * outer-join escape) are EclipseLink-specific. The whole class is therefore gated to run only under EclipseLink
 * (the default provider); it is skipped under Hibernate.
 */
@Slf4j
@Import(QueryCountConfiguration.class)
//Change the logging level to DEBUG for this test class to see the SQL statements in the logs and compare with passed execution.
@TestPropertySource(properties = { "logging.level.org.eclipse.hawkbit.repository.jpa.scheduler.SqlProbeTest=DEBUG" })
class SqlProbeTest extends AbstractJpaIntegrationTest {

    private static final String PREFIX = "roll-probe-";
    private static final int TARGETS = 10;
    private static final int GROUPS = 2;

    // expected per-table SELECT footprints (EclipseLink, this scenario) - captured from the dump; see assertSelectsByTable
    private static final Map<String, Long> ASSIGNING_TICK = Map.of(
            "sp_action", 20L,
            "sp_auto_assignment", 2L,
            "sp_distribution_set", 4L,
            "sp_distribution_set_type", 1L,
            "sp_ds_sm", 1L,
            "sp_ds_type_element", 1L,
            "sp_software_module_type", 3L,
            "sp_target", 17L,
            "sp_target_conf_status", 10L,
            "sp_tenant_configuration", 21L);
    private static final Map<String, Long> AUTO_ASSIGN_STEADY_TICK = Map.of(
            "sp_auto_assignment", 1L,
            "sp_distribution_set", 1L,
            "sp_target", 1L);
    private static final Map<String, Long> ROLLOUT_FIRST_TICK = Map.of(
            "sp_action", 14L,
            "sp_distribution_set", 4L,
            "sp_rollout", 5L,
            "sp_rollout_group", 9L,
            "sp_rollout_target_group", 2L,
            "sp_target", 10L,
            "sp_target_attributes", 5L,
            "sp_target_conf_status", 5L,
            "sp_target_metadata", 5L,
            "sp_tenant_configuration", 11L);
    private static final Map<String, Long> ROLLOUT_STEADY_TICK = Map.of(
            "sp_action", 3L,
            "sp_rollout", 2L,
            "sp_rollout_group", 2L,
            "sp_target", 1L);

    @Autowired
    private QueryCount queryCount;

    @Autowired
    private JpaAutoAssignHandler autoAssignHandler;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void eclipseLinkOnly() {
        // baselines (counts + {oj} outer-join SQL) are EclipseLink-specific; Hibernate emits different SQL/counts.
        // the injected EMF is a Spring proxy, so detect the provider by its vendor property keys, not the class name
        final boolean eclipseLink = entityManagerFactory.getProperties().keySet().stream()
                .anyMatch(key -> key.toLowerCase(Locale.ROOT).startsWith("eclipselink"));
        assumeTrue(eclipseLink, "SQL-count baselines are EclipseLink-specific - skipped under other JPA providers");
    }

    /**
     * Regression guard for the auto-assign handler, both ticks:
     * - assigning tick (10 targets) - type reads BOUNDED (once via isComplete, not per target);
     * - steady tick (nothing to assign) - the ~909/sec prod hot path, must NOT touch the DS type at all.
     * Asserts the exact per-table SELECT footprint (each table touched), so a regression names the table that changed.
     */
    @Test
    void autoAssignHandlerRun() {
        autoAssignScenario();

        queryCount.resetQueries();
        autoAssignHandler.handleAll(); // assigns all 10 targets
        dump("auto-assign assigning 10 targets - first handleAll() tick");
        assertSelectsByTable("auto-assign assigning 10 targets", ASSIGNING_TICK);

        queryCount.resetQueries();
        autoAssignHandler.handleAll(); // steady tick - nothing left to assign
        dump("auto-assign steady tick - second handleAll() tick - nothing to assign");
        assertSelectsByTable("auto-assign steady tick", AUTO_ASSIGN_STEADY_TICK);
    }

    /**
     * Regression guard for the rollout executor, both ticks:
     * - first running tick (starts group, creates the group's actions);
     * - steady running tick (group running, nothing to advance without feedback) - must NOT touch the DS type.
     * Asserts the exact per-table SELECT footprint (each table touched), so a regression names the table that changed.
     */
    @Test
    void rolloutHandlerRun() {
        final DistributionSet ds = testdataFactory.createDistributionSet("rollguard");
        testdataFactory.createTargets(PREFIX, 0, TARGETS);
        final Rollout rollout = testdataFactory.createRolloutByVariables(
                "rollguard", "rollguard", GROUPS, "controllerid==" + PREFIX + "*", ds,
                "60", "30", Action.ActionType.FORCED, null, false);
        rolloutManagement.start(rollout.getId());

        queryCount.resetQueries();
        rolloutHandler.handleAll(); // first running tick - start rollout, run group 1, create its actions
        dump("rollout first running tick - starts group, creates actions");
        assertSelectsByTable("rollout first running tick", ROLLOUT_FIRST_TICK);

        queryCount.resetQueries();
        rolloutHandler.handleAll(); // steady running tick - nothing to advance without feedback
        dump("rollout steady running tick - nothing to advance");
        assertSelectsByTable("rollout steady running tick", ROLLOUT_STEADY_TICK);
    }

    private void autoAssignScenario() {
        final DistributionSet ds = testdataFactory.createDistributionSet("aaguard");
        testdataFactory.createTargets(PREFIX, 0, TARGETS);
        autoAssignmentManagement.create(Create.builder().name("aaguard").targetFilterQuery("controllerid==" + PREFIX + "*").distributionSet(ds).build());
    }

    /**
     * Asserts the exact per-table SELECT footprint of a measured scheduler phase: the full {table -> select count} map,
     * so a regression that adds/removes a query names the exact table that changed (not just a moved total). On mismatch
     * AssertJ prints both maps; the preceding {@code dump()} shows every statement.
     */
    private void assertSelectsByTable(final String phase, final Map<String, Long> expected) {
        final Map<String, Long> byTable = selectsByTable();
        // completeness: every recorded SELECT is bucketed to a table, so nothing slips past the per-table guard
        assertThat(byTable.values().stream().mapToLong(Long::longValue).sum())
                .as("every SELECT bucketed by table on %s (sum == countSelect) - statements: %s", phase, queryCount.getAllStatements())
                .isEqualTo(queryCount.countSelect());
        assertThat(byTable)
                .as("SELECTs by table on %s", phase)
                .containsExactlyInAnyOrderEntriesOf(expected);
    }

    // histogram of recorded SELECTs by the table in their (outermost) FROM - kept test-local so QueryCount stays a pure recorder
    private Map<String, Long> selectsByTable() {
        return queryCount.getAllStatements().stream()
                .filter(sql -> sql.stripLeading().regionMatches(true, 0, "select", 0, "select".length()))
                .map(SqlProbeTest::primaryFromTable)
                .filter(table -> table != null && !table.isEmpty())
                .collect(Collectors.groupingBy(table -> table, TreeMap::new, Collectors.counting()));
    }

    // table in the outermost FROM of a SELECT; handles EclipseLink's "{oj <table> ... LEFT OUTER JOIN ...}" escape
    private static String primaryFromTable(final String sql) {
        final String lower = sql.toLowerCase(Locale.ROOT);
        final int from = lower.indexOf(" from ");
        if (from < 0) {
            return null;
        }
        int start = from + " from ".length();
        while (start < lower.length() && Character.isWhitespace(lower.charAt(start))) {
            start++;
        }
        if (lower.startsWith("{oj ", start)) {
            start += "{oj ".length();
            while (start < lower.length() && Character.isWhitespace(lower.charAt(start))) {
                start++;
            }
        }
        int end = start;
        while (end < lower.length() && (Character.isLetterOrDigit(lower.charAt(end)) || lower.charAt(end) == '_')) {
            end++;
        }
        return lower.substring(start, end);
    }

    private void dump(final String label) {
        final var statements = queryCount.getAllStatements();
        log.info("********************** SQLDUMP {} **********************", label);
        log.info("SQLDUMP | {} | ==== {} stmt, {} SELECT, byTable={} ====",
                label, statements.size(), queryCount.countSelect(), selectsByTable());
        for (int i = 0; i < statements.size(); i++) {
            log.info("SQLDUMP | {} | [{}] {}", label, i + 1, statements.get(i));
        }
        log.info("********************** END SQLDUMP {} **********************", label);
    }
}
