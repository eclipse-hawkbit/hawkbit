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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.hawkbit.repository.exception.AutoConfirmationAlreadyActiveException;
import org.eclipse.hawkbit.repository.exception.InvalidConfirmationFeedbackException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
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