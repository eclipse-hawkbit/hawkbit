/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.eclipse.hawkbit.repository.exception.StopRolloutException;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Test class testing the invalidation of a {@link DistributionSet} while the
 * handle rollouts is ongoing.
 *
 */
@Feature("Component Tests - Repository")
@Story("Concurrent Distribution Set invalidation")
@ContextConfiguration(classes = ConcurrentDistributionSetInvalidationTest.Config.class)
@TestPropertySource(properties = { "hawkbit.server.repository.dsInvalidationLockTimeout=1" })
public class ConcurrentDistributionSetInvalidationTest extends AbstractJpaIntegrationTest {
    
    @Configuration
    static class Config {

        /**
         * Creates a {@link RolloutGroupRepository} bean that is slow during saving already created Groups.
         * This gives this test more time to succeed
         */
        @Bean
        @Primary
        RolloutGroupRepository slowRolloutGroupRepository(final RolloutGroupRepository groupRepo) {
            final RolloutGroupRepository slowGroupRepo = mock(RolloutGroupRepository.class, delegatesTo(groupRepo));

            doAnswer(invocation -> {
                final JpaRolloutGroup group = invocation.getArgument(0);
                if (group.getId() == null) {
                    return groupRepo.save(group);
                }
                TimeUnit.SECONDS.sleep(2);
                return groupRepo.save(group);
            }).when(slowGroupRepo).save(any());

            return slowGroupRepo;
        }
    }

    @Test
    @Description("Verify that a large rollout causes a timeout when trying to invalidate a distribution set")
    public void verifyInvalidateDistributionSetWithLargeRolloutThrowsException() throws Exception {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        final Rollout rollout = createRollout(distributionSet);
        final String tenant = tenantAware.getCurrentTenant();

        // run in new Thread so that the invalidation can be executed in
        // parallel
        new Thread(() -> systemSecurityContext.runAsSystemAsTenant(() -> {
            rolloutManagement.handleRollouts();
            return 0;
        }, tenant)).start();

        // wait until at least one RolloutGroup is created, as this means that
        // the thread has started and has acquired the lock
        Awaitility.await().atMost(Duration.FIVE_SECONDS)
                .pollInterval(Duration.ONE_HUNDRED_MILLISECONDS)
                .until(() -> tenantAware.runAsTenant(tenant, () -> systemSecurityContext
                .runAsSystem(() -> rolloutGroupManagement.findByRollout(PAGE, rollout.getId()).getSize() > 0)));

        assertThatExceptionOfType(StopRolloutException.class)
                .as("Invalidation of distributionSet should throw an exception")
                .isThrownBy(() -> distributionSetInvalidationManagement.invalidateDistributionSet(
                        new DistributionSetInvalidation(Collections.singletonList(distributionSet.getId()),
                                CancelationType.SOFT, true)));
    }

    private Rollout createRollout(final DistributionSet distributionSet) {
        testdataFactory.createTargets(
                quotaManagement.getMaxTargetsPerRolloutGroup() * quotaManagement.getMaxRolloutGroupsPerRollout(),
                "verifyInvalidateDistributionSetWithLargeRolloutThrowsException");
        final RolloutGroupConditions conditions = new RolloutGroupConditionBuilder().withDefaults()
                .successCondition(RolloutGroupSuccessCondition.THRESHOLD, "50")
                .errorCondition(RolloutGroupErrorCondition.THRESHOLD, "80")
                .errorAction(RolloutGroupErrorAction.PAUSE, null).build();

        return rolloutManagement.create(entityFactory.rollout().create()
                .name("verifyInvalidateDistributionSetWithLargeRolloutThrowsException").description("desc")
                        .targetFilterQuery("name==*").set(distributionSet).actionType(ActionType.FORCED),
                quotaManagement.getMaxRolloutGroupsPerRollout(), false, conditions);
    }

}
