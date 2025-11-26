/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.hawkbit.context.AccessContext.asSystemAsTenant;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.repository.RolloutManagement.Create;
import org.eclipse.hawkbit.repository.exception.StopRolloutException;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.ActionCancellationType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation;
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

/**
 * Test class testing the invalidation of a {@link DistributionSet} while the
 * handle rollouts is ongoing.
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Concurrent Distribution Set invalidation
 */
@ContextConfiguration(classes = ConcurrentDistributionSetInvalidationTest.Config.class)
@TestPropertySource(properties = { "hawkbit.server.repository.dsInvalidationLockTimeout=1" })
class ConcurrentDistributionSetInvalidationTest extends AbstractJpaIntegrationTest {

    /**
     * Verify that a large rollout causes a timeout when trying to invalidate a distribution set
     */
    @Test
    void verifyInvalidateDistributionSetWithLargeRolloutThrowsException() {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        final Rollout rollout = createRollout(distributionSet);
        final String tenant = AccessContext.tenant();

        // run in new Thread so that the invalidation can be executed in
        // parallel
        new Thread(() -> asSystemAsTenant(tenant, rolloutHandler::handleAll)).start();

        // wait until at least one RolloutGroup is created, as this means that the thread has started and has acquired the lock
        Awaitility.await()
                .pollInterval(Duration.ofMillis(100))
                .atMost(Duration.ofSeconds(5))
                .until(() -> asSystemAsTenant(tenant, () -> rolloutGroupManagement.findByRollout(rollout.getId(), PAGE).getSize() > 0));

        final DistributionSetInvalidation distributionSetInvalidation = new DistributionSetInvalidation(
                Collections.singletonList(distributionSet.getId()), ActionCancellationType.SOFT);
        assertThatExceptionOfType(StopRolloutException.class)
                .as("Invalidation of distributionSet should throw an exception")
                .isThrownBy(() -> distributionSetInvalidationManagement.invalidateDistributionSet(distributionSetInvalidation));
    }

    private Rollout createRollout(final DistributionSet distributionSet) {
        testdataFactory.createTargets(
                quotaManagement.getMaxTargetsPerRolloutGroup() * quotaManagement.getMaxRolloutGroupsPerRollout(),
                "verifyInvalidateDistributionSetWithLargeRolloutThrowsException");
        final RolloutGroupConditions conditions = new RolloutGroupConditionBuilder().withDefaults()
                .successCondition(RolloutGroupSuccessCondition.THRESHOLD, "50")
                .errorCondition(RolloutGroupErrorCondition.THRESHOLD, "80")
                .errorAction(RolloutGroupErrorAction.PAUSE, null).build();

        return rolloutManagement.create(Create.builder()
                        .name("verifyInvalidateDistributionSetWithLargeRolloutThrowsException").description("desc")
                        .targetFilterQuery("name==*").distributionSet(distributionSet).actionType(ActionType.FORCED)
                        .build(),
                quotaManagement.getMaxRolloutGroupsPerRollout(), false, conditions);
    }

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
}