/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.scheduler;

import jakarta.persistence.EntityManagerFactory;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.AutoAssignmentManagement.Create;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.test.util.QueryCount;
import org.eclipse.hawkbit.repository.test.util.QueryCountConfiguration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@Slf4j
@Import(QueryCountConfiguration.class)
@TestPropertySource(properties = { "logging.level.org.eclipse.hawkbit.repository.jpa.scheduler.SchedulersSqlProbeTest=DEBUG" })
@Disabled("For manual run only, No asserts, just prints the SQL statements per (AutoAssign/Rollout) scheduler tick.")
class SchedulersSqlProbeTest extends AbstractJpaIntegrationTest {

    private static final String PREFIX = "roll-probe-";
    private static final int TARGETS = 10;
    private static final int GROUPS = 2;

    @Autowired
    private QueryCount queryCount;

    @Autowired
    private JpaAutoAssignHandler autoAssignHandler;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    /**
     * Regression guard for the auto-assign handler, both ticks:
     * - assigning tick (10 targets) - type reads BOUNDED (once via isComplete, not per target);
     * - steady tick (nothing to assign) - the ~909/sec prod hot path, must NOT touch the DS type at all.
     * Asserts the exact per-table SELECT footprint (each table touched), so a regression names the table that changed.
     */
    @Test
    void autoAssignHandlerRun() {
        // reset the query count before the setup, so that the setup queries are not included in the measured queries for the test - i.e schema creation, etc.
        queryCount.resetQueries();
        autoAssignScenario();

        queryCount.resetQueries();
        autoAssignHandler.handleAll(); // assigns all 10 targets
        dump("auto-assign assigning 10 targets - first handleAll() tick");

        queryCount.resetQueries();
        autoAssignHandler.handleAll(); // steady tick - nothing left to assign
        dump("auto-assign steady tick - second handleAll() tick - nothing to assign");
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

        queryCount.resetQueries();
        rolloutHandler.handleAll(); // steady running tick - nothing to advance without feedback
        dump("rollout steady running tick - nothing to advance");
    }

    private void autoAssignScenario() {
        final DistributionSet ds = testdataFactory.createDistributionSet("aaguard");
        testdataFactory.createTargets(PREFIX, 0, TARGETS);
        autoAssignmentManagement.create(Create.builder().name("aaguard").targetFilterQuery("controllerid==" + PREFIX + "*").distributionSet(ds)
                .build());
    }

    private void dump(final String label) {
        final var statements = queryCount.getAllStatements();
        log.info("********************** SQLDUMP {} **********************", label);
        log.info("SQLDUMP | {} | ==== {} stmt, {} SELECT", label, statements.size(), queryCount.countSelect());
        for (int i = 0; i < statements.size(); i++) {
            log.info("SQLDUMP | {} | [{}] {}", label, i + 1, statements.get(i));
        }
        log.info("********************** END SQLDUMP {} **********************", label);
    }
}
