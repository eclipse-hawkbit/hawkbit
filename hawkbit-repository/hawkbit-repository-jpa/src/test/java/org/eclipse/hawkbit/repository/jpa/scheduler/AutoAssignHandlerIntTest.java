/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement.AutoAssignDistributionSetUpdate;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement.Create;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

/**
 * Test class for {@link JpaAutoAssignHandler}.
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Auto assign checker
 */
@SuppressWarnings("java:S6813") // constructor injects are not possible for test classes
class AutoAssignHandlerIntTest extends AbstractJpaIntegrationTest {

    private static final String SPACE_AND_DESCRIPTION = " description";

    @Autowired
    private JpaAutoAssignHandler autoAssignChecker;
    @Autowired
    private DeploymentManagement deploymentManagement;

    /**
     * Verifies that a running action is auto canceled by a AutoAssignment which assigns another distribution-set.
     */
    @Test
    void autoAssignDistributionSetAndAutoCloseOldActions() {
        tenantConfigurationManagement().addOrUpdateConfiguration(TenantConfigurationKey.REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED, true);

        try {
            final String knownControllerId = "controller12345";
            final DistributionSet firstDistributionSet = testdataFactory.createDistributionSet();
            final DistributionSet secondDistributionSet = testdataFactory.createDistributionSet("second");
            testdataFactory.createTarget(knownControllerId);
            final DistributionSetAssignmentResult assignmentResult = assignDistributionSet(firstDistributionSet.getId(),
                    knownControllerId);
            final Long manuallyAssignedActionId = getFirstAssignedActionId(assignmentResult);

            // target filter query that matches all targets
            final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                    .create(Create.builder().name("filterA").query("name==*").build());
            targetFilterQueryManagement.updateAutoAssignDS(
                    new AutoAssignDistributionSetUpdate(targetFilterQuery.getId()).ds(secondDistributionSet.getId()));
            // Run the check
            autoAssignChecker.handleAll();

            // verify that manually created action is canceled and action
            // created from AutoAssign is running
            final List<Action> actionsByKnownTarget = deploymentManagement.findActionsByTarget(knownControllerId, PAGE)
                    .getContent();
            // should be 2 actions, one manually and one from the AutoAssign
            assertThat(actionsByKnownTarget).hasSize(2);
            // verify that manually assigned action is still running
            assertThat(deploymentManagement.findAction(manuallyAssignedActionId).get().getStatus())
                    .isEqualTo(Status.CANCELED);
            // verify that AutoAssign created action is running
            final Action rolloutCreatedAction = actionsByKnownTarget.stream()
                    .filter(action -> !action.getId().equals(manuallyAssignedActionId)).findAny().get();
            assertThat(rolloutCreatedAction.getStatus()).isEqualTo(Status.RUNNING);
            assertThat(rolloutCreatedAction.getActionType()).isEqualTo(ActionType.FORCED);
        } finally {
            tenantConfigurationManagement().addOrUpdateConfiguration(TenantConfigurationKey.REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED, false);
        }
    }

    /**
     * Test auto assignment of a DS to filtered targets
     */
    @Test
    void checkAutoAssign() {
        // will be auto assigned
        final DistributionSet setA = testdataFactory.createDistributionSet("dsA");
        final DistributionSet setB = testdataFactory.createDistributionSet("dsB");

        // target filter query that matches all targets
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement.updateAutoAssignDS(
                new AutoAssignDistributionSetUpdate(targetFilterQueryManagement
                        .create(Create.builder().name("filterA").query("name==*").build()).getId())
                        .ds(setA.getId()));
        implicitLock(setA);

        final String targetDsAIdPref = "targ";
        final List<Target> targets = testdataFactory.createTargets(25, targetDsAIdPref,
                targetDsAIdPref.concat(SPACE_AND_DESCRIPTION));
        final int targetsCount = targets.size();

        // assign set A to first 10 targets
        assignDistributionSet(setA, targets.subList(0, 10));
        // because of targetFilterQuery the assigned to targets group is a locked one
        // in the rest it is locked in process and targets gets unlocked (?)
        verifyThatTargetsHaveDistributionSetAssignment(setA, targets.subList(0, 10), targetsCount);

        // assign set B to first 5 targets
        // they have now 2 DS in their action history and should not get updated
        // with dsA
        assignDistributionSet(setB, targets.subList(0, 5));
        implicitLock(setB);
        verifyThatTargetsHaveDistributionSetAssignment(setB, targets.subList(0, 5), targetsCount);

        // assign set B to next 10 targets
        assignDistributionSet(setB, targets.subList(10, 20));
        verifyThatTargetsHaveDistributionSetAssignment(setB, targets.subList(10, 20), targetsCount);

        // Count the number of targets that will be assigned with setA
        assertThat(targetManagement.countByRsqlAndNonDsAndCompatibleAndUpdatable(setA.getId(), targetFilterQuery.getQuery())).isEqualTo(15);

        // Run the check
        autoAssignChecker.handleAll();

        verifyThatTargetsHaveDistributionSetAssignment(setA, targets.subList(5, 25), targetsCount);

        // first 5 should keep their dsB, because they already had the dsA once
        verifyThatTargetsHaveDistributionSetAssignment(setB, targets.subList(0, 5), targetsCount);

        verifyThatCreatedActionsAreInitiatedByCurrentUser(targetFilterQuery, setA, targets);
    }

    /**
     * Test auto assignment of a DS for a specific device
     */
    @Test
    void checkAutoAssignmentForDevice() {

        final DistributionSet toAssignDs = testdataFactory.createDistributionSet();

        // target filter query that matches all targets
        targetFilterQueryManagement.updateAutoAssignDS(
                new AutoAssignDistributionSetUpdate(targetFilterQueryManagement
                        .create(Create.builder().name("filterA").query("name==*").build()).getId())
                        .ds(toAssignDs.getId()));
        implicitLock(toAssignDs);

        final List<Target> targets = testdataFactory.createTargets(25);
        final int targetsCount = targets.size();

        // Run the check
        autoAssignChecker.handleSingleTarget(targets.get(0).getControllerId());

        verifyThatTargetsHaveDistributionSetAssignment(toAssignDs, targets.subList(0, 1), targetsCount);

        verifyThatTargetsNotHaveDistributionSetAssignment(targets.subList(1, 25));
    }

    /**
     * Test auto assignment of a DS to filtered targets with different confirmation options
     */
    @ParameterizedTest
    @MethodSource("confirmationOptions")
    void checkAutoAssignWithConfirmationOptions(final boolean confirmationFlowActive, final boolean confirmationRequired,
            final Action.Status expectedStatus) {

        final DistributionSet distributionSet = testdataFactory.createDistributionSet("dsA");

        if (confirmationFlowActive) {
            enableConfirmationFlow();
        }

        targetFilterQueryManagement.updateAutoAssignDS(
                new AutoAssignDistributionSetUpdate(targetFilterQueryManagement
                        .create(Create.builder().name("filterA").query("name==*").build()).getId())
                        .ds(distributionSet.getId()).confirmationRequired(confirmationRequired));

        final String targetDsAIdPref = "targ";
        final List<Target> targets = testdataFactory.createTargets(20, targetDsAIdPref,
                targetDsAIdPref.concat(SPACE_AND_DESCRIPTION));

        // Run the check
        autoAssignChecker.handleAll();

        verifyThatTargetsHaveDistributionSetAssignedAndActionStatus(distributionSet, targets, expectedStatus);
    }

    /**
     * Test auto assignment of a DS for a specific device with different confirmation options
     */
    @ParameterizedTest
    @MethodSource("confirmationOptions")
    void checkAutoAssignmentForDeviceWithConfirmationRequired(final boolean confirmationFlowActive,
            final boolean confirmationRequired, final Action.Status expectedStatus) {

        final DistributionSet toAssignDs = testdataFactory.createDistributionSet();

        if (confirmationFlowActive) {
            enableConfirmationFlow();
        }

        // target filter query that matches all targets
        targetFilterQueryManagement.updateAutoAssignDS(
                new AutoAssignDistributionSetUpdate(targetFilterQueryManagement
                        .create(Create.builder().name("filterA").query("name==*").build()).getId())
                        .ds(toAssignDs.getId()).confirmationRequired(confirmationRequired));

        final List<Target> targets = testdataFactory.createTargets(25);

        // Run the check
        autoAssignChecker.handleSingleTarget(targets.get(0).getControllerId());
        verifyThatTargetsHaveDistributionSetAssignedAndActionStatus(toAssignDs, targets.subList(0, 1), expectedStatus);

        verifyThatTargetsNotHaveDistributionSetAssignment(targets.subList(1, 25));
    }

    /**
     * Test auto assignment of an incomplete DS to filtered targets, that causes failures
     */
    @Test
    void checkAutoAssignWithFailures() {

        // incomplete distribution set that will be assigned
        final DistributionSet setF = distributionSetManagement.create(DistributionSetManagement.Create.builder()
                .type(testdataFactory.findOrCreateDefaultTestDsType())
                .name("dsA").version("1")
                .build());
        final DistributionSet setA = testdataFactory.createDistributionSet("dsA");
        final DistributionSet setB = testdataFactory.createDistributionSet("dsB");

        final String targetDsAIdPref = "targA";
        final String targetDsFIdPref = "targB";

        final Long filterId = targetFilterQueryManagement.create(
                        Create.builder().name("filterA").query("id==" + targetDsFIdPref + "*").build())
                .getId();
        final AutoAssignDistributionSetUpdate targetFilterQuery = new AutoAssignDistributionSetUpdate(filterId).ds(setF.getId());
        // target filter query that matches first bunch of targets, that should fail
        assertThatExceptionOfType(IncompleteDistributionSetException.class).isThrownBy(
                () -> targetFilterQueryManagement.updateAutoAssignDS(targetFilterQuery));
        // target filter query that matches failed bunch of targets
        targetFilterQueryManagement.create(Create.builder().name("filterB")
                .query("id==" + targetDsAIdPref + "*").autoAssignDistributionSet(setA).build());
        implicitLock(setA);

        final List<Target> targetsF = testdataFactory.createTargets(10, targetDsFIdPref,
                targetDsFIdPref.concat(SPACE_AND_DESCRIPTION));

        final List<Target> targetsA = testdataFactory.createTargets(10, targetDsAIdPref,
                targetDsAIdPref.concat(SPACE_AND_DESCRIPTION));

        final int targetsCount = targetsA.size() + targetsF.size();

        // assign set B to first 5 targets of fail group
        assignDistributionSet(setB, targetsF.subList(0, 5));
        implicitLock(setB);
        verifyThatTargetsHaveDistributionSetAssignment(setB, targetsF.subList(0, 5), targetsCount);

        // Run the check
        autoAssignChecker.handleAll();

        // first 5 targets of the fail group should still have setB
        verifyThatTargetsHaveDistributionSetAssignment(setB, targetsF.subList(0, 5), targetsCount);

        // all targets of A group should have received setA
        verifyThatTargetsHaveDistributionSetAssignment(setA, targetsA, targetsCount);

    }

    /**
     * Test auto assignment of a distribution set with FORCED, SOFT and DOWNLOAD_ONLY action types
     */
    @Test
    void checkAutoAssignWithDifferentActionTypes() {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        final String targetDsAIdPref = "A";
        final String targetDsBIdPref = "B";
        final String targetDsCIdPref = "C";

        final List<Target> targetsA = createTargetsAndAutoAssignDistSet(targetDsAIdPref, 5, distributionSet, ActionType.FORCED);
        implicitLock(distributionSet);
        final DistributionSet distributionSetLocked = distributionSetManagement.find(distributionSet.getId()).orElseThrow();
        final List<Target> targetsB = createTargetsAndAutoAssignDistSet(targetDsBIdPref, 10, distributionSetLocked, ActionType.SOFT);
        final List<Target> targetsC = createTargetsAndAutoAssignDistSet(targetDsCIdPref, 10, distributionSetLocked, ActionType.DOWNLOAD_ONLY);

        final int targetsCount = targetsA.size() + targetsB.size() + targetsC.size();

        autoAssignChecker.handleAll();

        verifyThatTargetsHaveDistributionSetAssignment(distributionSet, targetsA, targetsCount);
        verifyThatTargetsHaveDistributionSetAssignment(distributionSet, targetsB, targetsCount);
        verifyThatTargetsHaveDistributionSetAssignment(distributionSet, targetsC, targetsCount);

        verifyThatTargetsHaveAssignmentActionType(ActionType.FORCED, targetsA);
        verifyThatTargetsHaveAssignmentActionType(ActionType.SOFT, targetsB);
        verifyThatTargetsHaveAssignmentActionType(ActionType.DOWNLOAD_ONLY, targetsC);
    }

    /**
     * An auto assignment target filter with weight creates actions with weights
     */
    @Test
    void actionsWithWeightAreCreated() {
        final int amountOfTargets = 5;
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final int weight = 32;
        enableMultiAssignments();

        targetFilterQueryManagement.create(Create.builder()
                .name("a").query("name==*").autoAssignDistributionSet(ds).autoAssignWeight(weight)
                .build());
        testdataFactory.createTargets(amountOfTargets);
        autoAssignChecker.handleAll();

        final List<Action> actions = deploymentManagement.findActionsAll(PAGE).getContent();
        assertThat(actions)
                .hasSize(amountOfTargets)
                .allMatch(action -> action.getWeight().get() == weight);
    }

    /**
     * An auto assignment target filter without weight still works after multi assignment is enabled
     */
    @Test
    void filterWithoutWeightWorksInMultiAssignmentMode() {
        final int amountOfTargets = 5;
        final DistributionSet ds = testdataFactory.createDistributionSet();
        targetFilterQueryManagement.create(Create.builder().name("a").query("name==*").autoAssignDistributionSet(ds).build());
        enableMultiAssignments();

        testdataFactory.createTargets(amountOfTargets);
        autoAssignChecker.handleAll();

        final List<Action> actions = deploymentManagement.findActionsAll(PAGE).getContent();
        assertThat(actions)
                .hasSize(amountOfTargets)
                .allMatch(action -> action.getWeight().isPresent());
    }

    /**
     * Verifies an auto assignment only creates actions for compatible targets
     */
    @Test
    void checkAutoAssignmentWithIncompatibleTargets() {
        final int TARGET_COUNT = 5;

        final DistributionSet testDs = testdataFactory.createDistributionSet();
        final DistributionSetType incompatibleDsType1 = testdataFactory
                .findOrCreateDistributionSetType("incompatibleDsType1", "incompDsType1");
        final DistributionSetType incompatibleDsType2 = testdataFactory
                .findOrCreateDistributionSetType("incompatibleDsType2", "incompDsType2");
        final TargetFilterQuery testFilter = targetFilterQueryManagement.create(
                Create.builder().name("test-filter").query("name==*").autoAssignDistributionSet(testDs).build());

        final TargetType incompatibleEmptyType = testdataFactory.createTargetType("incompatibleEmptyType", Set.of());
        final TargetType incompatibleSingleType = testdataFactory.createTargetType(
                "incompatibleSingleType", Set.of(incompatibleDsType1));
        final TargetType incompatibleMultiType = testdataFactory.createTargetType(
                "incompatibleMultiType", Set.of(incompatibleDsType1, incompatibleDsType2));
        final TargetType compatibleSingleType = testdataFactory.createTargetType(
                "compatibleSingleType", Set.of(testDs.getType()));
        final TargetType compatibleMultiType = testdataFactory.createTargetType(
                "compatibleMultiType", Set.of(testDs.getType(), incompatibleDsType1));

        testdataFactory.createTargetsWithType(TARGET_COUNT, "incompatibleEmpty", incompatibleEmptyType);
        testdataFactory.createTargetsWithType(TARGET_COUNT, "incompatibleSingle", incompatibleSingleType);
        testdataFactory.createTargetsWithType(TARGET_COUNT, "incompatibleMulti", incompatibleMultiType);

        final List<Target> compatibleTargetsSingleType = testdataFactory.createTargetsWithType(TARGET_COUNT,
                "compatibleSingle", compatibleSingleType);
        final List<Target> compatibleTargetsMultiType = testdataFactory.createTargetsWithType(TARGET_COUNT,
                "compatibleMulti", compatibleMultiType);
        final List<Target> compatibleTargetsWithoutType = testdataFactory.createTargets(TARGET_COUNT,
                "compatibleSingleWithoutType");

        final List<Long> compatibleTargets = Stream
                .of(compatibleTargetsSingleType, compatibleTargetsMultiType, compatibleTargetsWithoutType)
                .flatMap(Collection::stream).map(Target::getId).toList();
        final long compatibleCount = targetManagement.countByRsqlAndNonDsAndCompatibleAndUpdatable(testDs.getId(),
                testFilter.getQuery());
        assertThat(compatibleCount).isEqualTo(compatibleTargets.size());

        autoAssignChecker.handleAll();

        final List<Action> actions = deploymentManagement.findActionsAll(Pageable.unpaged()).getContent();
        assertThat(actions).hasSize(compatibleTargets.size());
        final List<Long> actionTargets = actions.stream().map(a -> a.getTarget().getId()).toList();
        assertThat(actionTargets).containsExactlyInAnyOrderElementsOf(compatibleTargets);
    }

    private static Stream<Arguments> confirmationOptions() {
        return Stream.of( //
                Arguments.of(true, true, Status.WAIT_FOR_CONFIRMATION), //
                Arguments.of(true, false, Status.RUNNING), //
                Arguments.of(false, true, Status.RUNNING), //
                Arguments.of(false, false, Status.RUNNING));
    }

    private void verifyThatTargetsHaveDistributionSetAssignment(final DistributionSet set, final List<Target> targets, final int count) {
        final List<Long> targetIds = targets.stream().map(Target::getId).toList();

        final Page<? extends Target> targetsAll = targetManagement.findAll(PAGE);
        assertThat(targetsAll).as("Count of targets").hasSize(count);

        for (final Target target : targetsAll) {
            if (targetIds.contains(target.getId())) {
                assertThat(deploymentManagement.findAssignedDistributionSet(target.getControllerId())).as("assigned DS").contains(set);
            }
        }
    }

    private void verifyThatTargetsHaveDistributionSetAssignedAndActionStatus(
            final DistributionSet set, final List<Target> targets, final Action.Status status) {
        final List<String> targetIds = targets.stream().map(Target::getControllerId).toList();
        final List<Target> targetsWithAssignedDS = targetManagement.findByAssignedDistributionSet(set.getId(), PAGE).getContent();
        assertThat(targetsWithAssignedDS).isNotEmpty();
        assertThat(targetsWithAssignedDS).allMatch(target -> targetIds.contains(target.getControllerId()));

        final List<Action> actionsByDs = findActionsByDistributionSet(PAGE, set.getId()).getContent();
        assertThat(actionsByDs).hasSize(targets.size());
        assertThat(actionsByDs).allMatch(action -> action.getStatus() == status);
    }

    private void verifyThatTargetsNotHaveDistributionSetAssignment(final List<Target> targets) {
        final List<Long> targetIds = targets.stream().map(Target::getId).toList();

        final Page<? extends Target> targetsAll = targetManagement.findAll(PAGE);

        for (final Target target : targetsAll) {
            if (targetIds.contains(target.getId())) {
                assertThat(deploymentManagement.findAssignedDistributionSet(target.getControllerId())).isEmpty();
            }
        }
    }

    private void verifyThatCreatedActionsAreInitiatedByCurrentUser(final TargetFilterQuery targetFilterQuery,
            final DistributionSet distributionSet, final List<Target> targets) {
        final Set<String> targetIds = targets.stream().map(Target::getControllerId).collect(Collectors.toSet());

        findActionsByDistributionSet(Pageable.unpaged(), distributionSet.getId()).stream()
                .filter(a -> targetIds.contains(a.getTarget().getControllerId()))
                .forEach(a -> assertThat(a.getInitiatedBy())
                        .as("Action should be initiated by the user who initiated the auto assignment")
                        .isEqualTo(targetFilterQuery.getAutoAssignInitiatedBy()));
    }

    private List<Target> createTargetsAndAutoAssignDistSet(final String prefix, final int targetCount,
            final DistributionSet distributionSet, final ActionType actionType) {

        final List<Target> targets = testdataFactory.createTargets(targetCount, "target" + prefix,
                prefix.concat(SPACE_AND_DESCRIPTION));
        targetFilterQueryManagement.create(Create.builder()
                .name("filter" + prefix).query("id==target" + prefix + "*")
                .autoAssignDistributionSet(distributionSet).autoAssignActionType(actionType)
                .build());

        return targets;
    }

    private void verifyThatTargetsHaveAssignmentActionType(final ActionType actionType, final List<Target> targets) {
        final List<Action> actions = targets.stream()
                .map(Target::getControllerId)
                .flatMap(controllerId -> deploymentManagement.findActionsByTarget(controllerId, PAGE).getContent().stream())
                .toList();

        assertThat(actions).hasSize(targets.size());
        assertThat(actions).allMatch(action -> action.getActionType().equals(actionType));
    }

    private Slice<Action> findActionsByDistributionSet(final Pageable pageable, final long distributionSetId) {
        return actionRepository
                .findAll(byDistributionSetId(distributionSetId), pageable)
                .map(Action.class::cast);
    }
}