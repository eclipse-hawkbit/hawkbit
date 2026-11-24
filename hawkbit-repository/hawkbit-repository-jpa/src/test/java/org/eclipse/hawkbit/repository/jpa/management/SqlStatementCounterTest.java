/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.test.util.QueryCount;
import org.eclipse.hawkbit.repository.test.util.QueryCount.Kind;
import org.eclipse.hawkbit.repository.test.util.QueryCountConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Self-test of the JDBC statement counter ({@link QueryCount}) used by the rollout statement tests.
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: SQL statement counter
 */
@Import(QueryCountConfiguration.class)
class SqlStatementCounterTest extends AbstractJpaIntegrationTest {

    @Autowired
    private QueryCount queryCount;

    /**
     * One by-id read of a target is counted as exactly one SELECT on {@code sp_target}, and nothing is left after a reset.
     */
    @Test
    void countsOneSelectForFindByIdAndNothingAfterReset() {
        final long targetId = testdataFactory.createTarget("counter-self-test").getId();
        waitForEvents(); // async event processing may query the DB too

        queryCount.resetQueries();
        assertThat(targetRepository.findById(targetId)).isPresent();

        final QueryCount.Snapshot snapshot = queryCount.snapshot();
        assertThat(snapshot.count(Kind.SELECT, "sp_target")).isEqualTo(1);
        assertThat(snapshot.countExcluding(Kind.SELECT, "sp_tenant_configuration")).isEqualTo(1);
        assertThat(snapshot.count(Kind.UPDATE)).isZero();
        assertThat(snapshot.count(Kind.INSERT)).isZero();

        queryCount.resetQueries();
        final QueryCount.Snapshot afterReset = queryCount.snapshot();
        for (final Kind kind : Kind.values()) {
            assertThat(afterReset.count(kind)).as(kind.name()).isZero();
        }
    }
}
