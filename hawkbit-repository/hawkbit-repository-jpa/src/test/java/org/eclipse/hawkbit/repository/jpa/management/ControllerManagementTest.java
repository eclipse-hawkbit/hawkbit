/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
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
import static org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions.CONTROLLER_ROLE;
import static org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions.CONTROLLER_ROLE_ANONYMOUS;
import static org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions.IS_CONTROLLER;
import static org.eclipse.hawkbit.repository.jpa.configuration.Constants.TX_RT_MAX;
import static org.eclipse.hawkbit.repository.model.Action.ActionType.DOWNLOAD_ONLY;
import static org.eclipse.hawkbit.repository.test.util.TestdataFactory.DEFAULT_CONTROLLER_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.validation.ConstraintViolationException;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.UpdateMode;
import org.eclipse.hawkbit.repository.builder.ActionStatusCreate;
import org.eclipse.hawkbit.repository.event.remote.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAttributesRequestedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTypeCreatedEvent;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.CancelActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InvalidTargetAttributeException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.specifications.ActionSpecifications;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.ArtifactUpload;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.repository.test.util.TargetTestData;
import org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch;
import org.eclipse.hawkbit.repository.test.util.WithUser;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;

@Feature("Component Tests - Repository")
@Story("Controller Management")
class ControllerManagementTest extends AbstractJpaIntegrationTest {

    @Autowired
    private RepositoryProperties repositoryProperties;

    @Test
    @Description("Verifies that management get access react as specified on calls for non existing entities by means "
            + "of Optional not present.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 1) })
    void nonExistingEntityAccessReturnsNotPresent() {
        final Target target = testdataFactory.createTarget();
        final SoftwareModule module = testdataFactory.createSoftwareModuleOs();

        assertThat(controllerManagement.findActionWithDetails(NOT_EXIST_IDL)).isNotPresent();
        assertThat(controllerManagement.getByControllerId(NOT_EXIST_ID)).isNotPresent();
        assertThat(controllerManagement.get(NOT_EXIST_IDL)).isNotPresent();
        assertThat(controllerManagement.getActionForDownloadByTargetAndSoftwareModule(target.getControllerId(),
                module.getId())).isNotPresent();

        assertThat(controllerManagement.findActiveActionWithHighestWeight(NOT_EXIST_ID)).isNotPresent();

        assertThat(controllerManagement.hasTargetArtifactAssigned(target.getControllerId(), "XXX")).isFalse();
        assertThat(controllerManagement.hasTargetArtifactAssigned(target.getId(), "XXX")).isFalse();
    }

    @Test
    @Description("Verifies that management queries react as specified on calls for non existing entities "
            + " by means of throwing EntityNotFoundException.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 1) })
    void entityQueriesReferringToNotExistingEntitiesThrowsException() throws URISyntaxException {
        final Target target = testdataFactory.createTarget();
        final SoftwareModule module = testdataFactory.createSoftwareModuleOs();

        verifyThrownExceptionBy(() -> controllerManagement.addCancelActionStatus(
                entityFactory.actionStatus().create(NOT_EXIST_IDL).status(Action.Status.FINISHED)), "Action");

        verifyThrownExceptionBy(() -> controllerManagement.addInformationalActionStatus(
                entityFactory.actionStatus().create(NOT_EXIST_IDL).status(Action.Status.RUNNING)), "Action");

        verifyThrownExceptionBy(() -> controllerManagement.addUpdateActionStatus(
                entityFactory.actionStatus().create(NOT_EXIST_IDL).status(Action.Status.FINISHED)), "Action");

        verifyThrownExceptionBy(() -> controllerManagement
                .getActionForDownloadByTargetAndSoftwareModule(target.getControllerId(), NOT_EXIST_IDL),
                "SoftwareModule");

        verifyThrownExceptionBy(
                () -> controllerManagement.getActionForDownloadByTargetAndSoftwareModule(NOT_EXIST_ID, module.getId()),
                "Target");

        verifyThrownExceptionBy(() -> controllerManagement.findActionStatusByAction(PAGE, NOT_EXIST_IDL), "Action");
        verifyThrownExceptionBy(() -> controllerManagement.hasTargetArtifactAssigned(NOT_EXIST_IDL, "XXX"), "Target");

        verifyThrownExceptionBy(() -> controllerManagement.hasTargetArtifactAssigned(NOT_EXIST_ID, "XXX"), "Target");

        verifyThrownExceptionBy(() -> controllerManagement.registerRetrieved(NOT_EXIST_IDL, "test message"), "Action");

        verifyThrownExceptionBy(
                () -> controllerManagement.updateControllerAttributes(NOT_EXIST_ID, new HashMap<>(), null), "Target");
    }

    @Test
    @Description("Controller confirms successful update with FINISHED status.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1) })
    void controllerConfirmsUpdateWithFinished() {
        final Long actionId = createTargetAndAssignDs();

        simulateIntermediateStatusOnUpdate(actionId);

        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.FINISHED));
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.IN_SYNC, Action.Status.FINISHED,
                Action.Status.FINISHED, false);

        assertThat(actionStatusRepository.count()).isEqualTo(7);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(7);
    }

    @Test
    @Description("Controller confirmation fails with invalid messages.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    void controllerConfirmationFailsWithInvalidMessages() {
        final Long actionId = createTargetAndAssignDs();

        simulateIntermediateStatusOnUpdate(actionId);

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set invalid description text should not be created")
                .isThrownBy(() -> controllerManagement.addUpdateActionStatus(entityFactory.actionStatus()
                        .create(actionId).status(Action.Status.FINISHED).message(INVALID_TEXT_HTML)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set invalid description text should not be created")
                .isThrownBy(() -> controllerManagement.addUpdateActionStatus(
                        entityFactory.actionStatus().create(actionId).status(Action.Status.FINISHED)
                                .messages(Arrays.asList("this is valid.", INVALID_TEXT_HTML))));

        assertThat(actionStatusRepository.count()).isEqualTo(6);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(6);
    }

    @Test
    @Description("Controller confirms successful update with FINISHED status on a action that is on canceling. "
            + "Reason: The decision to ignore the cancellation is in fact up to the controller.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1) })
    void controllerConfirmsUpdateWithFinishedAndIgnoresCancellationWithThat() {
        final Long actionId = createTargetAndAssignDs();
        deploymentManagement.cancelAction(actionId);

        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.FINISHED));
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.IN_SYNC, Action.Status.FINISHED,
                Action.Status.FINISHED, false);

        assertThat(actionStatusRepository.count()).isEqualTo(3);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(3);
    }

    @Test
    @Description("Update server rejects cancellation feedback if action is not in CANCELING state.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    void cancellationFeedbackRejectedIfActionIsNotInCanceling() {
        final Long actionId = createTargetAndAssignDs();

        assertThatExceptionOfType(CancelActionNotAllowedException.class)
                .as("Expected " + CancelActionNotAllowedException.class.getName())
                .isThrownBy(() -> controllerManagement.addCancelActionStatus(
                        entityFactory.actionStatus().create(actionId).status(Action.Status.FINISHED)));

        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, Action.Status.RUNNING,
                Action.Status.RUNNING, true);

        assertThat(actionStatusRepository.count()).isEqualTo(1);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(1);
    }

    @Test
    @Description("Controller confirms action cancellation with FINISHED status.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    void controllerConfirmsActionCancellationWithFinished() {
        final Long actionId = createTargetAndAssignDs();

        deploymentManagement.cancelAction(actionId);
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, Action.Status.CANCELING,
                Action.Status.CANCELING, true);

        simulateIntermediateStatusOnCancellation(actionId);

        controllerManagement
                .addCancelActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.FINISHED));
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.IN_SYNC, Action.Status.CANCELED,
                Action.Status.FINISHED, false);

        assertThat(actionStatusRepository.count()).isEqualTo(8);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(8);
    }

    @Test
    @Description("Controller confirms action cancellation with FINISHED status.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    void controllerConfirmsActionCancellationWithCanceled() {
        final Long actionId = createTargetAndAssignDs();

        deploymentManagement.cancelAction(actionId);
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, Action.Status.CANCELING,
                Action.Status.CANCELING, true);

        simulateIntermediateStatusOnCancellation(actionId);

        controllerManagement
                .addCancelActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.CANCELED));
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.IN_SYNC, Action.Status.CANCELED,
                Action.Status.CANCELED, false);

        assertThat(actionStatusRepository.count()).isEqualTo(8);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(8);
    }

    @Test
    @Description("Controller rejects action cancellation with CANCEL_REJECTED status. Action goes back to RUNNING status as it expects "
            + "that the controller will continue the original update.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    void controllerRejectsActionCancellationWithReject() {
        final Long actionId = createTargetAndAssignDs();

        deploymentManagement.cancelAction(actionId);
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, Action.Status.CANCELING,
                Action.Status.CANCELING, true);

        simulateIntermediateStatusOnCancellation(actionId);

        controllerManagement.addCancelActionStatus(
                entityFactory.actionStatus().create(actionId).status(Action.Status.CANCEL_REJECTED));
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, Action.Status.RUNNING,
                Action.Status.CANCEL_REJECTED, true);

        assertThat(actionStatusRepository.count()).isEqualTo(8);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(8);
    }

    @Test
    @Description("Controller rejects action cancellation with ERROR status. Action goes back to RUNNING status as it expects "
            + "that the controller will continue the original update.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    void controllerRejectsActionCancellationWithError() {
        final Long actionId = createTargetAndAssignDs();

        deploymentManagement.cancelAction(actionId);
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, Action.Status.CANCELING,
                Action.Status.CANCELING, true);

        simulateIntermediateStatusOnCancellation(actionId);

        controllerManagement
                .addCancelActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.ERROR));
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, Action.Status.RUNNING,
                Action.Status.ERROR, true);

        assertThat(actionStatusRepository.count()).isEqualTo(8);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(8);
    }

    @Step
    private Long createTargetAndAssignDs() {
        final Long dsId = testdataFactory.createDistributionSet().getId();
        testdataFactory.createTarget();
        assignDistributionSet(dsId, DEFAULT_CONTROLLER_ID);
        assertThat(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.PENDING);

        return deploymentManagement.findActiveActionsByTarget(PAGE, DEFAULT_CONTROLLER_ID).getContent().get(0).getId();
    }

    @Step
    private Long createAndAssignDsAsDownloadOnly(final String dsName, final String defaultControllerId) {
        final Long dsId = testdataFactory.createDistributionSet(dsName).getId();
        assignDistributionSet(dsId, defaultControllerId, DOWNLOAD_ONLY);
        assertThat(targetManagement.getByControllerID(defaultControllerId).get().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.PENDING);

        final Long id = deploymentManagement.findActiveActionsByTarget(PAGE, defaultControllerId).getContent().get(0)
                .getId();
        assertThat(id).isNotNull();
        return id;
    }

    @Step
    private Long assignDs(final Long dsId, final String defaultControllerId, final Action.ActionType actionType) {
        assignDistributionSet(dsId, defaultControllerId, actionType);
        assertThat(targetManagement.getByControllerID(defaultControllerId).get().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.PENDING);

        final Long id = deploymentManagement.findActiveActionsByTarget(PAGE, defaultControllerId).getContent().get(0)
                .getId();
        assertThat(id).isNotNull();
        return id;
    }

    @Step
    private void simulateIntermediateStatusOnCancellation(final Long actionId) {
        controllerManagement
                .addCancelActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.RUNNING));
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, Action.Status.CANCELING,
                Action.Status.RUNNING, true);

        controllerManagement
                .addCancelActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.DOWNLOAD));
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, Action.Status.CANCELING,
                Action.Status.DOWNLOAD, true);

        controllerManagement
                .addCancelActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.DOWNLOADED));
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, Action.Status.CANCELING,
                Action.Status.DOWNLOADED, true);

        controllerManagement
                .addCancelActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.RETRIEVED));
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, Action.Status.CANCELING,
                Action.Status.RETRIEVED, true);

        controllerManagement
                .addCancelActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.WARNING));
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, Action.Status.CANCELING,
                Action.Status.WARNING, true);
    }

    @Step
    private void simulateIntermediateStatusOnUpdate(final Long actionId) {
        addUpdateActionStatusAndAssert(actionId, Action.Status.RUNNING);

        addUpdateActionStatusAndAssert(actionId, Action.Status.DOWNLOAD);

        addUpdateActionStatusAndAssert(actionId, Action.Status.DOWNLOADED);

        addUpdateActionStatusAndAssert(actionId, Action.Status.RETRIEVED);

        addUpdateActionStatusAndAssert(actionId, Action.Status.WARNING);
    }

    private void addUpdateActionStatusAndAssert(final Long actionId, final Action.Status actionStatus) {
        addUpdateActionStatusAndAssert(actionId, actionStatus, null);
    }

    private void addUpdateActionStatusAndAssert(final Long actionId, final Action.Status actionStatus,
            final Integer code) {
        final ActionStatusCreate status = entityFactory.actionStatus().create(actionId).status(actionStatus);
        if (code != null) {
            status.code(code.intValue());
        }
        controllerManagement
                .addUpdateActionStatus(status);
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, Action.Status.RUNNING,
                actionStatus, true);
    }

    private void assertActionStatus(final Long actionId, final String controllerId,
            final TargetUpdateStatus expectedTargetUpdateStatus, final Action.Status expectedActionActionStatus,
            final Action.Status expectedActionStatus, final boolean actionActive) {
        final TargetUpdateStatus targetStatus = targetManagement.getByControllerID(controllerId).get()
                .getUpdateStatus();
        assertThat(targetStatus).isEqualTo(expectedTargetUpdateStatus);
        final Action action = deploymentManagement.findAction(actionId).get();
        assertThat(action.getStatus()).isEqualTo(expectedActionActionStatus);
        assertThat(action.isActive()).isEqualTo(actionActive);
        final List<ActionStatus> actionStatusList = controllerManagement.findActionStatusByAction(PAGE, actionId)
                .getContent();
        assertThat(actionStatusList.get(actionStatusList.size() - 1).getStatus()).isEqualTo(expectedActionStatus);
        if (actionActive) {
            assertThat(controllerManagement.findActiveActionWithHighestWeight(controllerId).get().getId())
                    .isEqualTo(actionId);
        }
    }

    @Test
    @Description("Verifies that assignment verification works based on SHA1 hash. By design it is not important which artifact "
            + "is actually used for the check as long as they have an identical binary, i.e. same SHA1 hash. ")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 6),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 5), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    void hasTargetArtifactAssignedIsTrueWithMultipleArtifacts() {
        final int artifactSize = 5 * 1024;
        final byte[] random = RandomUtils.nextBytes(artifactSize);

        final DistributionSet ds = testdataFactory.createDistributionSet("");
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2");
        Target savedTarget = testdataFactory.createTarget();

        // create two artifacts with identical SHA1 hash
        final Artifact artifact = artifactManagement.create(new ArtifactUpload(new ByteArrayInputStream(random),
                ds.findFirstModuleByType(osType).get().getId(), "file1", false, artifactSize));
        final Artifact artifact2 = artifactManagement.create(new ArtifactUpload(new ByteArrayInputStream(random),
                ds2.findFirstModuleByType(osType).get().getId(), "file1", false, artifactSize));
        assertThat(artifact.getSha1Hash()).isEqualTo(artifact2.getSha1Hash());

        assertThat(
                controllerManagement.hasTargetArtifactAssigned(savedTarget.getControllerId(), artifact.getSha1Hash()))
                        .isFalse();
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));
        assertThat(
                controllerManagement.hasTargetArtifactAssigned(savedTarget.getControllerId(), artifact.getSha1Hash()))
                        .isTrue();
        assertThat(
                controllerManagement.hasTargetArtifactAssigned(savedTarget.getControllerId(), artifact2.getSha1Hash()))
                        .isTrue();
    }

    @Test
    @Description("Register a controller which does not exist")
    @WithUser(principal = "controller", authorities = { CONTROLLER_ROLE })
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 2) })
    void findOrRegisterTargetIfItDoesNotExist() {
        final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST);
        assertThat(target).as("target should not be null").isNotNull();

        final Target sameTarget = controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST);
        assertThat(target.getId()).as("Target should be the equals").isEqualTo(sameTarget.getId());
        assertThat(targetRepository.count()).as("Only 1 target should be registered").isEqualTo(1L);
    }

    @Test
    @Description("Register a controller with name which does not exist and update its name")
    @WithUser(principal = "controller", authorities = { CONTROLLER_ROLE })
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 2), @Expect(type = TargetUpdatedEvent.class, count = 1) })
    void findOrRegisterTargetIfItDoesNotExistWithName() {
        final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST, "TestName", null);
        final Target sameTarget = controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST,
                "ChangedTestName", null);
        assertThat(target.getId()).as("Target should be the equals").isEqualTo(sameTarget.getId());
        assertThat(target.getName()).as("Target names should be different").isNotEqualTo(sameTarget.getName());
        assertThat(sameTarget.getName()).as("Target name should be changed").isEqualTo("ChangedTestName");
        assertThat(targetRepository.count()).as("Only 1 target should be registred").isEqualTo(1L);
    }

    @Test
    @Description("Register a controller which does not exist with existing target type and update its target type to another existing one")
    @WithUser(principal = "controller", authorities = { CONTROLLER_ROLE })
    @ExpectEvents({ @Expect(type = TargetTypeCreatedEvent.class, count = 2),
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 1) })
    void findOrRegisterTargetIfItDoesNotExistWithExistingTypeAndUpdateToExistingType() {
        createTargetType("knownTargetTypeName1");
        createTargetType("knownTargetTypeName2");
        final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST,
                null, "knownTargetTypeName1");
        final Target sameTarget = controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST,
                null, "knownTargetTypeName2");
        assertThat(target.getId()).as("Target should be the same").isEqualTo(sameTarget.getId());
        assertThat(target.getTargetType().getName()).as("Target type should be set")
                .isEqualTo("knownTargetTypeName1");
        assertThat(sameTarget.getTargetType().getName()).as("Target type should be changed")
                .isEqualTo("knownTargetTypeName2");
        assertThat(targetRepository.count()).as("Only 1 target should be registred").isEqualTo(1L);
    }

    private void createTargetType(String targetTypeName) {
        systemSecurityContext.runAsSystem(() ->
                targetTypeManagement.create(entityFactory.targetType().create().name(targetTypeName)));
    }

    @Test
    @Description("Register a controller which does not exist with existing target type and update its target type to non existing one")
    @WithUser(principal = "controller", authorities = { CONTROLLER_ROLE })
    @ExpectEvents({ @Expect(type = TargetTypeCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 2) })
    void findOrRegisterTargetIfItDoesNotExistWithExistingTypeAndUpdateToNonExistingType() {
        createTargetType("knownTargetTypeName");
        final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST,
                null,"knownTargetTypeName");
        final Target sameTarget = controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST,
                null,"unknownTargetTypeName");
        assertThat(target.getId()).as("Target should be the same").isEqualTo(sameTarget.getId());
        assertThat(sameTarget.getTargetType().getName()).as("Target type should be unchanged")
                .isEqualTo("knownTargetTypeName");
        assertThat(targetRepository.count()).as("Only 1 target should be registred").isEqualTo(1L);
    }

    @Test
    @Description("Register a controller which does not exist with existing target type and unassign its target type")
    @WithUser(principal = "controller", authorities = { CONTROLLER_ROLE })
    @ExpectEvents({ @Expect(type = TargetTypeCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 1) })
    void findOrRegisterTargetIfItDoesNotExistWithExistingTypeAndUnassignType() {
        createTargetType("knownTargetTypeName");
        final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST,
                null, "knownTargetTypeName");
        final Target sameTarget = controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST,
                null, "");
        assertThat(target.getId()).as("Target should be the same").isEqualTo(sameTarget.getId());
        assertThat(sameTarget.getTargetType()).as("Target type should be unassigned")
                .isNull();
        assertThat(targetRepository.count()).as("Only 1 target should be registred").isEqualTo(1L);
    }

    @Test
    @Description("Register a controller which does not exist without target type and update its target type to existing one")
    @WithUser(principal = "controller", authorities = { CONTROLLER_ROLE })
    @ExpectEvents({ @Expect(type = TargetTypeCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 1) })
    void findOrRegisterTargetIfItDoesNotExistWithoutTypeAndUpdateToExistingType() {
        createTargetType("knownTargetTypeName");
        final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST,
                null, null);
        final Target sameTarget = controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST,
                null, "knownTargetTypeName");
        assertThat(target.getId()).as("Target should be the equals").isEqualTo(sameTarget.getId());
        assertThat(target.getTargetType()).as("Target type should not be assigned")
                .isNull();
        assertThat(sameTarget.getTargetType().getName()).as("Target type should be assigned")
                .isEqualTo("knownTargetTypeName");
        assertThat(targetRepository.count()).as("Only 1 target should be registred").isEqualTo(1L);
    }

    @Test
    @Description("Register a controller which does not exist with non existing target type and update its target type to existing one")
    @WithUser(principal = "controller", authorities = { CONTROLLER_ROLE })
    @ExpectEvents({ @Expect(type = TargetTypeCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 1) })
    void findOrRegisterTargetIfItDoesNotExistWithNonExistingTypeAndUpdateToExistingType() {
        createTargetType("knownTargetTypeName");
        final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST,
                null, "unknownTargetTypeName");
        final Target sameTarget = controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST,
                null, "knownTargetTypeName");
        assertThat(target.getId()).as("Target should be the equals").isEqualTo(sameTarget.getId());
        assertThat(target.getTargetType()).as("Target type should not be assigned")
                .isNull();
        assertThat(sameTarget.getTargetType().getName()).as("Target type should be assigned")
                .isEqualTo("knownTargetTypeName");
        assertThat(targetRepository.count()).as("Only 1 target should be registred").isEqualTo(1L);
    }

    @Test
    @Description("Tries to register a target with an invalid controller id")
    void findOrRegisterTargetIfItDoesNotExistThrowsExceptionForInvalidControllerIdParam() {
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("register target with null as controllerId should fail")
                .isThrownBy(() -> controllerManagement.findOrRegisterTargetIfItDoesNotExist(null, LOCALHOST));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("register target with empty controllerId should fail")
                .isThrownBy(() -> controllerManagement.findOrRegisterTargetIfItDoesNotExist("", LOCALHOST));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("register target with empty controllerId should fail")
                .isThrownBy(() -> controllerManagement.findOrRegisterTargetIfItDoesNotExist(" ", LOCALHOST));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("register target with too long controllerId should fail")
                .isThrownBy(() -> controllerManagement.findOrRegisterTargetIfItDoesNotExist(
                        RandomStringUtils.randomAlphabetic(Target.CONTROLLER_ID_MAX_SIZE + 1), LOCALHOST));
    }

    @Test
    @Description("Register a controller which does not exist, when a ConcurrencyFailureException is raised, the "
            + "exception is rethrown after max retries")
    void findOrRegisterTargetIfItDoesNotExistThrowsExceptionAfterMaxRetries() {
        final TargetRepository mockTargetRepository = Mockito.mock(TargetRepository.class);
        when(mockTargetRepository.findOne(any())).thenThrow(ConcurrencyFailureException.class);
        ((JpaControllerManagement) controllerManagement).setTargetRepository(mockTargetRepository);

        try {
            assertThatExceptionOfType(ConcurrencyFailureException.class)
                    .as("Expected an ConcurrencyFailureException to be thrown!")
                    .isThrownBy(() -> controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST));

            verify(mockTargetRepository, times(TX_RT_MAX)).findOne(any());
        } finally {
            // revert
            ((JpaControllerManagement) controllerManagement).setTargetRepository(targetRepository);
        }
    }

    @Test
    @Description("Register a controller which does not exist, when a ConcurrencyFailureException is raised, the "
            + "exception is not rethrown when the max retries are not yet reached")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1) })
    void findOrRegisterTargetIfItDoesNotExistDoesNotThrowExceptionBeforeMaxRetries() {

        final TargetRepository mockTargetRepository = Mockito.mock(TargetRepository.class);
        ((JpaControllerManagement) controllerManagement).setTargetRepository(mockTargetRepository);
        final Target target = testdataFactory.createTarget();

        when(mockTargetRepository.findOne(any())).thenThrow(ConcurrencyFailureException.class)
                .thenThrow(ConcurrencyFailureException.class).thenReturn(Optional.of((JpaTarget) target));
        when(mockTargetRepository.save(any())).thenReturn(target);

        try {
            final Target targetFromControllerManagement = controllerManagement
                    .findOrRegisterTargetIfItDoesNotExist(target.getControllerId(), LOCALHOST);
            verify(mockTargetRepository, times(3)).findOne(any());
            verify(mockTargetRepository, times(1)).save(any());
            assertThat(target).isEqualTo(targetFromControllerManagement);
        } finally {
            // revert
            ((JpaControllerManagement) controllerManagement).setTargetRepository(targetRepository);
        }
    }

    @Test
    @Description("Register a controller which does not exist, then update the controller twice, first time by providing a name property and second time without a new name")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 3), @Expect(type = TargetUpdatedEvent.class, count = 1) })
    void findOrRegisterTargetIfItDoesNotExistDoesUpdateNameOnExistingTargetProperly() {

        final String controllerId = "12345";
        final String targetName = "UpdatedName";

        final Target newTarget = controllerManagement.findOrRegisterTargetIfItDoesNotExist(controllerId, LOCALHOST);
        assertThat(newTarget.getName()).isEqualTo(controllerId);

        final Target firstTimeUpdatedTarget = controllerManagement.findOrRegisterTargetIfItDoesNotExist(controllerId,
                LOCALHOST, targetName, null);
        assertThat(firstTimeUpdatedTarget.getName()).isEqualTo(targetName);

        // Name should not change to default (name=targetId) if target is
        // updated without new name provided
        final Target secondTimeUpdatedTarget = controllerManagement.findOrRegisterTargetIfItDoesNotExist(controllerId,
                LOCALHOST);
        assertThat(secondTimeUpdatedTarget.getName()).isEqualTo(targetName);
    }

    @Test
    @Description("Register a controller which does not exist, if a EntityAlreadyExistsException is raised, the "
            + "exception is rethrown and no further retries will be attempted")
    void findOrRegisterTargetIfItDoesNotExistDoesntRetryWhenEntityAlreadyExistsException() {

        final TargetRepository mockTargetRepository = Mockito.mock(TargetRepository.class);
        ((JpaControllerManagement) controllerManagement).setTargetRepository(mockTargetRepository);

        when(mockTargetRepository.findOne(any())).thenReturn(Optional.empty());
        when(mockTargetRepository.save(any())).thenThrow(EntityAlreadyExistsException.class);

        try {
            assertThatExceptionOfType(EntityAlreadyExistsException.class)
                    .as("Expected an EntityAlreadyExistsException to be thrown!")
                    .isThrownBy(() -> controllerManagement.findOrRegisterTargetIfItDoesNotExist("1234", LOCALHOST));
            verify(mockTargetRepository, times(1)).findOne(any());
            verify(mockTargetRepository, times(1)).save(any());
        } finally {
            // revert
            ((JpaControllerManagement) controllerManagement).setTargetRepository(targetRepository);
        }
    }

    @Test
    @Description("Retry is aborted when an unchecked exception is thrown and the exception should also be "
            + "rethrown")
    void recoverFindOrRegisterTargetIfItDoesNotExistIsNotInvokedForOtherExceptions() {

        final TargetRepository mockTargetRepository = Mockito.mock(TargetRepository.class);
        ((JpaControllerManagement) controllerManagement).setTargetRepository(mockTargetRepository);

        when(mockTargetRepository.findOne(any())).thenThrow(RuntimeException.class);

        try {
            assertThatExceptionOfType(RuntimeException.class).as("Expected a RuntimeException to be thrown!")
                    .isThrownBy(() -> controllerManagement.findOrRegisterTargetIfItDoesNotExist("aControllerId",
                            LOCALHOST));
            verify(mockTargetRepository, times(1)).findOne(any());
        } finally {
            // revert
            ((JpaControllerManagement) controllerManagement).setTargetRepository(targetRepository);
        }
    }

    @Test
    @Description("Verify that targetVisible metadata is returned from repository")
    @ExpectEvents({ @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 6) })
    void findTargetVisibleMetaDataBySoftwareModuleId() {
        final DistributionSet set = testdataFactory.createDistributionSet();
        testdataFactory.addSoftwareModuleMetadata(set);

        final Map<Long, List<SoftwareModuleMetadata>> result = controllerManagement
                .findTargetVisibleMetaDataBySoftwareModuleId(
                        set.getModules().stream().map(SoftwareModule::getId).collect(Collectors.toList()));

        assertThat(result).hasSize(3);
        result.forEach((key, value) -> assertThat(value).hasSize(1));
    }

    @Test
    @Description("Verify that controller registration does not result in a TargetPollEvent if feature is disabled")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 0) })
    void targetPollEventNotSendIfDisabled() {
        repositoryProperties.setPublishTargetPollEvent(false);
        controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST);
        repositoryProperties.setPublishTargetPollEvent(true);
    }

    @Test
    @Description("Controller tries to finish an update process after it has been finished by an error action status.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    void tryToFinishWithErrorUpdateProcessMoreThanOnce() {
        final Long actionId = createTargetAndAssignDs();

        // test and verify
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.RUNNING));
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, Action.Status.RUNNING,
                Action.Status.RUNNING, true);

        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.ERROR));
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.ERROR, Action.Status.ERROR,
                Action.Status.ERROR, false);

        // try with disabled late feedback
        repositoryProperties.setRejectActionStatusForClosedAction(true);
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.FINISHED));

        // test
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.ERROR, Action.Status.ERROR,
                Action.Status.ERROR, false);

        // try with enabled late feedback - should not make a difference as it
        // only allows intermediate feedback and not multiple close
        repositoryProperties.setRejectActionStatusForClosedAction(false);
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.FINISHED));

        // test
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.ERROR, Action.Status.ERROR,
                Action.Status.ERROR, false);

        assertThat(actionStatusRepository.count()).isEqualTo(3);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(3);

    }

    @Test
    @Description("Controller tries to finish an update process after it has been finished by an FINISHED action status.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1) })
    void tryToFinishUpdateProcessMoreThanOnce() {
        final Long actionId = prepareFinishedUpdate().getId();

        // try with disabled late feedback
        repositoryProperties.setRejectActionStatusForClosedAction(true);
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.FINISHED));

        // test
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.IN_SYNC, Action.Status.FINISHED,
                Action.Status.FINISHED, false);

        // try with enabled late feedback - should not make a difference as it
        // only allows intermediate feedback and not multiple close
        repositoryProperties.setRejectActionStatusForClosedAction(false);
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.FINISHED));

        // test
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.IN_SYNC, Action.Status.FINISHED,
                Action.Status.FINISHED, false);

        assertThat(actionStatusRepository.count()).isEqualTo(3);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(3);

    }

    @Test
    @Description("Controller tries to send an update feedback after it has been finished which is reject as the repository is "
            + "configured to reject that.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1) })
    void sendUpdatesForFinishUpdateProcessDroppedIfDisabled() {
        repositoryProperties.setRejectActionStatusForClosedAction(true);

        final Action action = prepareFinishedUpdate();

        controllerManagement.addUpdateActionStatus(
                entityFactory.actionStatus().create(action.getId()).status(Action.Status.RUNNING));

        // nothing changed as "feedback after close" is disabled
        assertThat(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.IN_SYNC);

        assertThat(actionRepository.findById(action.getId()))
                .hasValueSatisfying(a -> assertThat(a.getStatus()).isEqualTo(Status.FINISHED));
        assertThat(actionStatusRepository.count()).isEqualTo(3);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, action.getId()).getNumberOfElements())
                .isEqualTo(3);
    }

    @Test
    @Description("Controller tries to send an update feedback after it has been finished which is accepted as the repository is "
            + "configured to accept them.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1) })
    void sendUpdatesForFinishUpdateProcessAcceptedIfEnabled() {
        repositoryProperties.setRejectActionStatusForClosedAction(false);

        Action action = prepareFinishedUpdate();
        action = controllerManagement.addUpdateActionStatus(
                entityFactory.actionStatus().create(action.getId()).status(Action.Status.RUNNING));

        // nothing changed as "feedback after close" is disabled
        assertThat(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.IN_SYNC);

        // however, additional action status has been stored
        assertThat(actionStatusRepository.findAll(PAGE).getNumberOfElements()).isEqualTo(4);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, action.getId()).getNumberOfElements())
                .isEqualTo(4);
    }

    @Test
    @Description("Ensures that target attribute update is reflected by the repository.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 3) })
    void updateTargetAttributes() throws Exception {
        final String controllerId = "test123";
        final Target target = testdataFactory.createTarget(controllerId);

        SecurityContextSwitch.runAs(SecurityContextSwitch.withController(
                "controller",
                CONTROLLER_ROLE_ANONYMOUS, SpPermission.READ_TARGET), () -> {
            addAttributeAndVerify(controllerId);
            addSecondAttributeAndVerify(controllerId);
            updateAttributeAndVerify(controllerId);
            return null;
        });

        // verify that audit information has not changed
        final Target targetVerify = targetManagement.getByControllerID(controllerId).get();
        assertThat(targetVerify.getCreatedBy()).isEqualTo(target.getCreatedBy());
        assertThat(targetVerify.getCreatedAt()).isEqualTo(target.getCreatedAt());
        assertThat(targetVerify.getLastModifiedBy()).isEqualTo(target.getLastModifiedBy());
        assertThat(targetVerify.getLastModifiedAt()).isEqualTo(target.getLastModifiedAt());
    }

    @Step
    private void addAttributeAndVerify(final String controllerId) {
        final Map<String, String> testData = new HashMap<>(1);
        testData.put("test1", "testdata1");
        controllerManagement.updateControllerAttributes(controllerId, testData, null);

        assertThat(targetManagement.getControllerAttributes(controllerId)).as("Controller Attributes are wrong")
                .isEqualTo(testData);
    }

    @Step
    private void addSecondAttributeAndVerify(final String controllerId) {
        final Map<String, String> testData = new HashMap<>(2);
        testData.put("test2", "testdata20");
        controllerManagement.updateControllerAttributes(controllerId, testData, null);

        testData.put("test1", "testdata1");
        assertThat(targetManagement.getControllerAttributes(controllerId)).as("Controller Attributes are wrong")
                .isEqualTo(testData);
    }

    @Step
    private void updateAttributeAndVerify(final String controllerId) {
        final Map<String, String> testData = new HashMap<>(2);
        testData.put("test1", "testdata12");

        controllerManagement.updateControllerAttributes(controllerId, testData, null);

        testData.put("test2", "testdata20");
        assertThat(targetManagement.getControllerAttributes(controllerId)).as("Controller Attributes are wrong")
                .isEqualTo(testData);
    }

    @Test
    @Description("Ensures that target attributes can be updated using different update modes.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 4) })
    void updateTargetAttributesWithDifferentUpdateModes() {

        final String controllerId = "testCtrl";
        testdataFactory.createTarget(controllerId);

        // no update mode
        updateTargetAttributesWithoutUpdateMode(controllerId);

        // update mode REPLACE
        updateTargetAttributesWithUpdateModeReplace(controllerId);

        // update mode MERGE
        updateTargetAttributesWithUpdateModeMerge(controllerId);

        // update mode REMOVE
        updateTargetAttributesWithUpdateModeRemove(controllerId);

    }

    @Step
    private void updateTargetAttributesWithUpdateModeRemove(final String controllerId) {

        final int previousSize = targetManagement.getControllerAttributes(controllerId).size();

        // update the attributes using update mode REMOVE
        final Map<String, String> removeAttributes = new HashMap<>();
        removeAttributes.put("k1", "foo");
        removeAttributes.put("k3", "bar");
        controllerManagement.updateControllerAttributes(controllerId, removeAttributes, UpdateMode.REMOVE);

        // verify attribute removal
        final Map<String, String> updatedAttributes = targetManagement.getControllerAttributes(controllerId);
        assertThat(updatedAttributes.size()).isEqualTo(previousSize - 2);
        assertThat(updatedAttributes).doesNotContainKeys("k1", "k3");

    }

    @Step
    private void updateTargetAttributesWithUpdateModeMerge(final String controllerId) {
        // get the current attributes
        final HashMap<String, String> attributes = new HashMap<>(
                targetManagement.getControllerAttributes(controllerId));

        // update the attributes using update mode MERGE
        final Map<String, String> mergeAttributes = new HashMap<>();
        mergeAttributes.put("k1", "v1_modified_again");
        mergeAttributes.put("k4", "v4");
        controllerManagement.updateControllerAttributes(controllerId, mergeAttributes, UpdateMode.MERGE);

        // verify attribute merge
        final Map<String, String> updatedAttributes = targetManagement.getControllerAttributes(controllerId);
        assertThat(updatedAttributes.size()).isEqualTo(4);
        assertThat(updatedAttributes).containsAllEntriesOf(mergeAttributes);
        assertThat(updatedAttributes.get("k1")).isEqualTo("v1_modified_again");
        attributes.keySet().forEach(assertThat(updatedAttributes)::containsKey);
    }

    @Step
    private void updateTargetAttributesWithUpdateModeReplace(final String controllerId) {

        // get the current attributes
        final HashMap<String, String> attributes = new HashMap<>(
                targetManagement.getControllerAttributes(controllerId));

        // update the attributes using update mode REPLACE
        final Map<String, String> replacementAttributes = new HashMap<>();
        replacementAttributes.put("k1", "v1_modified");
        replacementAttributes.put("k2", "v2");
        replacementAttributes.put("k3", "v3");
        controllerManagement.updateControllerAttributes(controllerId, replacementAttributes, UpdateMode.REPLACE);

        // verify attribute replacement
        final Map<String, String> updatedAttributes = targetManagement.getControllerAttributes(controllerId);
        assertThat(updatedAttributes.size()).isEqualTo(replacementAttributes.size());
        assertThat(updatedAttributes).containsAllEntriesOf(replacementAttributes);
        assertThat(updatedAttributes.get("k1")).isEqualTo("v1_modified");
        attributes.entrySet().forEach(assertThat(updatedAttributes)::doesNotContain);
    }

    @Step
    private void updateTargetAttributesWithoutUpdateMode(final String controllerId) {

        // set the initial attributes
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("k0", "v0");
        attributes.put("k1", "v1");
        controllerManagement.updateControllerAttributes(controllerId, attributes, null);

        // verify initial attributes
        final Map<String, String> updatedAttributes = targetManagement.getControllerAttributes(controllerId);
        assertThat(updatedAttributes.size()).isEqualTo(attributes.size());
        assertThat(updatedAttributes).containsAllEntriesOf(attributes);
    }

    @Test
    @Description("Ensures that target attribute update fails if quota hits.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2) })
    public void updateTargetAttributesFailsIfTooManyEntries() throws Exception {
        final String controllerId = "test123";
        final int allowedAttributes = quotaManagement.getMaxAttributeEntriesPerTarget();
        testdataFactory.createTarget(controllerId);

        assertThatExceptionOfType(AssignmentQuotaExceededException.class).isThrownBy(() -> SecurityContextSwitch
                .runAs(SecurityContextSwitch.withController("controller", CONTROLLER_ROLE_ANONYMOUS), () -> {
                    writeAttributes(controllerId, allowedAttributes + 1, "key", "value");
                    return null;
                })).withMessageContaining("" + allowedAttributes);

        // verify that no attributes have been written
        assertThat(targetManagement.getControllerAttributes(controllerId)).isEmpty();

        // Write allowed number of attributes twice with same key should result
        // in update but work
        SecurityContextSwitch.runAs(SecurityContextSwitch.withController("controller", CONTROLLER_ROLE_ANONYMOUS), () -> {
            writeAttributes(controllerId, allowedAttributes, "key", "value1");
            writeAttributes(controllerId, allowedAttributes, "key", "value2");
            return null;
        });
        assertThat(targetManagement.getControllerAttributes(controllerId)).hasSize(10);

        // Now rite one more
        assertThatExceptionOfType(AssignmentQuotaExceededException.class).isThrownBy(() -> SecurityContextSwitch
                .runAs(SecurityContextSwitch.withController("controller", CONTROLLER_ROLE_ANONYMOUS), () -> {
                    writeAttributes(controllerId, 1, "additional", "value1");
                    return null;
                })).withMessageContaining("" + allowedAttributes);
        assertThat(targetManagement.getControllerAttributes(controllerId)).hasSize(10);

    }

    private void writeAttributes(final String controllerId, final int allowedAttributes, final String keyPrefix,
            final String valuePrefix) {
        final Map<String, String> testData = new HashMap<>(allowedAttributes);
        for (int i = 0; i < allowedAttributes; i++) {
            testData.put(keyPrefix + i, valuePrefix);
        }
        controllerManagement.updateControllerAttributes(controllerId, testData, null);
    }

    @Test
    @Description("Checks if invalid values of attribute-key and attribute-value are handled correctly")
    public void updateTargetAttributesFailsForInvalidAttributes() {
        final String controllerId = "targetId123";
        testdataFactory.createTarget(controllerId);

        assertThatExceptionOfType(InvalidTargetAttributeException.class)
                .as("Attribute with key too long should not be created")
                .isThrownBy(() -> controllerManagement.updateControllerAttributes(controllerId,
                        Collections.singletonMap(TargetTestData.ATTRIBUTE_KEY_TOO_LONG, TargetTestData.ATTRIBUTE_VALUE_VALID), null));

        assertThatExceptionOfType(InvalidTargetAttributeException.class)
                .as("Attribute with key too long and value too long should not be created")
                .isThrownBy(() -> controllerManagement.updateControllerAttributes(controllerId,
                        Collections.singletonMap(TargetTestData.ATTRIBUTE_KEY_TOO_LONG, TargetTestData.ATTRIBUTE_VALUE_TOO_LONG), null));

        assertThatExceptionOfType(InvalidTargetAttributeException.class)
                .as("Attribute with value too long should not be created")
                .isThrownBy(() -> controllerManagement.updateControllerAttributes(controllerId,
                        Collections.singletonMap(TargetTestData.ATTRIBUTE_KEY_VALID, TargetTestData.ATTRIBUTE_VALUE_TOO_LONG), null));

        assertThatExceptionOfType(InvalidTargetAttributeException.class)
                .as("Attribute with key NULL should not be created").isThrownBy(() -> controllerManagement
                        .updateControllerAttributes(controllerId,
                                Collections.singletonMap(null, TargetTestData.ATTRIBUTE_VALUE_VALID), null));
    }

    @Test
    @Description("Controller providing status entries fails if providing more than permitted by quota.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    public void controllerProvidesIntermediateFeedbackFailsIfQuotaHit() {
        final int allowStatusEntries = 10;
        final Long actionId = createTargetAndAssignDs();

        // Fails as one entry is already in there from the assignment
        assertThatExceptionOfType(AssignmentQuotaExceededException.class).isThrownBy(() -> SecurityContextSwitch
                .runAs(SecurityContextSwitch.withController("controller", CONTROLLER_ROLE_ANONYMOUS), () -> {
                    writeStatus(actionId, allowStatusEntries);
                    return null;
                })).withMessageContaining("" + allowStatusEntries);

    }

    private void writeStatus(final Long actionId, final int allowedStatusEntries) {
        for (int i = 0; i < allowedStatusEntries; i++) {
            controllerManagement.addInformationalActionStatus(
                    entityFactory.actionStatus().create(actionId).status(Status.RUNNING).message("test" + i));
        }
    }

    @Test
    @Description("Test to verify the storage and retrieval of action history.")
    public void findMessagesByActionStatusId() {
        final DistributionSet testDs = testdataFactory.createDistributionSet("1");
        final List<Target> testTarget = testdataFactory.createTargets(1);

        final Long actionId = getFirstAssignedActionId(assignDistributionSet(testDs, testTarget));

        controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(actionId)
                .status(Action.Status.RUNNING).occurredAt(System.currentTimeMillis())
                .messages(List.of("proceeding message 1")));

        final long createTime = System.currentTimeMillis();
        waitNextMillis();
        controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(actionId)
                .status(Action.Status.RUNNING).occurredAt(System.currentTimeMillis())
                .messages(List.of("proceeding message 2")));

        final List<String> messages = controllerManagement.getActionHistoryMessages(actionId, 2);

        assertThat(deploymentManagement.findActionStatusByAction(PAGE, actionId).getTotalElements())
                .as("Two action-states in total").isEqualTo(3L);
        assertThat(messages.get(0)).as("Message of action-status").isEqualTo("proceeding message 2");
        assertThat(messages.get(1)).as("Message of action-status").isEqualTo("proceeding message 1");
    }

    @Test
    @Description("Verifies that the quota specifying the maximum number of status entries per action is enforced.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 2),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 6),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 2), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 6), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 2), @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 2) })
    public void addActionStatusUpdatesUntilQuotaIsExceeded() {

        // any distribution set assignment causes 1 status entity to be created
        final int maxStatusEntries = quotaManagement.getMaxStatusEntriesPerAction() - 1;

        // test for informational status
        final Long actionId1 = getFirstAssignedActionId(assignDistributionSet(
                testdataFactory.createDistributionSet("ds1"), testdataFactory.createTargets(1, "t1")));
        assertThat(actionId1).isNotNull();
        for (int i = 0; i < maxStatusEntries; ++i) {
            controllerManagement.addInformationalActionStatus(entityFactory.actionStatus().create(actionId1)
                    .status(Status.WARNING).message("Msg " + i).occurredAt(System.currentTimeMillis()));
        }
        assertThatExceptionOfType(AssignmentQuotaExceededException.class).isThrownBy(() -> controllerManagement
                .addInformationalActionStatus(entityFactory.actionStatus().create(actionId1).status(Status.WARNING)));

        // test for update status (and mixed case)
        final Long actionId2 = getFirstAssignedActionId(assignDistributionSet(
                testdataFactory.createDistributionSet("ds2"), testdataFactory.createTargets(1, "t2")));
        assertThat(actionId2).isNotEqualTo(actionId1);
        for (int i = 0; i < maxStatusEntries; ++i) {
            controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(actionId2)
                    .status(Status.WARNING).message("Msg " + i).occurredAt(System.currentTimeMillis()));
        }
        assertThatExceptionOfType(AssignmentQuotaExceededException.class).isThrownBy(() -> controllerManagement
                .addInformationalActionStatus(entityFactory.actionStatus().create(actionId2).status(Status.WARNING)));

    }

    @Test
    @Description("Verifies that the quota specifying the maximum number of messages per action status is enforced.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    public void createActionStatusWithTooManyMessages() {

        final int maxMessages = quotaManagement.getMaxMessagesPerActionStatus();

        final Long actionId = getFirstAssignedActionId(
                assignDistributionSet(testdataFactory.createDistributionSet("ds1"), testdataFactory.createTargets(1)));
        assertThat(actionId).isNotNull();

        final List<String> messages = new ArrayList<>();
        IntStream.range(0, maxMessages).forEach(i -> messages.add(i, "msg"));

        assertThat(controllerManagement.addInformationalActionStatus(
                entityFactory.actionStatus().create(actionId).messages(messages).status(Status.WARNING))).isNotNull();

        messages.add("msg");
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> controllerManagement.addInformationalActionStatus(
                        entityFactory.actionStatus().create(actionId).messages(messages).status(Status.WARNING)));

    }

    @Test
    @Description("Verifies that a DOWNLOAD_ONLY action is not marked complete when the controller reports DOWNLOAD")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    public void controllerReportsDownloadForDownloadOnlyAction() {
        testdataFactory.createTarget();
        final Long actionId = createAndAssignDsAsDownloadOnly("downloadOnlyDs", DEFAULT_CONTROLLER_ID);
        assertThat(actionId).isNotNull();
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Status.DOWNLOAD));
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, Action.Status.RUNNING,
                Action.Status.DOWNLOAD, true);

        assertThat(actionStatusRepository.count()).isEqualTo(2);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(2);
        assertThat(activeActionExistsForControllerId(DEFAULT_CONTROLLER_ID)).isEqualTo(true);
    }

    @Test
    @Description("Verifies that a DOWNLOAD_ONLY action is marked complete once the controller reports DOWNLOADED")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    void controllerReportsDownloadedForDownloadOnlyAction() {
        testdataFactory.createTarget();
        final Long actionId = createAndAssignDsAsDownloadOnly("downloadOnlyDs", DEFAULT_CONTROLLER_ID);
        assertThat(actionId).isNotNull();
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Status.DOWNLOADED));
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.IN_SYNC, Action.Status.DOWNLOADED,
                Action.Status.DOWNLOADED, false);

        assertThat(actionStatusRepository.count()).isEqualTo(2);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(2);
        assertThat(activeActionExistsForControllerId(DEFAULT_CONTROLLER_ID)).isEqualTo(false);
    }

    @Test
    @Description("Verifies that a controller can report a FINISHED event for a DOWNLOAD_ONLY non-active action.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 3),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 2),
            @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    void controllerReportsActionFinishedForDownloadOnlyActionThatIsNotActive() {
        testdataFactory.createTarget();
        final Long actionId = createAndAssignDsAsDownloadOnly("downloadOnlyDs", DEFAULT_CONTROLLER_ID);
        assertThat(actionId).isNotNull();
        finishDownloadOnlyUpdateAndSendUpdateActionStatus(actionId, Status.FINISHED);

        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.IN_SYNC, Action.Status.FINISHED,
                Action.Status.FINISHED, false);

        assertThat(actionStatusRepository.count()).isEqualTo(3);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(3);
        assertThat(activeActionExistsForControllerId(DEFAULT_CONTROLLER_ID)).isEqualTo(false);
    }

    @Test
    @Description("Verifies that multiple DOWNLOADED events for a DOWNLOAD_ONLY action are handled.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 3),
            @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    void controllerReportsMultipleDownloadedForDownloadOnlyAction() {
        testdataFactory.createTarget();
        final Long actionId = createAndAssignDsAsDownloadOnly("downloadOnlyDs", DEFAULT_CONTROLLER_ID);
        assertThat(actionId).isNotNull();
        IntStream.range(0, 3).forEach(i -> controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Status.DOWNLOADED)));

        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.IN_SYNC, Status.DOWNLOADED,
                Status.DOWNLOADED, false);

        assertThat(actionStatusRepository.count()).isEqualTo(4);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(4);
        assertThat(activeActionExistsForControllerId(DEFAULT_CONTROLLER_ID)).isEqualTo(false);
    }

    @Test
    @Description("Verifies that quota is asserted when a controller reports too many DOWNLOADED events for a "
            + "DOWNLOAD_ONLY action.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 9),
            @Expect(type = ActionUpdatedEvent.class, count = 1) })
    void quotaExceptionWhencontrollerReportsTooManyDownloadedMessagesForDownloadOnlyAction() {
        final int maxMessages = quotaManagement.getMaxMessagesPerActionStatus();
        testdataFactory.createTarget();
        final Long actionId = createAndAssignDsAsDownloadOnly("downloadOnlyDs", DEFAULT_CONTROLLER_ID);
        assertThat(actionId).isNotNull();

        Assertions.assertThatExceptionOfType(AssignmentQuotaExceededException.class).isThrownBy(() ->
                IntStream.range(0, maxMessages).forEach(i -> controllerManagement
                        .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Status.DOWNLOADED))));
    }

    @Test
    @Description("Verifies that quota is enforced for UpdateActionStatus events for DOWNLOAD_ONLY assignments.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 9),
            @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    void quotaExceededExceptionWhenControllerReportsTooManyUpdateActionStatusMessagesForDownloadOnlyAction() {
        final int maxMessages = quotaManagement.getMaxMessagesPerActionStatus();
        testdataFactory.createTarget();
        final Long actionId = createAndAssignDsAsDownloadOnly("downloadOnlyDs", DEFAULT_CONTROLLER_ID);
        assertThat(actionId).isNotNull();

        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .as("No QuotaExceededException thrown for too many DOWNLOADED updateActionStatus updates").isThrownBy(
                        () -> IntStream.range(0, maxMessages).forEach(i -> controllerManagement.addUpdateActionStatus(
                                entityFactory.actionStatus().create(actionId).status(Status.DOWNLOADED))));

        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .as("No QuotaExceededException thrown for too many ERROR updateActionStatus updates")
                .isThrownBy(() -> IntStream.range(0, maxMessages).forEach(i -> controllerManagement
                        .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Status.ERROR))));

        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .as("No QuotaExceededException thrown for too many FINISHED updateActionStatus updates")
                .isThrownBy(() -> IntStream.range(0, maxMessages).forEach(i -> controllerManagement
                        .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Status.FINISHED))));
    }

    @Test
    @Description("Verifies that quota is enforced for UpdateActionStatus events for FORCED assignments.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    void quotaExceededExceptionWhenControllerReportsTooManyUpdateActionStatusMessagesForForced() {
        final int maxMessages = quotaManagement.getMaxMessagesPerActionStatus();
        final Long actionId = createTargetAndAssignDs();
        assertThat(actionId).isNotNull();

        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .as("No QuotaExceededException thrown for too many DOWNLOADED updateActionStatus updates").isThrownBy(
                        () -> IntStream.range(0, maxMessages).forEach(i -> controllerManagement.addUpdateActionStatus(
                                entityFactory.actionStatus().create(actionId).status(Status.DOWNLOADED))));

        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .as("No QuotaExceededException thrown for too many ERROR updateActionStatus updates")
                .isThrownBy(() -> IntStream.range(0, maxMessages).forEach(i -> controllerManagement
                        .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Status.ERROR))));

        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .as("No QuotaExceededException thrown for too many FINISHED updateActionStatus updates")
                .isThrownBy(() -> IntStream.range(0, maxMessages).forEach(i -> controllerManagement
                        .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Status.FINISHED))));
    }

    @Test
    @Description("Verify that the attaching externalRef to an action is properly stored")
    void updatedExternalRefOnActionIsReallyUpdated() {
        final List<String> allExternalRef = new ArrayList<>();
        final List<Long> allActionId = new ArrayList<>();
        final int numberOfActions = 3;
        final DistributionSet knownDistributionSet = testdataFactory.createDistributionSet();
        for (int i = 0; i < numberOfActions; i++) {
            final String knownControllerId = "controllerId" + i;
            final String knownExternalRef = "externalRefId" + i;

            testdataFactory.createTarget(knownControllerId);
            final DistributionSetAssignmentResult assignmentResult = assignDistributionSet(knownDistributionSet.getId(),
                    knownControllerId);
            final Long actionId = getFirstAssignedActionId(assignmentResult);
            controllerManagement.updateActionExternalRef(actionId, knownExternalRef);

            allExternalRef.add(knownExternalRef);
            allActionId.add(actionId);
        }

        final List<Action> foundAction = actionRepository.findAll((root, query, cb) -> cb.and(
                   root.get(JpaAction_.externalRef).in(allExternalRef),
                   cb.equal(root.get(JpaAction_.active), true)
           )).stream().map(Action.class::cast).toList();
        assertThat(foundAction).isNotNull();
        for (int i = 0; i < numberOfActions; i++) {
            assertThat(foundAction.get(i).getId()).isEqualTo(allActionId.get(i));
        }
    }

    @Test
    @Description("Verify that getting a single action using externalRef works")
    void getActionUsingSingleExternalRef() {

        final String knownControllerId = "controllerId";
        final String knownExternalRef = "externalRefId";
        final DistributionSet knownDistributionSet = testdataFactory.createDistributionSet();

        // GIVEN
        testdataFactory.createTarget(knownControllerId);
        final DistributionSetAssignmentResult assignmentResult = assignDistributionSet(knownDistributionSet.getId(),
                knownControllerId);
        final Long actionId = getFirstAssignedActionId(assignmentResult);
        controllerManagement.updateActionExternalRef(actionId, knownExternalRef);

        // WHEN
        final Optional<Action> foundAction = controllerManagement.getActionByExternalRef(knownExternalRef);

        // THEN
        assertThat(foundAction).isPresent();
        assertThat(foundAction.get().getId()).isEqualTo(actionId);
    }
    @Test
    @Description("Verify that assigning version form target works")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3)}
          )
    void assignVersionToTarget() {

        final DistributionSet knownDistributionSet = testdataFactory.createDistributionSet();

        // GIVEN
        testdataFactory.createTarget(DEFAULT_CONTROLLER_ID).getId();

        // WHEN
        boolean updated1 = controllerManagement.updateOfflineAssignedVersion(DEFAULT_CONTROLLER_ID,
                knownDistributionSet.getName(),knownDistributionSet.getVersion());
        // if target is already assigned to a distribution then it shouldn't reassign the distribution
        boolean updated2 = controllerManagement.updateOfflineAssignedVersion(DEFAULT_CONTROLLER_ID,
                knownDistributionSet.getName(),knownDistributionSet.getVersion());

        // THEN
        assertAssignedDistributionSetId(DEFAULT_CONTROLLER_ID, knownDistributionSet.getId());
        assertInstalledDistributionSetId(DEFAULT_CONTROLLER_ID, knownDistributionSet.getId());
        assertThat(updated1).isTrue();
        assertThat(updated2).isFalse();
    }

    @Test
    @Description("Verify that a null externalRef cannot be assigned to an action")
    void externalRefCannotBeNull() {
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("No ConstraintViolationException thrown when a null externalRef was set on an action")
                .isThrownBy(() -> controllerManagement.updateActionExternalRef(1L, null));
    }

    @Test
    @Description("Verifies that a target can report FINISHED/ERROR updates for DOWNLOAD_ONLY assignments regardless of "
            + "repositoryProperties.rejectActionStatusForClosedAction value.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 4),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 12),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 4), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 12), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 4), @Expect(type = TargetUpdatedEvent.class, count = 12),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 4),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 6),
            @Expect(type = ActionUpdatedEvent.class, count = 8) })
    void targetCanAlwaysReportFinishedOrErrorAfterActionIsClosedForDownloadOnlyAssignments() {

        testdataFactory.createTarget();

        // allow actionStatusUpdates for closed actions
        repositoryProperties.setRejectActionStatusForClosedAction(false);

        final Long actionId = createAndAssignDsAsDownloadOnly("downloadOnlyDs1", DEFAULT_CONTROLLER_ID);
        assertThat(actionId).isNotNull();
        finishDownloadOnlyUpdateAndSendUpdateActionStatus(actionId, Status.FINISHED);

        final Long actionId2 = createAndAssignDsAsDownloadOnly("downloadOnlyDs2", DEFAULT_CONTROLLER_ID);
        assertThat(actionId).isNotNull();
        finishDownloadOnlyUpdateAndSendUpdateActionStatus(actionId2, Status.ERROR);

        // disallow actionStatusUpdates for closed actions
        repositoryProperties.setRejectActionStatusForClosedAction(true);

        final Long actionId3 = createAndAssignDsAsDownloadOnly("downloadOnlyDs3", DEFAULT_CONTROLLER_ID);
        assertThat(actionId).isNotNull();
        finishDownloadOnlyUpdateAndSendUpdateActionStatus(actionId3, Status.FINISHED);

        final Long actionId4 = createAndAssignDsAsDownloadOnly("downloadOnlyDs4", DEFAULT_CONTROLLER_ID);
        assertThat(actionId).isNotNull();
        finishDownloadOnlyUpdateAndSendUpdateActionStatus(actionId4, Status.ERROR);

        // actionStatusRepository should have 12 ActionStatusUpdates, 3 from
        // each action
        assertThat(actionStatusRepository.count()).isEqualTo(12L);
    }

    @Step
    private void finishDownloadOnlyUpdateAndSendUpdateActionStatus(final Long actionId, final Status status) {
        // finishing action
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Status.DOWNLOADED));

        controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(status));
        assertThat(activeActionExistsForControllerId(DEFAULT_CONTROLLER_ID)).isEqualTo(false);
    }

    @Test
    @Description("Verifies that a controller can report a FINISHED event for a DOWNLOAD_ONLY action after having"
            + " installed an intermediate update.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 6),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 2), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 6), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 2), @Expect(type = TargetUpdatedEvent.class, count = 5),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 3),
            @Expect(type = ActionUpdatedEvent.class, count = 3),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 2) })
    void controllerReportsFinishedForOldDownloadOnlyActionAfterSuccessfulForcedAssignment() {
        testdataFactory.createTarget();
        final DistributionSet downloadOnlyDs = testdataFactory.createDistributionSet("downloadOnlyDs1");

        // assign DOWNLOAD_ONLY Distribution set
        final Long downloadOnlyActionId = assignDs(downloadOnlyDs.getId(), DEFAULT_CONTROLLER_ID, DOWNLOAD_ONLY);
        addUpdateActionStatus(downloadOnlyActionId, DEFAULT_CONTROLLER_ID, Status.DOWNLOADED);
        assertAssignedDistributionSetId(DEFAULT_CONTROLLER_ID, downloadOnlyDs.getId());
        assertInstalledDistributionSetId(DEFAULT_CONTROLLER_ID, null);
        assertNoActiveActionsExistsForControllerId(DEFAULT_CONTROLLER_ID);

        // assign distributionSet as FORCED assignment
        final Long forcedDistributionSetId = testdataFactory.createDistributionSet("forcedDs1").getId();
        final DistributionSetAssignmentResult assignmentResult = assignDistributionSet(forcedDistributionSetId,
                DEFAULT_CONTROLLER_ID, Action.ActionType.SOFT);
        addUpdateActionStatus(getFirstAssignedActionId(assignmentResult), DEFAULT_CONTROLLER_ID, Status.FINISHED);
        assertAssignedDistributionSetId(DEFAULT_CONTROLLER_ID, forcedDistributionSetId);
        assertInstalledDistributionSetId(DEFAULT_CONTROLLER_ID, forcedDistributionSetId);
        assertNoActiveActionsExistsForControllerId(DEFAULT_CONTROLLER_ID);

        // report FINISHED for the DOWNLOAD_ONLY action
        addUpdateActionStatus(downloadOnlyActionId, DEFAULT_CONTROLLER_ID, Status.FINISHED);
        assertAssignedDistributionSetId(DEFAULT_CONTROLLER_ID, downloadOnlyDs.getId());
        assertInstalledDistributionSetId(DEFAULT_CONTROLLER_ID, downloadOnlyDs.getId());
        assertNoActiveActionsExistsForControllerId(DEFAULT_CONTROLLER_ID);
    }

    @Step
    private void addUpdateActionStatus(final Long actionId, final String controllerId, final Status actionStatus) {
        controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(actionStatus));
        assertActionStatus(actionId, controllerId, TargetUpdateStatus.IN_SYNC, actionStatus, actionStatus, false);
    }

    @Test
    @Description("Actions are exposed according to thier weight in multi assignment mode.")
    void actionsAreExposedAccordingToTheirWeight() {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long actionWeightNull = assignDistributionSet(ds.getId(), targetId).getAssignedEntity().get(0).getId();
        enableMultiAssignments();
        final Long actionWeight500old = assignDistributionSet(ds.getId(), targetId, 500).getAssignedEntity().get(0)
                .getId();
        final Long actionWeight500new = assignDistributionSet(ds.getId(), targetId, 500).getAssignedEntity().get(0)
                .getId();
        final Long actionWeight1000 = assignDistributionSet(ds.getId(), targetId, 1000).getAssignedEntity().get(0)
                .getId();

        assertThat(controllerManagement.findActiveActionWithHighestWeight(targetId).get().getId())
                .isEqualTo(actionWeightNull);
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionWeightNull).status(Status.FINISHED));
        assertThat(controllerManagement.findActiveActionWithHighestWeight(targetId).get().getId())
                .isEqualTo(actionWeight1000);
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionWeight1000).status(Status.FINISHED));
        assertThat(controllerManagement.findActiveActionWithHighestWeight(targetId).get().getId())
                .isEqualTo(actionWeight500old);
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionWeight500old).status(Status.FINISHED));
        assertThat(controllerManagement.findActiveActionWithHighestWeight(targetId).get().getId())
                .isEqualTo(actionWeight500new);
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionWeight500new).status(Status.FINISHED));
        assertThat(controllerManagement.findActiveActionWithHighestWeight(targetId)).isEmpty();
    }

    private void assertAssignedDistributionSetId(final String controllerId, final Long dsId) {
        final Optional<Target> target = controllerManagement.getByControllerId(controllerId);
        assertThat(target).isPresent();
        final DistributionSet assignedDistributionSet = ((JpaTarget) target.get()).getAssignedDistributionSet();
        assertThat(assignedDistributionSet.getId()).isEqualTo(dsId);
    }

    private void assertInstalledDistributionSetId(final String controllerId, final Long dsId) {
        final Optional<Target> target = controllerManagement.getByControllerId(controllerId);
        assertThat(target).isPresent();
        final DistributionSet installedDistributionSet = ((JpaTarget) target.get()).getInstalledDistributionSet();
        if (dsId == null) {
            assertThat(installedDistributionSet).isNull();
        } else {
            assertThat(installedDistributionSet.getId()).isEqualTo(dsId);
        }
    }

    @Test
    @Description("Delete a target on requested target deletion from client side")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1), @Expect(type = TargetDeletedEvent.class, count = 1) })
    void deleteTargetWithValidThingId() {
        final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST);
        assertThat(target).as("target should not be null").isNotNull();
        assertThat(targetRepository.count()).as("target exists and is ready for deletion").isEqualTo(1L);

        controllerManagement.deleteExistingTarget(target.getControllerId());

        assertThat(targetRepository.count()).as("target should not exist anymore").isEqualTo(0L);
    }

    @Test
    @Description("Delete a target with a non existing thingId")
    @ExpectEvents({ @Expect(type = TargetDeletedEvent.class, count = 0) })
    void deleteTargetWithInvalidThingId() {
        assertThatExceptionOfType(EntityNotFoundException.class)
                .as("No EntityNotFoundException thrown when deleting a non-existing target")
                .isThrownBy(() -> controllerManagement.deleteExistingTarget("BB"));
        assertThat(targetRepository.count()).as("target should not exist").isEqualTo(0L);
    }

    @Test
    @Description("Delete a target after it has been deleted already")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1), @Expect(type = TargetDeletedEvent.class, count = 1) })
    void deleteTargetAfterItWasDeleted() {
        final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST);
        assertThat(target).as("target should not be null").isNotNull();
        assertThat(targetRepository.count()).as("target exists and is ready for deletion").isEqualTo(1L);

        controllerManagement.deleteExistingTarget(target.getControllerId());
        assertThat(targetRepository.count()).as("target should not exist anymore").isEqualTo(0L);

        assertThatExceptionOfType(EntityNotFoundException.class)
                .as("No EntityNotFoundException thrown when deleting a non-existing target")
                .isThrownBy(() -> controllerManagement.deleteExistingTarget(target.getControllerId()));
    }

    @Test
    @Description("When action status code is provided in feedback it is also stored in the action field lastActionStatusCode")
    void lastActionStatusCodeIsSet() {
        final Long actionId = createTargetAndAssignDs();

        addUpdateActionStatusAndAssert(actionId, Action.Status.RUNNING, 10);
        assertLastActionStatusCodeInAction(actionId, 10);

        addUpdateActionStatusAndAssert(actionId, Action.Status.RUNNING);
        assertLastActionStatusCodeInAction(actionId, null);

        addUpdateActionStatusAndAssert(actionId, Action.Status.RUNNING, 20);
        assertLastActionStatusCodeInAction(actionId, 20);

    }

    private void assertLastActionStatusCodeInAction(final Long actionId, final Integer expectedLastActionStatusCode) {
        final Optional<Action> action = actionRepository.findWithDetailsById(actionId);
        assertThat(action).isPresent();
        assertThat(action.get().getLastActionStatusCode()).isEqualTo(Optional.ofNullable(expectedLastActionStatusCode));
    }

    private void assertNoActiveActionsExistsForControllerId(final String controllerId) {
        assertThat(activeActionExistsForControllerId(controllerId)).isEqualTo(false);
    }

    private boolean activeActionExistsForControllerId(final String controllerId) {
        return actionRepository.exists(ActionSpecifications.byTargetControllerIdAndActive(controllerId, true));
    }
}
