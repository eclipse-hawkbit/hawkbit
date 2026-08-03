/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.hawkbit.repository.exception.AutoConfirmationAlreadyActiveException;
import org.eclipse.hawkbit.repository.exception.InvalidConfirmationFeedbackException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test class testing the functionality of triggering a deployment of
 * {@link DistributionSet}s to {@link Target}s with AutoConfirmation active.
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Confirmation Management
 */
class ConfirmationManagementTest extends AbstractJpaIntegrationTest {

    /**
     * Verify 'findActiveActionsWaitingConfirmation' method is filtering like expected
     */
    @Test
    void retrieveActionsWithConfirmationState() {
        enableConfirmationFlow();

        final String controllerId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        final List<Action> actions = assignDistributionSet(dsId, controllerId).getAssignedEntity();
        assertThat(actions).hasSize(1);

        assertThat(confirmationManagement.findActiveActionsWaitingConfirmation(controllerId)).hasSize(1)
                .allMatch(action -> action.getStatus() == Status.WAIT_FOR_CONFIRMATION);

        final Long dsId2 = testdataFactory.createDistributionSet().getId();
        // ds1 will be in canceling state afterwards
        assignDistributionSet(dsId2, controllerId);

        assertThat(confirmationManagement.findActiveActionsWaitingConfirmation(controllerId)).hasSize(1)
                .allMatch(action -> action.getStatus() == Status.WAIT_FOR_CONFIRMATION);
    }

    /**
     * Verify confirming an action will put it to the running state
     */
    @Test
    void confirmedActionWillSwitchToRunningState() {
        enableConfirmationFlow();

        final String controllerId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        final List<Action> actions = assignDistributionSet(dsId, controllerId).getAssignedEntity();
        assertThat(actions).hasSize(1).allMatch(action -> action.getStatus() == Status.WAIT_FOR_CONFIRMATION);

        assertThat(confirmationManagement.findActiveActionsWaitingConfirmation(controllerId)).hasSize(1)
                .allMatch(action -> action.getStatus() == Status.WAIT_FOR_CONFIRMATION);
        assertThat(controllerManagement.findActionStatusByAction(actions.get(0).getId(), PAGE)).hasSize(1)
                .allMatch(status -> status.getStatus() == Status.WAIT_FOR_CONFIRMATION);

        final Action newAction = confirmationManagement.confirmAction(actions.get(0).getId(), null, null);

        assertThat(confirmationManagement.findActiveActionsWaitingConfirmation(controllerId)).isEmpty();

        // verify action in RUNNING state
        assertThat(newAction.getStatus()).isEqualTo(Status.RUNNING);

        // status entry RUNNING should be present in status history
        assertThat(controllerManagement.findActionStatusByAction(newAction.getId(), PAGE)).hasSize(2)
                .anyMatch(status -> status.getStatus() == Status.RUNNING);
    }

    /**
     * Verify confirming an confirmed action will lead to a specific failure
     */
    @Test
    void confirmedActionCannotBeConfirmedAgain() {
        enableConfirmationFlow();

        final String controllerId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        final List<Action> actions = assignDistributionSet(dsId, controllerId).getAssignedEntity();
        assertThat(actions).hasSize(1).allMatch(action -> action.getStatus() == Status.WAIT_FOR_CONFIRMATION);
        final Long actionId = actions.get(0).getId();
        final Action newAction = confirmationManagement.confirmAction(actionId, null, null);
        // verify action in RUNNING state
        assertThat(newAction.getStatus()).isEqualTo(Status.RUNNING);

        assertThatThrownBy(() -> confirmationManagement.confirmAction(actionId, null, null))
                .isInstanceOf(InvalidConfirmationFeedbackException.class)
                .matches(e -> ((InvalidConfirmationFeedbackException) e)
                        .getReason() == InvalidConfirmationFeedbackException.Reason.NOT_AWAITING_CONFIRMATION);
    }

    /**
     * Verify confirming a closed action will lead to a specific failure
     */
    @Test
    void confirmedActionCannotBeGivenOnFinishedAction() {
        enableConfirmationFlow();
        final Long actionId = prepareFinishedUpdate().getId();
        assertThatThrownBy(() -> confirmationManagement.confirmAction(actionId, null, null))
                .isInstanceOf(InvalidConfirmationFeedbackException.class)
                .matches(e -> ((InvalidConfirmationFeedbackException) e)
                        .getReason() == InvalidConfirmationFeedbackException.Reason.ACTION_CLOSED);
    }

    /**
     * Verify denying an action will leave it in WFC state
     */
    @Test
    void deniedActionWillStayInWfcState() {
        enableConfirmationFlow();

        final String controllerId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        final List<Action> actions = assignDistributionSet(dsId, controllerId).getAssignedEntity();
        assertThat(actions).hasSize(1).allMatch(action -> action.getStatus() == Status.WAIT_FOR_CONFIRMATION);
        assertThat(confirmationManagement.findActiveActionsWaitingConfirmation(controllerId)).hasSize(1)
                .allMatch(action -> action.getStatus() == Status.WAIT_FOR_CONFIRMATION);
        assertThat(controllerManagement.findActionStatusByAction(actions.get(0).getId(), PAGE)).hasSize(1)
                .allMatch(status -> status.getStatus() == Status.WAIT_FOR_CONFIRMATION);

        final Action newAction = confirmationManagement.denyAction(actions.get(0).getId(), null, null);

        assertThat(confirmationManagement.findActiveActionsWaitingConfirmation(controllerId)).hasSize(1)
                .allMatch(action -> action.getStatus() == Status.WAIT_FOR_CONFIRMATION);

        // verify action still in WFC state
        assertThat(newAction.getStatus()).isEqualTo(Status.WAIT_FOR_CONFIRMATION);

        // no status entry RUNNING should be present in status history
        assertThat(controllerManagement.findActionStatusByAction(newAction.getId(), PAGE)).hasSize(2)
                .noneMatch(status -> status.getStatus() == Status.RUNNING);
    }

    /**
     * Verify denying a manually confirmed (RUNNING) action reverts it back to WFC state
     */
    @Test
    void deniedActionAfterManualConfirmRevertsToWfcState() {
        enableConfirmationFlow();

        final String controllerId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        final List<Action> actions = assignDistributionSet(dsId, controllerId).getAssignedEntity();
        assertThat(actions).hasSize(1).allMatch(action -> action.getStatus() == Status.WAIT_FOR_CONFIRMATION);
        final Long actionId = actions.get(0).getId();

        // manual consent -> RUNNING
        assertThat(confirmationManagement.confirmAction(actionId, null, null).getStatus()).isEqualTo(Status.RUNNING);
        assertThat(confirmationManagement.findActiveActionsWaitingConfirmation(controllerId)).isEmpty();

        // revoke consent -> back to WAIT_FOR_CONFIRMATION
        final Action revokedAction = confirmationManagement.denyAction(actionId, null, null);
        assertThat(revokedAction.getStatus()).isEqualTo(Status.WAIT_FOR_CONFIRMATION);

        // action is offered for confirmation again
        assertThat(confirmationManagement.findActiveActionsWaitingConfirmation(controllerId)).hasSize(1)
                .allMatch(action -> action.getStatus() == Status.WAIT_FOR_CONFIRMATION);

        // audit trail: WFC (initial) -> RUNNING (confirm) -> WAIT_FOR_CONFIRMATION (revoke)
        assertThat(controllerManagement.findActionStatusByAction(actionId, PAGE)).hasSize(3)
                .anyMatch(status -> status.getStatus() == Status.RUNNING)
                .anyMatch(status -> status.getStatus() == Status.WAIT_FOR_CONFIRMATION);
    }

    /**
     * Verify denying an auto-confirmed (RUNNING) action reverts it back to WFC state
     */
    @Test
    void deniedActionAfterAutoConfirmRevertsToWfcState() {
        enableConfirmationFlow();

        final String controllerId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        // auto-confirmation -> assigned action goes straight to RUNNING
        confirmationManagement.activateAutoConfirmation(controllerId, null, null);

        final List<Action> actions = assignDistributionSet(dsId, controllerId).getAssignedEntity();
        assertThat(actions).hasSize(1).allMatch(action -> action.getStatus() == Status.RUNNING);
        final Long actionId = actions.get(0).getId();
        assertThat(confirmationManagement.findActiveActionsWaitingConfirmation(controllerId)).isEmpty();

        // revoke consent -> back to WAIT_FOR_CONFIRMATION
        final Action revokedAction = confirmationManagement.denyAction(actionId, null, null);
        assertThat(revokedAction.getStatus()).isEqualTo(Status.WAIT_FOR_CONFIRMATION);

        // action is offered for confirmation again
        assertThat(confirmationManagement.findActiveActionsWaitingConfirmation(controllerId)).hasSize(1)
                .allMatch(action -> action.getStatus() == Status.WAIT_FOR_CONFIRMATION);
    }

    /**
     * Verify a revoked (reverted to WFC) action can be confirmed again and returns to RUNNING
     */
    @Test
    void revokedActionCanBeConfirmedAgain() {
        enableConfirmationFlow();

        final String controllerId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        final List<Action> actions = assignDistributionSet(dsId, controllerId).getAssignedEntity();
        assertThat(actions).hasSize(1);
        final Long actionId = actions.get(0).getId();

        // confirm -> RUNNING
        assertThat(confirmationManagement.confirmAction(actionId, null, null).getStatus()).isEqualTo(Status.RUNNING);
        // revoke -> WAIT_FOR_CONFIRMATION
        assertThat(confirmationManagement.denyAction(actionId, null, null).getStatus())
                .isEqualTo(Status.WAIT_FOR_CONFIRMATION);
        // re-confirm -> RUNNING again
        assertThat(confirmationManagement.confirmAction(actionId, null, null).getStatus()).isEqualTo(Status.RUNNING);

        assertThat(confirmationManagement.findActiveActionsWaitingConfirmation(controllerId)).isEmpty();
    }

    /**
     * Verify denying a closed action will lead to a specific failure
     */
    @Test
    void deniedActionCannotBeGivenOnFinishedAction() {
        enableConfirmationFlow();
        final Long actionId = prepareFinishedUpdate().getId();
        assertThatThrownBy(() -> confirmationManagement.denyAction(actionId, null, null))
                .isInstanceOf(InvalidConfirmationFeedbackException.class)
                .matches(e -> ((InvalidConfirmationFeedbackException) e)
                        .getReason() == InvalidConfirmationFeedbackException.Reason.ACTION_CLOSED);
    }

    /**
     * Verify denying a canceling action is rejected and keeps the action in canceling state
     */
    @Test
    void deniedActionNotPossibleForCancelingAction() {
        enableConfirmationFlow();

        final String controllerId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        final List<Action> actions = assignDistributionSet(dsId, controllerId).getAssignedEntity();
        assertThat(actions).hasSize(1);
        final Long actionId = actions.get(0).getId();

        // confirm -> RUNNING and cancel it afterwards (soft cancel) -> CANCELING
        assertThat(confirmationManagement.confirmAction(actionId, null, null).getStatus()).isEqualTo(Status.RUNNING);
        assertThat(deploymentManagement.cancelAction(actionId).getStatus()).isEqualTo(Status.CANCELING);

        // the target gets a cancel action on poll - denying must not push it back to WAIT_FOR_CONFIRMATION
        assertThatThrownBy(() -> confirmationManagement.denyAction(actionId, null, null))
                .isInstanceOf(InvalidConfirmationFeedbackException.class)
                .matches(e -> ((InvalidConfirmationFeedbackException) e)
                        .getReason() == InvalidConfirmationFeedbackException.Reason.NOT_AWAITING_CONFIRMATION);

        assertThat(deploymentManagement.findAction(actionId).orElseThrow().getStatus()).isEqualTo(Status.CANCELING);
        assertThat(confirmationManagement.findActiveActionsWaitingConfirmation(controllerId)).isEmpty();
    }

    /**
     * Verify denying a running action is rejected in case the confirmation flow is disabled
     */
    @Test
    void deniedActionNotPossibleWithDisabledConfirmationFlow() {
        final String controllerId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        // confirmation flow disabled -> action is directly in RUNNING state
        final List<Action> actions = assignDistributionSet(dsId, controllerId).getAssignedEntity();
        assertThat(actions).hasSize(1).allMatch(action -> action.getStatus() == Status.RUNNING);
        final Long actionId = actions.get(0).getId();

        assertThatThrownBy(() -> confirmationManagement.denyAction(actionId, null, null))
                .isInstanceOf(InvalidConfirmationFeedbackException.class)
                .matches(e -> ((InvalidConfirmationFeedbackException) e)
                        .getReason() == InvalidConfirmationFeedbackException.Reason.NOT_AWAITING_CONFIRMATION);

        assertThat(deploymentManagement.findAction(actionId).orElseThrow().getStatus()).isEqualTo(Status.RUNNING);
        assertThat(confirmationManagement.findActiveActionsWaitingConfirmation(controllerId)).isEmpty();
    }

    /**
     * Verify an action waiting for confirmation can still be denied after the confirmation flow got disabled
     */
    @Test
    void deniedActionStillPossibleForWfcActionAfterDisablingConfirmationFlow() {
        enableConfirmationFlow();

        final String controllerId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        final List<Action> actions = assignDistributionSet(dsId, controllerId).getAssignedEntity();
        assertThat(actions).hasSize(1).allMatch(action -> action.getStatus() == Status.WAIT_FOR_CONFIRMATION);
        final Long actionId = actions.get(0).getId();

        disableConfirmationFlow();

        assertThat(confirmationManagement.denyAction(actionId, null, null).getStatus())
                .isEqualTo(Status.WAIT_FOR_CONFIRMATION);
    }

    /**
     * Verify action in WFC state will be transferred in RUNNING state in case auto-confirmation is activated.
     */
    @Test
    void activateAutoConfirmationOnActiveAction() {
        enableConfirmationFlow();

        final String controllerId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        // do assignment and verify
        assertThat(assignDistributionSet(dsId, controllerId).getAssignedEntity()).hasSize(1);

        assertThat(confirmationManagement.findActiveActionsWaitingConfirmation(controllerId)).hasSize(1)
                .allMatch(action -> action.getStatus() == Status.WAIT_FOR_CONFIRMATION);

        confirmationManagement.activateAutoConfirmation(controllerId, null, null);

        assertThat(confirmationManagement.findActiveActionsWaitingConfirmation(controllerId)).isEmpty();

        assertThat(deploymentManagement.findActionsByTarget(controllerId, PAGE).getContent()).hasSize(1)
                .allMatch(action -> action.getStatus() == Status.RUNNING);
    }

    /**
     * Verify created action after activating auto confirmation is directly in running state.
     */
    @Test
    void activateAutoConfirmationAndCreateAction() {
        enableConfirmationFlow();

        final String controllerId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        assertThat(confirmationManagement.findActiveActionsWaitingConfirmation(controllerId)).isEmpty();

        confirmationManagement.activateAutoConfirmation(controllerId, null, null);

        // do assignment and verify
        assertThat(assignDistributionSet(dsId, controllerId).getAssignedEntity()).hasSize(1);

        assertThat(confirmationManagement.findActiveActionsWaitingConfirmation(controllerId)).isEmpty();

        assertThat(deploymentManagement.findActionsByTarget(controllerId, PAGE).getContent()).hasSize(1)
                .allMatch(action -> action.getStatus() == Status.RUNNING);
    }

    /**
     * Verify activating auto confirmation with different parameters
     */
    @ParameterizedTest
    @MethodSource("getAutoConfirmationArguments")
    void verifyAutoConfirmationActivationValues(final String initiator, final String remark) {
        final String controllerId = testdataFactory.createTarget().getControllerId();
        confirmationManagement.activateAutoConfirmation(controllerId, initiator, remark);

        assertThat(targetManagement.getWithAutoConfigurationStatus(controllerId).getAutoConfirmationStatus())
                .isNotNull()
                .matches(status -> status.getTarget().getControllerId().equals(controllerId))
                .matches(status -> Objects.equals(status.getInitiator(), initiator))
                .matches(status -> Objects.equals(status.getCreatedBy(), "bumlux"))
                .matches(status -> Objects.equals(status.getRemark(), remark)).satisfies(status -> {
                    final Instant activationTime = Instant.ofEpochMilli(status.getActivatedAt());
                    assertThat(activationTime).isAfterOrEqualTo(activationTime.minusSeconds(3L));
                });

        confirmationManagement.deactivateAutoConfirmation(controllerId);
        verifyAutoConfirmationIsDisabled(controllerId);
    }

    /**
     * Verify activating already active auto confirmation will throw exception.
     */
    @Test
    void verifyActivateAlreadyActiveAutoConfirmationThrowException() {
        final String controllerId = testdataFactory.createTarget().getControllerId();

        confirmationManagement.activateAutoConfirmation(controllerId, "any", "any");
        assertThat(targetManagement.getWithAutoConfigurationStatus(controllerId).getAutoConfirmationStatus()).isNotNull();

        assertThatThrownBy(() -> confirmationManagement.activateAutoConfirmation(controllerId, "any", "any"))
                .isInstanceOf(AutoConfirmationAlreadyActiveException.class)
                .hasMessage("Auto confirmation is already active for device " + controllerId);
    }

    /**
     * Verify disabling already disabled auto confirmation will not have any affect.
     */
    @Test
    void disableAlreadyDisabledAutoConfirmationHaveNoAffect() {
        final String controllerId = testdataFactory.createTarget().getControllerId();

        verifyAutoConfirmationIsDisabled(controllerId);
        confirmationManagement.deactivateAutoConfirmation(controllerId);
        verifyAutoConfirmationIsDisabled(controllerId);
    }

    private static Stream<Arguments> getAutoConfirmationArguments() {
        return Stream.of(
                Arguments.of("TestUser", "TestRemark"),
                Arguments.of("TestUser", null),
                Arguments.of(null, "TestRemark"),
                Arguments.of(null, null));
    }

    private void verifyAutoConfirmationIsDisabled(final String controllerId) {
        assertThat(targetManagement.getWithAutoConfigurationStatus(controllerId).getAutoConfirmationStatus()).isNull();
    }
}