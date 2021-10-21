/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Collections;
import java.util.List;

import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.exception.InvalidDistributionSetException;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation.CancelationType;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidationCount;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test class testing the functionality of invalidating a
 * {@link DistributionSet}
 *
 */
@Feature("Component Tests - Repository")
@Story("Distribution set invalidation management")
class DistributionSetInvalidationManagementTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Verify invalidation of distribution sets that only removes distribution sets from auto assignments")
    void verifyInvalidateDistributionSetStopAutoAssignment() {
        final InvalidationTestData invalidationTestData = createInvalidationTestData(
                "verifyInvalidateDistributionSetStopAutoAssignment");

        final DistributionSetInvalidation distributionSetInvalidation = new DistributionSetInvalidation(
                Collections.singletonList(invalidationTestData.getDistributionSet().getId()), CancelationType.NONE,
                false);
        final DistributionSetInvalidationCount distributionSetInvalidationCount = distributionSetInvalidationManagement
                .countEntitiesForInvalidation(distributionSetInvalidation);
        assertDistributionSetInvalidationCount(distributionSetInvalidationCount, 1, 0, 0);

        distributionSetInvalidationManagement.invalidateDistributionSet(distributionSetInvalidation);
        rolloutManagement.handleRollouts();

        assertThat(targetFilterQueryManagement.get(invalidationTestData.getTargetFilterQuery().getId()).get()
                .getAutoAssignDistributionSet()).isNull();
        assertThat(rolloutRepository.findById(invalidationTestData.getRollout().getId()).get().getStatus())
                .isNotIn(RolloutStatus.STOPPING, RolloutStatus.FINISHED);
        for (final Target target : invalidationTestData.getTargets()) {
            // if status is pending, the assignment has not been canceled
            assertThat(targetRepository.findById(target.getId()).get().getUpdateStatus())
                    .isEqualTo(TargetUpdateStatus.PENDING);
            assertThat(actionRepository.findByTarget(target).size()).isEqualTo(1);
            assertThat(actionRepository.findByTarget(target).get(0).getStatus()).isEqualTo(Status.RUNNING);
        }
    }

    @Test
    @Description("Verify invalidation of distribution sets that removes distribution sets from auto assignments and stops rollouts")
    void verifyInvalidateDistributionSetStopRollouts() {
        final InvalidationTestData invalidationTestData = createInvalidationTestData(
                "verifyInvalidateDistributionSetStopRollouts");

        final DistributionSetInvalidation distributionSetInvalidation = new DistributionSetInvalidation(
                Collections.singletonList(invalidationTestData.getDistributionSet().getId()), CancelationType.NONE,
                true);
        final DistributionSetInvalidationCount distributionSetInvalidationCount = distributionSetInvalidationManagement
                .countEntitiesForInvalidation(distributionSetInvalidation);
        assertDistributionSetInvalidationCount(distributionSetInvalidationCount, 1, 0, 1);

        distributionSetInvalidationManagement.invalidateDistributionSet(distributionSetInvalidation);
        rolloutManagement.handleRollouts();

        assertThat(targetFilterQueryManagement.get(invalidationTestData.getTargetFilterQuery().getId()).get()
                .getAutoAssignDistributionSet()).isNull();
        assertThat(rolloutRepository.findById(invalidationTestData.getRollout().getId()).get().getStatus())
                .isEqualTo(RolloutStatus.FINISHED);
        assertNoScheduledActionsExist(invalidationTestData.getRollout());
        assertRolloutGroupsAreFinished(invalidationTestData.getRollout());
        for (final Target target : invalidationTestData.getTargets()) {
            // if status is pending, the assignment has not been canceled
            assertThat(
                    targetRepository.findById(invalidationTestData.getTargets().get(0).getId()).get().getUpdateStatus())
                            .isEqualTo(TargetUpdateStatus.PENDING);
            assertThat(actionRepository.findByTarget(target).size()).isEqualTo(1);
            assertThat(actionRepository.findByTarget(target).get(0).getStatus()).isEqualTo(Status.RUNNING);
        }
    }

    @Test
    @Description("Verify invalidation of distribution sets that removes distribution sets from auto assignments, stops rollouts and force cancels assignments")
    void verifyInvalidateDistributionSetStopAllAndForceCancel() {
        final InvalidationTestData invalidationTestData = createInvalidationTestData(
                "verifyInvalidateDistributionSetStopAllAndForceCancel");

        final DistributionSetInvalidation distributionSetInvalidation = new DistributionSetInvalidation(
                Collections.singletonList(invalidationTestData.getDistributionSet().getId()), CancelationType.FORCE,
                true);
        final DistributionSetInvalidationCount distributionSetInvalidationCount = distributionSetInvalidationManagement
                .countEntitiesForInvalidation(distributionSetInvalidation);
        assertDistributionSetInvalidationCount(distributionSetInvalidationCount, 1, 5, 1);

        distributionSetInvalidationManagement.invalidateDistributionSet(distributionSetInvalidation);
        rolloutManagement.handleRollouts();

        assertThat(targetFilterQueryManagement.get(invalidationTestData.getTargetFilterQuery().getId()).get()
                .getAutoAssignDistributionSet()).isNull();
        assertThat(rolloutRepository.findById(invalidationTestData.getRollout().getId()).get().getStatus())
                .isEqualTo(RolloutStatus.FINISHED);
        assertNoScheduledActionsExist(invalidationTestData.getRollout());
        assertRolloutGroupsAreFinished(invalidationTestData.getRollout());
        for (final Target target : invalidationTestData.getTargets()) {
            assertThat(targetRepository.findById(target.getId()).get().getUpdateStatus())
                    .isEqualTo(TargetUpdateStatus.IN_SYNC);
            assertThat(actionRepository.findByTarget(target).size()).isEqualTo(1);
            assertThat(actionRepository.findByTarget(target).get(0).getStatus()).isEqualTo(Status.CANCELED);
        }
    }

    private void assertNoScheduledActionsExist(final Rollout rollout) {
        assertThat(
                actionRepository.findByRolloutIdAndStatus(PAGE, rollout.getId(), Status.SCHEDULED).getTotalElements())
                        .isZero();
    }

    private void assertRolloutGroupsAreFinished(final Rollout rollout) {
        assertThat(rolloutGroupRepository.findByRolloutId(rollout.getId(), PAGE))
                .allMatch(rolloutGroup -> rolloutGroup.getStatus().equals(RolloutGroupStatus.FINISHED));
    }

    @Test
    @Description("Verify invalidation of distribution sets that removes distribution sets from auto assignments, stops rollouts and cancels assignments")
    void verifyInvalidateDistributionSetStopAll() {
        final InvalidationTestData invalidationTestData = createInvalidationTestData(
                "verifyInvalidateDistributionSetStopAll");

        final DistributionSetInvalidation distributionSetInvalidation = new DistributionSetInvalidation(
                Collections.singletonList(invalidationTestData.getDistributionSet().getId()), CancelationType.SOFT,
                true);
        final DistributionSetInvalidationCount distributionSetInvalidationCount = distributionSetInvalidationManagement
                .countEntitiesForInvalidation(distributionSetInvalidation);
        assertDistributionSetInvalidationCount(distributionSetInvalidationCount, 1, 5, 1);

        distributionSetInvalidationManagement.invalidateDistributionSet(distributionSetInvalidation);

        assertThat(targetFilterQueryManagement.get(invalidationTestData.getTargetFilterQuery().getId()).get()
                .getAutoAssignDistributionSet()).isNull();
        assertThat(rolloutRepository.findById(invalidationTestData.getRollout().getId()).get().getStatus())
                .isIn(RolloutStatus.STOPPING, RolloutStatus.FINISHED);
        for (final Target target : invalidationTestData.getTargets()) {
            assertThat(targetRepository.findById(target.getId()).get().getUpdateStatus())
                    .isEqualTo(TargetUpdateStatus.PENDING);
            assertThat(actionRepository.findByTarget(target).size()).isEqualTo(1);
            assertThat(actionRepository.findByTarget(target).get(0).getStatus()).isEqualTo(Status.CANCELING);
        }
    }

    @Test
    @Description("Verify that invalidating an incomplete distribution set throws an exception")
    void verifyInvalidateIncompleteDistributionSetThrowsException() {
        final DistributionSet distributionSet = testdataFactory.createIncompleteDistributionSet();

        assertThatExceptionOfType(IncompleteDistributionSetException.class)
                .as("Incomplete distributionSet should throw an exception")
                .isThrownBy(() -> distributionSetInvalidationManagement.invalidateDistributionSet(
                        new DistributionSetInvalidation(Collections.singletonList(distributionSet.getId()),
                                CancelationType.SOFT, true)));
    }

    @Test
    @Description("Verify that invalidating an invalidated distribution set throws an exception")
    void verifyInvalidateInvalidatedDistributionSetThrowsException() {
        final DistributionSet distributionSet = testdataFactory.createAndInvalidateDistributionSet();

        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Invalid distributionSet should throw an exception")
                .isThrownBy(() -> distributionSetInvalidationManagement.invalidateDistributionSet(
                        new DistributionSetInvalidation(Collections.singletonList(distributionSet.getId()),
                                CancelationType.SOFT, true)));
    }

    @Test
    @Description("Verify that a user that has authority READ_REPOSITORY and UPDATE_REPOSITORY is not allowed to invalidate a distribution set")
    @WithUser(authorities = { "READ_REPOSITORY", "UPDATE_REPOSITORY" })
    void verifyInvalidateWithReadAndUpdateRepoAuthority() {
        final InvalidationTestData invalidationTestData = systemSecurityContext
                .runAsSystem(() -> createInvalidationTestData("verifyInvalidateWithUpdateRepoAuthority"));

        assertThatExceptionOfType(InsufficientPermissionException.class)
                .as("Insufficient permission exception expected")
                .isThrownBy(() -> distributionSetInvalidationManagement
                        .invalidateDistributionSet(new DistributionSetInvalidation(
                                Collections.singletonList(invalidationTestData.getDistributionSet().getId()),
                                CancelationType.NONE, false)));
    }

    @Test
    @Description("Verify that a user that has authority READ_REPOSITORY, UPDATE_REPOSITORY and UPDATE_TARGET is allowed to invalidate a distribution set only without canceling rollouts")
    @WithUser(authorities = { "READ_REPOSITORY", "UPDATE_REPOSITORY", "UPDATE_TARGET" })
    void verifyInvalidateWithReadAndUpdateRepoAndUpdateTargetAuthority() {
        final InvalidationTestData invalidationTestData = systemSecurityContext.runAsSystem(
                () -> createInvalidationTestData("verifyInvalidateWithUpdateRepoAndUpdateTargetAuthority"));

        assertThatExceptionOfType(InsufficientPermissionException.class)
                .as("Insufficient permission exception expected")
                .isThrownBy(() -> distributionSetInvalidationManagement
                        .invalidateDistributionSet(new DistributionSetInvalidation(
                                Collections.singletonList(invalidationTestData.getDistributionSet().getId()),
                                CancelationType.SOFT, true)));

        distributionSetInvalidationManagement.invalidateDistributionSet(new DistributionSetInvalidation(
                Collections.singletonList(invalidationTestData.getDistributionSet().getId()), CancelationType.NONE,
                false));
        assertThat(
                distributionSetRepository.findById(invalidationTestData.getDistributionSet().getId()).get().isValid())
                        .isFalse();
    }

    @Test
    @Description("Verify that a user that has authority READ_REPOSITORY, UPDATE_REPOSITORY, UPDATE_ROLLOUT and UPDATE_TARGET is allowed to invalidate a distribution")
    @WithUser(authorities = { "READ_REPOSITORY", "UPDATE_REPOSITORY", "UPDATE_TARGET", "UPDATE_ROLLOUT" })
    void verifyInvalidateWithReadAndUpdateRepoAndUpdateTargetAndUpdateRolloutAuthority() {
        final InvalidationTestData invalidationTestData = systemSecurityContext.runAsSystem(
                () -> createInvalidationTestData("verifyInvalidateWithUpdateRepoAndUpdateTargetAuthority"));

        distributionSetInvalidationManagement.invalidateDistributionSet(new DistributionSetInvalidation(
                Collections.singletonList(invalidationTestData.getDistributionSet().getId()), CancelationType.SOFT,
                true));
        assertThat(
                distributionSetRepository.findById(invalidationTestData.getDistributionSet().getId()).get().isValid())
                        .isFalse();
    }

    private InvalidationTestData createInvalidationTestData(final String testName) {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        final List<Target> targets = testdataFactory.createTargets(5, testName);
        assignDistributionSet(distributionSet, targets);
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement.create(entityFactory.targetFilterQuery()
                .create().name(testName).query("name==*").autoAssignDistributionSet(distributionSet));
        final Rollout rollout = testdataFactory.createRolloutByVariables(testName, "desc", 2, "name==*",
                distributionSet, "50", "80");

        return new InvalidationTestData(distributionSet, targets, targetFilterQuery, rollout);
    }

    private static class InvalidationTestData {
        private final DistributionSet distributionSet;
        private final List<Target> targets;
        private final TargetFilterQuery targetFilterQuery;
        private final Rollout rollout;

        public InvalidationTestData(final DistributionSet distributionSet, final List<Target> targets,
                final TargetFilterQuery targetFilterQuery, final Rollout rollout) {
            super();
            this.distributionSet = distributionSet;
            this.targets = targets;
            this.targetFilterQuery = targetFilterQuery;
            this.rollout = rollout;
        }

        public DistributionSet getDistributionSet() {
            return distributionSet;
        }

        public List<Target> getTargets() {
            return targets;
        }

        public TargetFilterQuery getTargetFilterQuery() {
            return targetFilterQuery;
        }

        public Rollout getRollout() {
            return rollout;
        }
    }

    private void assertDistributionSetInvalidationCount(
            final DistributionSetInvalidationCount distributionSetInvalidationCount,
            final long expectedAutoAssignmentCount, final long expectedActionCount, final long expectedRolloutCount) {
        assertThat(distributionSetInvalidationCount.getAutoAssignmentCount()).isEqualTo(expectedAutoAssignmentCount);
        assertThat(distributionSetInvalidationCount.getActionCount()).isEqualTo(expectedActionCount);
        assertThat(distributionSetInvalidationCount.getRolloutsCount()).isEqualTo(expectedRolloutCount);
    }

}
