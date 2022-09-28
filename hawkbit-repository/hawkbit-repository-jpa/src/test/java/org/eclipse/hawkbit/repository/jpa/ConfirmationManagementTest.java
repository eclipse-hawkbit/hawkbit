/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.hawkbit.repository.exception.AutoConfirmationAlreadyActiveException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.repository.model.DeploymentRequestBuilder;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test class testing the functionality of triggering a deployment of
 * {@link DistributionSet}s to {@link Target}s with AutoConfirmation active.
 *
 */
@Feature("Component Tests - Repository")
@Story("Confirmation Management")
class ConfirmationManagementTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Verify 'findActiveActionsWaitingConfirmation' method is filtering like expected")
    void retrieveActionsWithConfirmationState() {
        enableUserConsentFlow();
        
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

    @Test
    @Description("Verify 'findActiveActionsWaitingConfirmation' method is filtering like expected with multi assignment active")
    void retrieveActionsWithConfirmationStateInMultiAssignment() {
        enableMultiAssignments();
        enableUserConsentFlow();

        final String controllerId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        final List<Action> actions = assignDistributionSet(dsId, controllerId).getAssignedEntity();
        assertThat(actions).hasSize(1);

        assertThat(confirmationManagement.findActiveActionsWaitingConfirmation(controllerId)).hasSize(1)
                .allMatch(action -> action.getStatus() == Status.WAIT_FOR_CONFIRMATION);

        final Long dsId2 = testdataFactory.createDistributionSet().getId();
        assignDistributionSet(dsId2, controllerId);

        assertThat(confirmationManagement.findActiveActionsWaitingConfirmation(controllerId)).hasSize(2)
                .allMatch(action -> action.getStatus() == Status.WAIT_FOR_CONFIRMATION);

        controllerManagement.addUpdateActionStatus(
                entityFactory.actionStatus().create(actions.get(0).getId()).status(Status.RUNNING));

        assertThat(confirmationManagement.findActiveActionsWaitingConfirmation(controllerId)).hasSize(1)
                .allMatch(action -> action.getStatus() == Status.WAIT_FOR_CONFIRMATION
                        && Objects.equals(action.getDistributionSet().getId(), dsId2));

    }

    @Test
    @Description("Verify multiple actions in WFC state will be transferred in RUNNING state in case auto-confirmation is activated.")
    void activateAutoConfirmationInMultiAssignment() {
        enableMultiAssignments();
        enableUserConsentFlow();

        final String controllerId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();
        final Long dsId2 = testdataFactory.createDistributionSet().getId();

        final List<Action> actions = assignDistributionSets(
                Arrays.asList(toDeploymentRequest(controllerId, dsId), toDeploymentRequest(controllerId, dsId2)))
                        .stream().flatMap(s -> s.getAssignedEntity().stream()).collect(Collectors.toList());
        assertThat(actions).hasSize(2);

        assertThat(confirmationManagement.findActiveActionsWaitingConfirmation(controllerId)).hasSize(2)
                .allMatch(action -> action.getStatus() == Status.WAIT_FOR_CONFIRMATION);

        confirmationManagement.activateAutoConfirmation(controllerId, null, null);

        assertThat(confirmationManagement.findActiveActionsWaitingConfirmation(controllerId)).isEmpty();

        assertThat(deploymentManagement.findActionsByTarget(controllerId, PAGE).getContent()).hasSize(2)
                .allMatch(action -> action.getStatus() == Status.RUNNING);
    }

    @Test
    @Description("Verify action in WFC state will be transferred in RUNNING state in case auto-confirmation is activated.")
    void activateAutoConfirmationOnActiveAction() {
        enableUserConsentFlow();

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

    @Test
    @Description("Verify created action after activating auto confirmation is directly in running state.")
    void activateAutoConfirmationAndCreateAction() {
        enableUserConsentFlow();

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

    @ParameterizedTest
    @MethodSource("getAutoConfirmationArguments")
    @Description("Verify activating auto confirmation with different parameters")
    void verifyAutoConfirmationActivationValues(final String initiator, final String remark) {
        final String controllerId = testdataFactory.createTarget().getControllerId();
        confirmationManagement.activateAutoConfirmation(controllerId, initiator, remark);

        assertThat(targetManagement.getByControllerID(controllerId)).hasValueSatisfying(target -> {
            assertThat(target.getAutoConfirmationStatus()).isNotNull()
                    .matches(status -> status.getTarget().getControllerId().equals(controllerId))
                    .matches(status -> Objects.equals(status.getInitiator(), initiator))
                    .matches(status -> Objects.equals(status.getCreatedBy(), "bumlux"))
                    .matches(status -> Objects.equals(status.getRemark(), remark)).satisfies(status -> {
                        final Instant activationTime = Instant.ofEpochMilli(status.getActivatedAt());
                        assertThat(activationTime).isAfterOrEqualTo(activationTime.minusSeconds(3L));
                    });
        });

        confirmationManagement.deactivateAutoConfirmation(controllerId);
        verifyAutoConfirmationIsDisabled(controllerId);
    }

    private static Stream<Arguments> getAutoConfirmationArguments() {
        return Stream.of(Arguments.of("TestUser", "TestRemark"), Arguments.of("TestUser", null),
                Arguments.of(null, "TestRemark"), Arguments.of(null, null));
    }

    @Test
    @Description("Verify activating already active auto confirmation will throw exception.")
    void verifyActivateAlreadyActiveAutoConfirmationThrowException() {
        final String controllerId = testdataFactory.createTarget().getControllerId();

        confirmationManagement.activateAutoConfirmation(controllerId, "any", "any");
        assertThat(targetManagement.getByControllerID(controllerId))
                .hasValueSatisfying(target -> assertThat(target.getAutoConfirmationStatus()).isNotNull());

        assertThatThrownBy(() -> confirmationManagement.activateAutoConfirmation(controllerId, "any", "any"))
                .isInstanceOf(AutoConfirmationAlreadyActiveException.class)
                .hasMessage("Auto confirmation is already active for device " + controllerId);
    }

    @Test
    @Description("Verify disabling already disabled auto confirmation will not have any affect.")
    void disableAlreadyDisabledAutoConfirmationHaveNoAffect() {
        final String controllerId = testdataFactory.createTarget().getControllerId();

        verifyAutoConfirmationIsDisabled(controllerId);
        confirmationManagement.deactivateAutoConfirmation(controllerId);
        verifyAutoConfirmationIsDisabled(controllerId);
    }

    private void verifyAutoConfirmationIsDisabled(final String controllerId) {
        assertThat(targetManagement.getByControllerID(controllerId))
                .hasValueSatisfying(target -> assertThat(target.getAutoConfirmationStatus()).isNull());
    }

    private static DeploymentRequest toDeploymentRequest(final String controllerId, final Long distributionSetId) {
        return new DeploymentRequestBuilder(controllerId, distributionSetId).setConfirmationRequired(true).build();
    }

}
