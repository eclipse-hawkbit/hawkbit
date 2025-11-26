/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.hawkbit.context.AccessContext.asSystem;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.specifications.ActionSpecifications;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionCancellationType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidationCount;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.query.Param;

/**
 * Test class testing the functionality of invalidating a
 * {@link DistributionSet}
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Distribution set invalidation management
 */
@Slf4j
class DistributionSetInvalidationManagementTest extends AbstractJpaIntegrationTest {

    /**
     * Verify invalidation of distribution sets that only removes distribution sets from auto assignments
     */
    @Test
    void verifyInvalidateDistributionSetStopAutoAssignment() {
        final InvalidationTestData invalidationTestData = createInvalidationTestData("verifyInvalidateDistributionSetStopAutoAssignment");

        final DistributionSetInvalidation distributionSetInvalidation = new DistributionSetInvalidation(
                Collections.singletonList(invalidationTestData.distributionSet().getId()), ActionCancellationType.NONE);
        final DistributionSetInvalidationCount distributionSetInvalidationCount = countEntitiesForInvalidation(distributionSetInvalidation);
        assertDistributionSetInvalidationCount(distributionSetInvalidationCount, 1, 0, 0);

        distributionSetInvalidationManagement.invalidateDistributionSet(distributionSetInvalidation);
        rolloutHandler.handleAll();

        assertThat(targetFilterQueryManagement.find(invalidationTestData.targetFilterQuery().getId()).orElseThrow()
                .getAutoAssignDistributionSet()).isNull();
        assertThat(rolloutRepository.findById(invalidationTestData.rollout().getId()).orElseThrow().getStatus())
                .isNotIn(RolloutStatus.STOPPING, RolloutStatus.FINISHED);
        for (final Target target : invalidationTestData.targets()) {
            // if status is pending, the assignment has not been canceled
            assertThat(targetRepository.findById(target.getId()).orElseThrow().getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);
            assertThat(findActionsByTarget(target)).hasSize(1);
            assertThat(findActionsByTarget(target).get(0).getStatus()).isEqualTo(Status.RUNNING);
        }
    }

    /**
     * Verify invalidation of distribution sets that removes distribution sets from auto assignments but does not stop rollouts
     */
    @Test
    void verifyInvalidateDistributionSetDoesNotStopRollouts() {
        final InvalidationTestData invalidationTestData = createInvalidationTestData("verifyInvalidateDistributionSetStopRollouts");

        final DistributionSetInvalidation distributionSetInvalidation = new DistributionSetInvalidation(
                Collections.singletonList(invalidationTestData.distributionSet().getId()), ActionCancellationType.NONE);
        final DistributionSetInvalidationCount distributionSetInvalidationCount = countEntitiesForInvalidation(distributionSetInvalidation);
        assertDistributionSetInvalidationCount(distributionSetInvalidationCount, 1, 0, 0);

        distributionSetInvalidationManagement.invalidateDistributionSet(distributionSetInvalidation);
        rolloutHandler.handleAll();

        assertThat(targetFilterQueryManagement.find(invalidationTestData.targetFilterQuery().getId()).orElseThrow()
                .getAutoAssignDistributionSet()).isNull();
        assertThat(rolloutRepository.findById(invalidationTestData.rollout().getId()).orElseThrow().getStatus())
                .isEqualTo(RolloutStatus.READY);

        assertNoScheduledActionsExist(invalidationTestData.rollout());
        for (final Target target : invalidationTestData.targets()) {
            // if status is pending, the assignment has not been canceled
            assertThat(targetRepository.findById(invalidationTestData.targets().get(0).getId()).orElseThrow().getUpdateStatus())
                    .isEqualTo(TargetUpdateStatus.PENDING);
            assertThat(findActionsByTarget(target)).hasSize(1);
            assertThat(findActionsByTarget(target).get(0).getStatus()).isEqualTo(Status.RUNNING);
        }
    }

    /**
     * Verify invalidation of distribution sets that removes distribution sets from auto assignments, stops rollouts and force cancels assignments
     */
    @Test
    void verifyInvalidateDistributionSetStopAllAndForceCancel() {
        final InvalidationTestData invalidationTestData = createInvalidationTestData("verifyInvalidateDistributionSetStopAllAndForceCancel");

        final DistributionSetInvalidation distributionSetInvalidation = new DistributionSetInvalidation(
                Collections.singletonList(invalidationTestData.distributionSet().getId()), ActionCancellationType.FORCE);
        final DistributionSetInvalidationCount distributionSetInvalidationCount = countEntitiesForInvalidation(distributionSetInvalidation);
        assertDistributionSetInvalidationCount(distributionSetInvalidationCount, 1, 5, 1);

        distributionSetInvalidationManagement.invalidateDistributionSet(distributionSetInvalidation);
        rolloutHandler.handleAll();

        assertThat(targetFilterQueryManagement.find(invalidationTestData.targetFilterQuery().getId()).orElseThrow()
                .getAutoAssignDistributionSet()).isNull();
        // rollout should be deleted when force invalidation
        assertThat(rolloutRepository.findById(invalidationTestData.rollout().getId())).isEmpty();
        assertNoScheduledActionsExist(invalidationTestData.rollout());
        assertAllRolloutActionsAreCancelled(invalidationTestData.rollout());
    }

    /**
     * Verify invalidation of distribution sets that removes distribution sets from auto assignments, stops rollouts and cancels assignments
     */
    @Test
    void verifyInvalidateDistributionSetStopAll() {
        final InvalidationTestData invalidationTestData = createInvalidationTestData("verifyInvalidateDistributionSetStopAll");

        final DistributionSetInvalidation distributionSetInvalidation = new DistributionSetInvalidation(
                Collections.singletonList(invalidationTestData.distributionSet().getId()), ActionCancellationType.SOFT);
        final DistributionSetInvalidationCount distributionSetInvalidationCount = countEntitiesForInvalidation(distributionSetInvalidation);
        assertDistributionSetInvalidationCount(distributionSetInvalidationCount, 1, 5, 1);

        distributionSetInvalidationManagement.invalidateDistributionSet(distributionSetInvalidation);

        assertThat(targetFilterQueryManagement.find(invalidationTestData.targetFilterQuery().getId()).orElseThrow()
                .getAutoAssignDistributionSet()).isNull();
        assertThat(rolloutRepository.findById(invalidationTestData.rollout().getId()).orElseThrow().getStatus())
                .isIn(RolloutStatus.STOPPING, RolloutStatus.FINISHED);
        for (final Target target : invalidationTestData.targets()) {
            assertThat(targetRepository.findById(target.getId()).orElseThrow().getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);
            assertThat(findActionsByTarget(target)).hasSize(1);
            assertThat(findActionsByTarget(target).get(0).getStatus()).isEqualTo(Status.CANCELING);
        }
    }

    /**
     * Verify that invalidating an incomplete distribution set throws an exception
     */
    @Test
    void verifyInvalidateIncompleteDistributionSetThrowsException() {
        final DistributionSet distributionSet = testdataFactory.createIncompleteDistributionSet();

        final DistributionSetInvalidation distributionSetInvalidation = new DistributionSetInvalidation(
                List.of(distributionSet.getId()), ActionCancellationType.SOFT);
        assertThatExceptionOfType(IncompleteDistributionSetException.class)
                .as("Incomplete distributionSet should throw an exception")
                .isThrownBy(() -> distributionSetInvalidationManagement.invalidateDistributionSet(distributionSetInvalidation));
    }

    /**
     * Verify that invalidating an invalidated distribution set don't throws an exception
     * -> should be able to cancel actions again (if previous time there was a problem
     */
    @Test
    @SuppressWarnings("java:S2699")
    // test that no exception is thrown
    void verifyInvalidateInvalidatedDistributionSetDontThrowsException() {
        final DistributionSet distributionSet = testdataFactory.createAndInvalidateDistributionSet();
        distributionSetInvalidationManagement.invalidateDistributionSet(
                new DistributionSetInvalidation(Collections.singletonList(distributionSet.getId()), ActionCancellationType.SOFT));
    }

    /**
     * Verify that a user that has authority READ_DISTRIBUTION_SET and UPDATE_DISTRIBUTION_SET is allowed to invalidate a distribution set
     */
    @Test
    @WithUser(authorities = { "READ_DISTRIBUTION_SET", "UPDATE_DISTRIBUTION_SET" })
    void verifyInvalidateWithReadAndUpdateRepoAuthority() {
        final InvalidationTestData invalidationTestData = asSystem(() -> createInvalidationTestData("verifyInvalidateWithUpdateRepoAuthority"));
        distributionSetInvalidationManagement.invalidateDistributionSet(new DistributionSetInvalidation(
                List.of(invalidationTestData.distributionSet().getId()), ActionCancellationType.NONE));
        assertThat(distributionSetRepository.findById(invalidationTestData.distributionSet().getId()).orElseThrow().isValid()).isFalse();
    }

    /**
     * Verify that a user that has authority READ_DISTRIBUTION_SET, UPDATE_DISTRIBUTION_SET and UPDATE_TARGET is allowed to invalidate a distribution set only without canceling rollouts
     */
    @Test
    @WithUser(authorities = { "READ_DISTRIBUTION_SET", "UPDATE_DISTRIBUTION_SET", "UPDATE_TARGET" })
    void verifyInvalidateWithReadAndUpdateRepoAndUpdateTargetAuthority() {
        final InvalidationTestData invalidationTestData = asSystem(
                () -> createInvalidationTestData("verifyInvalidateWithUpdateRepoAndUpdateTargetAuthority"));

        final DistributionSetInvalidation distributionSetInvalidation = new DistributionSetInvalidation(
                List.of(invalidationTestData.distributionSet().getId()), ActionCancellationType.SOFT);
        assertThatExceptionOfType(InsufficientPermissionException.class)
                .as("Insufficient permission exception expected")
                .isThrownBy(() -> distributionSetInvalidationManagement.invalidateDistributionSet(distributionSetInvalidation));

        distributionSetInvalidationManagement.invalidateDistributionSet(new DistributionSetInvalidation(
                Collections.singletonList(invalidationTestData.distributionSet().getId()), ActionCancellationType.NONE));
        assertThat(distributionSetRepository.findById(invalidationTestData.distributionSet().getId()).orElseThrow().isValid()).isFalse();
    }

    /**
     * Verify that a user that has authority READ_DISTRIBUTION_SET, UPDATE_DISTRIBUTION_SET, UPDATE_ROLLOUT and UPDATE_TARGET is allowed to invalidate a distribution
     */
    @Test
    @WithUser(authorities = { "READ_DISTRIBUTION_SET", "UPDATE_DISTRIBUTION_SET", "UPDATE_TARGET", "UPDATE_ROLLOUT" })
    void verifyInvalidateWithReadAndUpdateRepoAndUpdateTargetAndUpdateRolloutAuthority() {
        final InvalidationTestData invalidationTestData = asSystem(
                () -> createInvalidationTestData("verifyInvalidateWithUpdateRepoAndUpdateTargetAuthority"));

        distributionSetInvalidationManagement.invalidateDistributionSet(new DistributionSetInvalidation(
                List.of(invalidationTestData.distributionSet().getId()), ActionCancellationType.SOFT));
        assertThat(distributionSetRepository.findById(invalidationTestData.distributionSet().getId()).orElseThrow().isValid()).isFalse();
    }

    private void assertNoScheduledActionsExist(final Rollout rollout) {
        assertThat(actionRepository.findByRolloutIdAndStatus(PAGE, rollout.getId(), Status.SCHEDULED).getTotalElements()).isZero();
    }

    private void assertAllRolloutActionsAreCancelled(final Rollout rollout) {
        assertThat(actionRepository.findByRolloutIdAndStatus(PAGE, rollout.getId(), Status.CANCELED).getTotalElements()).isZero();
    }

    private InvalidationTestData createInvalidationTestData(final String testName) {
        DistributionSet distributionSet = testdataFactory.createDistributionSet();
        final List<Target> targets = testdataFactory.createTargets(5, testName);
        // if implicitly locked - the old distribution set becomes stale
        distributionSet = assignDistributionSet(distributionSet, targets).getDistributionSet();
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement.create(TargetFilterQueryManagement.Create.builder()
                .name(testName).query("name==*").autoAssignDistributionSet(distributionSet).build());
        final Rollout rollout = testdataFactory.createRolloutByVariables(testName, "desc", 2, "name==*", distributionSet, "50", "80");

        return new InvalidationTestData(distributionSet, targets, targetFilterQuery, rollout);
    }

    private void assertDistributionSetInvalidationCount(
            final DistributionSetInvalidationCount distributionSetInvalidationCount,
            final long expectedAutoAssignmentCount, final long expectedActionCount, final long expectedRolloutCount) {
        assertThat(distributionSetInvalidationCount.autoAssignmentCount()).isEqualTo(expectedAutoAssignmentCount);
        assertThat(distributionSetInvalidationCount.actionCount()).isEqualTo(expectedActionCount);
        assertThat(distributionSetInvalidationCount.rolloutsCount()).isEqualTo(expectedRolloutCount);
    }

    private List<JpaAction> findActionsByTarget(@Param("target") Target target) { // order by id ?
        return actionRepository.findAll(ActionSpecifications.byTargetControllerId(target.getControllerId()));
    }

    private DistributionSetInvalidationCount countEntitiesForInvalidation(final DistributionSetInvalidation distributionSetInvalidation) {
        return asSystem(() -> {
            final Collection<Long> setIds = distributionSetInvalidation.getDistributionSetIds();
            final long rolloutsCount = distributionSetInvalidation.getActionCancellationType() != ActionCancellationType.NONE
                    ? countRolloutsForInvalidation(setIds)
                    : 0;
            final long autoAssignmentsCount = countAutoAssignmentsForInvalidation(setIds);
            final long actionsCount = countActionsForInvalidation(setIds, distributionSetInvalidation.getActionCancellationType());

            return new DistributionSetInvalidationCount(rolloutsCount, autoAssignmentsCount, actionsCount);
        });
    }

    private long countRolloutsForInvalidation(final Collection<Long> setIds) {
        return setIds.stream().mapToLong(rolloutManagement::countByDistributionSetIdAndRolloutIsStoppable).sum();
    }

    private long countAutoAssignmentsForInvalidation(final Collection<Long> setIds) {
        return setIds.stream().mapToLong(targetFilterQueryManagement::countByAutoAssignDistributionSetId).sum();
    }

    private long countActionsForInvalidation(final Collection<Long> setIds, final ActionCancellationType cancelationType) {
        long affectedActionsByDSInvalidation = 0;
        if (cancelationType == ActionCancellationType.FORCE) {
            affectedActionsByDSInvalidation = countActionsForForcedInvalidation(setIds);
        } else if (cancelationType == ActionCancellationType.SOFT) {
            affectedActionsByDSInvalidation = countActionsForSoftInvalidation(setIds);
        }
        return affectedActionsByDSInvalidation;
    }

    private long countActionsForForcedInvalidation(final Collection<Long> setIds) {
        return setIds.stream().mapToLong(actionRepository::countByDistributionSetIdAndActiveIsTrue).sum();
    }

    private long countActionsForSoftInvalidation(final Collection<Long> setIds) {
        return setIds.stream()
                .mapToLong(distributionSet -> actionRepository
                        .countByDistributionSetIdAndActiveIsTrueAndStatusIsNot(distributionSet, Status.CANCELING))
                .sum();
    }

    private record InvalidationTestData(
            DistributionSet distributionSet, List<Target> targets, TargetFilterQuery targetFilterQuery, Rollout rollout) {}
}