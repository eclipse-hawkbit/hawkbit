/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Collections;

import org.awaitility.Awaitility;
import org.eclipse.hawkbit.repository.exception.StopRolloutException;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation.CancelationType;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test class testing the invalidation of a {@link DistributionSet} while the
 * handle rollouts is ongoing.
 *
 */
@Feature("Component Tests - Repository")
@Story("Concurrent Distribution Set invalidation")
@TestPropertySource(properties = "hawkbit.server.repository.dsInvalidationLockTimeout=1")
public class ConcurrentDistributionSetInvalidationTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Verify that a large rollout causes a timeout when trying to invalidate a distribution set")
    public void verifyInvalidateDistributionSetWithLargeRolloutThrowsException() throws Exception {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        testdataFactory.createTargets(10000, "verifyInvalidateDistributionSetWithLargeRolloutThrowsException");
        final RolloutGroupConditions conditions = new RolloutGroupConditionBuilder().withDefaults()
                .successCondition(RolloutGroupSuccessCondition.THRESHOLD, "50")
                .errorCondition(RolloutGroupErrorCondition.THRESHOLD, "80")
                .errorAction(RolloutGroupErrorAction.PAUSE, null).build();
        final Rollout rollout = rolloutManagement.create(entityFactory.rollout().create()
                .name("verifyInvalidateDistributionSetWithLargeRolloutThrowsException").description("desc")
                .targetFilterQuery("name==*").set(distributionSet).actionType(ActionType.FORCED), 20, conditions);
        final String tenant = tenantAware.getCurrentTenant();

        // run in new Thread so that the invalidation can be executed in
        // parallel
        final Thread handleRolloutsThread = new Thread(() -> {
            tenantAware.runAsTenant(tenant, () -> systemSecurityContext.runAsSystem(() -> {
                rolloutManagement.handleRollouts();
                return 0;
            }));
        });
        handleRolloutsThread.start();
        // wait until at least one RolloutGroup is created, as this means that
        // the thread has started and has acquired the lock
        Awaitility.await().until(() -> tenantAware.runAsTenant(tenant, () -> systemSecurityContext
                .runAsSystem(() -> rolloutGroupManagement.findByRollout(PAGE, rollout.getId()).getSize() > 0)));

        assertThatExceptionOfType(StopRolloutException.class)
                .as("Invalidation of distributionSet should throw an exception")
                .isThrownBy(() -> distributionSetInvalidationManagement.invalidateDistributionSet(
                        new DistributionSetInvalidation(Collections.singletonList(distributionSet.getId()),
                                CancelationType.SOFT, true)));
    }

}
