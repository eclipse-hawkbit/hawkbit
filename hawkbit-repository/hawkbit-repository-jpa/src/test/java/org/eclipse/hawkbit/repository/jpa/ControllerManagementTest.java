/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions.CONTROLLER_ROLE_ANONYMOUS;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.validation.ConstraintViolationException;

import org.apache.commons.lang3.RandomUtils;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.UpdateMode;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.CancelActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.InvalidTargetAttributeException;
import org.eclipse.hawkbit.repository.exception.QuotaExceededException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.eclipse.hawkbit.repository.test.util.WithSpringAuthorityRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("Controller Management")
public class ControllerManagementTest extends AbstractJpaIntegrationTest {

    @Autowired
    private RepositoryProperties repositoryProperties;

    @Test
    @Description("Verifies that management get access react as specfied on calls for non existing entities by means "
            + "of Optional not present.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 1) })
    public void nonExistingEntityAccessReturnsNotPresent() {
        final Target target = testdataFactory.createTarget();
        final SoftwareModule module = testdataFactory.createSoftwareModuleOs();

        assertThat(controllerManagement.findActionWithDetails(NOT_EXIST_IDL)).isNotPresent();
        assertThat(controllerManagement.getByControllerId(NOT_EXIST_ID)).isNotPresent();
        assertThat(controllerManagement.get(NOT_EXIST_IDL)).isNotPresent();
        assertThat(controllerManagement.getActionForDownloadByTargetAndSoftwareModule(target.getControllerId(),
                module.getId())).isNotPresent();

        assertThat(controllerManagement.findOldestActiveActionByTarget(NOT_EXIST_ID)).isNotPresent();

        assertThat(controllerManagement.hasTargetArtifactAssigned(target.getControllerId(), "XXX")).isFalse();
        assertThat(controllerManagement.hasTargetArtifactAssigned(target.getId(), "XXX")).isFalse();
    }

    @Test
    @Description("Verifies that management queries react as specfied on calls for non existing entities "
            + " by means of throwing EntityNotFoundException.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 1) })
    public void entityQueriesReferringToNotExistingEntitiesThrowsException() throws URISyntaxException {
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
                () -> controllerManagement.updateControllerAttributes(NOT_EXIST_ID, Maps.newHashMap(), null), "Target");
    }

    @Test
    @Description("Controller confirms successfull update with FINISHED status.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3) })
    public void controllerConfirmsUpdateWithFinished() {
        final Long actionId = createTargetAndAssignDs();

        simulateIntermediateStatusOnUpdate(actionId);

        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.FINISHED));
        assertActionStatus(actionId, TestdataFactory.DEFAULT_CONTROLLER_ID, TargetUpdateStatus.IN_SYNC,
                Action.Status.FINISHED, Action.Status.FINISHED, false);

        assertThat(actionStatusRepository.count()).isEqualTo(7);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(7);
    }

    @Test
    @Description("Controller confirmation failes with invalid messages.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3) })
    public void controllerConfirmationFailsWithInvalidMessages() {
        final Long actionId = createTargetAndAssignDs();

        simulateIntermediateStatusOnUpdate(actionId);

        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> controllerManagement.addUpdateActionStatus(entityFactory.actionStatus()
                        .create(actionId).status(Action.Status.FINISHED).message(INVALID_TEXT_HTML)))
                .as("set invalid description text should not be created");

        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> controllerManagement.addUpdateActionStatus(
                        entityFactory.actionStatus().create(actionId).status(Action.Status.FINISHED)
                                .messages(Arrays.asList("this is valid.", INVALID_TEXT_HTML))))
                .as("set invalid description text should not be created");

        assertThat(actionStatusRepository.count()).isEqualTo(6);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(6);
    }

    @Test
    @Description("Controller confirms successfull update with FINISHED status on a action that is on canceling. "
            + "Reason: The decission to ignore the cancellation is in fact up to the controller.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3) })
    public void controllerConfirmsUpdateWithFinishedAndIgnorsCancellationWithThat() {
        final Long actionId = createTargetAndAssignDs();
        deploymentManagement.cancelAction(actionId);

        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.FINISHED));
        assertActionStatus(actionId, TestdataFactory.DEFAULT_CONTROLLER_ID, TargetUpdateStatus.IN_SYNC,
                Action.Status.FINISHED, Action.Status.FINISHED, false);

        assertThat(actionStatusRepository.count()).isEqualTo(3);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(3);
    }

    @Test
    @Description("Update server rejects cancelation feedback if action is not in CANCELING state.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3) })
    public void cancellationFeedbackRejectedIfActionIsNotInCanceling() {
        final Long actionId = createTargetAndAssignDs();

        try {
            controllerManagement.addCancelActionStatus(
                    entityFactory.actionStatus().create(actionId).status(Action.Status.FINISHED));
            fail("Expected " + CancelActionNotAllowedException.class.getName());
        } catch (final CancelActionNotAllowedException e) {
            // expected
        }

        assertActionStatus(actionId, TestdataFactory.DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING,
                Action.Status.RUNNING, Action.Status.RUNNING, true);

        assertThat(actionStatusRepository.count()).isEqualTo(1);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(1);

    }

    @Test
    @Description("Controller confirms action cancelation with FINISHED status.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3) })
    public void controllerConfirmsActionCancelationWithFinished() {
        final Long actionId = createTargetAndAssignDs();

        deploymentManagement.cancelAction(actionId);
        assertActionStatus(actionId, TestdataFactory.DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING,
                Action.Status.CANCELING, Action.Status.CANCELING, true);

        simulateIntermediateStatusOnCancellation(actionId);

        controllerManagement
                .addCancelActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.FINISHED));
        assertActionStatus(actionId, TestdataFactory.DEFAULT_CONTROLLER_ID, TargetUpdateStatus.IN_SYNC,
                Action.Status.CANCELED, Action.Status.FINISHED, false);

        assertThat(actionStatusRepository.count()).isEqualTo(8);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(8);
    }

    @Test
    @Description("Controller confirms action cancelation with FINISHED status.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3) })
    public void controllerConfirmsActionCancelationWithCanceled() {
        final Long actionId = createTargetAndAssignDs();

        deploymentManagement.cancelAction(actionId);
        assertActionStatus(actionId, TestdataFactory.DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING,
                Action.Status.CANCELING, Action.Status.CANCELING, true);

        simulateIntermediateStatusOnCancellation(actionId);

        controllerManagement
                .addCancelActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.CANCELED));
        assertActionStatus(actionId, TestdataFactory.DEFAULT_CONTROLLER_ID, TargetUpdateStatus.IN_SYNC,
                Action.Status.CANCELED, Action.Status.CANCELED, false);

        assertThat(actionStatusRepository.count()).isEqualTo(8);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(8);
    }

    @Test
    @Description("Controller rejects action cancelation with CANCEL_REJECTED status. Action goes back to RUNNING status as it expects "
            + "that the controller will continue the original update.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3) })
    public void controllerRejectsActionCancelationWithReject() {
        final Long actionId = createTargetAndAssignDs();

        deploymentManagement.cancelAction(actionId);
        assertActionStatus(actionId, TestdataFactory.DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING,
                Action.Status.CANCELING, Action.Status.CANCELING, true);

        simulateIntermediateStatusOnCancellation(actionId);

        controllerManagement.addCancelActionStatus(
                entityFactory.actionStatus().create(actionId).status(Action.Status.CANCEL_REJECTED));
        assertActionStatus(actionId, TestdataFactory.DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING,
                Action.Status.RUNNING, Action.Status.CANCEL_REJECTED, true);

        assertThat(actionStatusRepository.count()).isEqualTo(8);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(8);
    }

    @Test
    @Description("Controller rejects action cancelation with ERROR status. Action goes back to RUNNING status as it expects "
            + "that the controller will continue the original update.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3) })
    public void controllerRejectsActionCancelationWithError() {
        final Long actionId = createTargetAndAssignDs();

        deploymentManagement.cancelAction(actionId);
        assertActionStatus(actionId, TestdataFactory.DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING,
                Action.Status.CANCELING, Action.Status.CANCELING, true);

        simulateIntermediateStatusOnCancellation(actionId);

        controllerManagement
                .addCancelActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.ERROR));
        assertActionStatus(actionId, TestdataFactory.DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING,
                Action.Status.RUNNING, Action.Status.ERROR, true);

        assertThat(actionStatusRepository.count()).isEqualTo(8);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(8);
    }

    @Step
    private Long createTargetAndAssignDs() {
        final Long dsId = testdataFactory.createDistributionSet().getId();
        testdataFactory.createTarget();
        assignDistributionSet(dsId, TestdataFactory.DEFAULT_CONTROLLER_ID);
        assertThat(targetManagement.getByControllerID(TestdataFactory.DEFAULT_CONTROLLER_ID).get().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.PENDING);

        return deploymentManagement.findActiveActionsByTarget(PAGE, TestdataFactory.DEFAULT_CONTROLLER_ID).getContent()
                .get(0).getId();
    }

    @Step
    private void simulateIntermediateStatusOnCancellation(final Long actionId) {
        controllerManagement
                .addCancelActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.RUNNING));
        assertActionStatus(actionId, TestdataFactory.DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING,
                Action.Status.CANCELING, Action.Status.RUNNING, true);

        controllerManagement
                .addCancelActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.DOWNLOAD));
        assertActionStatus(actionId, TestdataFactory.DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING,
                Action.Status.CANCELING, Action.Status.DOWNLOAD, true);

        controllerManagement
                .addCancelActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.DOWNLOADED));
        assertActionStatus(actionId, TestdataFactory.DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING,
                Action.Status.CANCELING, Action.Status.DOWNLOADED, true);

        controllerManagement
                .addCancelActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.RETRIEVED));
        assertActionStatus(actionId, TestdataFactory.DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING,
                Action.Status.CANCELING, Action.Status.RETRIEVED, true);

        controllerManagement
                .addCancelActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.WARNING));
        assertActionStatus(actionId, TestdataFactory.DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING,
                Action.Status.CANCELING, Action.Status.WARNING, true);
    }

    @Step
    private void simulateIntermediateStatusOnUpdate(final Long actionId) {
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.RUNNING));
        assertActionStatus(actionId, TestdataFactory.DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING,
                Action.Status.RUNNING, Action.Status.RUNNING, true);

        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.DOWNLOAD));
        assertActionStatus(actionId, TestdataFactory.DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING,
                Action.Status.RUNNING, Action.Status.DOWNLOAD, true);
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.DOWNLOADED));
        assertActionStatus(actionId, TestdataFactory.DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING,
                Action.Status.RUNNING, Action.Status.DOWNLOADED, true);

        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.RETRIEVED));
        assertActionStatus(actionId, TestdataFactory.DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING,
                Action.Status.RUNNING, Action.Status.RETRIEVED, true);

        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.WARNING));
        assertActionStatus(actionId, TestdataFactory.DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING,
                Action.Status.RUNNING, Action.Status.WARNING, true);
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
            assertThat(controllerManagement.findOldestActiveActionByTarget(controllerId).get().getId())
                    .isEqualTo(actionId);
        }
    }

    @Test
    @Description("Verifies that assignement verification works based on SHA1 hash. By design it is not important which artifact "
            + "is actually used for the check as long as they have an identical binary, i.e. same SHA1 hash. ")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 6),
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 2) })
    public void hasTargetArtifactAssignedIsTrueWithMultipleArtifacts() {
        final int artifactSize = 5 * 1024;
        final byte[] random = RandomUtils.nextBytes(artifactSize);

        final DistributionSet ds = testdataFactory.createDistributionSet("");
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2");
        Target savedTarget = testdataFactory.createTarget();

        // create two artifacts with identical SHA1 hash
        final Artifact artifact = artifactManagement.create(new ByteArrayInputStream(random),
                ds.findFirstModuleByType(osType).get().getId(), "file1", false, artifactSize);
        final Artifact artifact2 = artifactManagement.create(new ByteArrayInputStream(random),
                ds2.findFirstModuleByType(osType).get().getId(), "file1", false, artifactSize);
        assertThat(artifact.getSha1Hash()).isEqualTo(artifact2.getSha1Hash());

        assertThat(
                controllerManagement.hasTargetArtifactAssigned(savedTarget.getControllerId(), artifact.getSha1Hash()))
                        .isFalse();
        savedTarget = assignDistributionSet(ds.getId(), savedTarget.getControllerId()).getAssignedEntity().iterator()
                .next();
        assertThat(
                controllerManagement.hasTargetArtifactAssigned(savedTarget.getControllerId(), artifact.getSha1Hash()))
                        .isTrue();
        assertThat(
                controllerManagement.hasTargetArtifactAssigned(savedTarget.getControllerId(), artifact2.getSha1Hash()))
                        .isTrue();
    }

    @Test
    @Description("Register a controller which does not exist")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 2) })
    public void findOrRegisterTargetIfItDoesNotexist() {
        final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotexist("AA", LOCALHOST);
        assertThat(target).as("target should not be null").isNotNull();

        final Target sameTarget = controllerManagement.findOrRegisterTargetIfItDoesNotexist("AA", LOCALHOST);
        assertThat(target.getId()).as("Target should be the equals").isEqualTo(sameTarget.getId());
        assertThat(targetRepository.count()).as("Only 1 target should be registred").isEqualTo(1L);

        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> controllerManagement.findOrRegisterTargetIfItDoesNotexist("", LOCALHOST))
                .as("register target with empty controllerId should fail");
    }

    @Test
    @Description("Verify that targetVisible metadata is returned from repository")
    @ExpectEvents({ @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 6) })
    public void findTargetVisibleMetaDataBySoftwareModuleId() {
        final DistributionSet set = testdataFactory.createDistributionSet();
        testdataFactory.addSoftwareModuleMetadata(set);

        final Map<Long, List<SoftwareModuleMetadata>> result = controllerManagement
                .findTargetVisibleMetaDataBySoftwareModuleId(
                        set.getModules().stream().map(SoftwareModule::getId).collect(Collectors.toList()));

        assertThat(result).hasSize(3);
        result.entrySet().forEach(entry -> assertThat(entry.getValue()).hasSize(1));
    }

    @Test
    @Description("Verify that controller registration does not result in a TargetPollEvent if feature is disabled")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 0) })
    public void targetPollEventNotSendIfDisabled() {
        repositoryProperties.setPublishTargetPollEvent(false);
        controllerManagement.findOrRegisterTargetIfItDoesNotexist("AA", LOCALHOST);
        repositoryProperties.setPublishTargetPollEvent(true);
    }

    @Test
    @Description("Controller trys to finish an update process after it has been finished by an error action status.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3) })
    public void tryToFinishWithErrorUpdateProcessMoreThanOnce() {
        final Long actionId = createTargetAndAssignDs();

        // test and verify
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.RUNNING));
        assertActionStatus(actionId, TestdataFactory.DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING,
                Action.Status.RUNNING, Action.Status.RUNNING, true);

        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.ERROR));
        assertActionStatus(actionId, TestdataFactory.DEFAULT_CONTROLLER_ID, TargetUpdateStatus.ERROR,
                Action.Status.ERROR, Action.Status.ERROR, false);

        // try with disabled late feedback
        repositoryProperties.setRejectActionStatusForClosedAction(true);
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.FINISHED));

        // test
        assertActionStatus(actionId, TestdataFactory.DEFAULT_CONTROLLER_ID, TargetUpdateStatus.ERROR,
                Action.Status.ERROR, Action.Status.ERROR, false);

        // try with enabled late feedback - should not make a difference as it
        // only allows intermediate feedbacks and not multiple close
        repositoryProperties.setRejectActionStatusForClosedAction(false);
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.FINISHED));

        // test
        assertActionStatus(actionId, TestdataFactory.DEFAULT_CONTROLLER_ID, TargetUpdateStatus.ERROR,
                Action.Status.ERROR, Action.Status.ERROR, false);

        assertThat(actionStatusRepository.count()).isEqualTo(3);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(3);

    }

    @Test
    @Description("Controller trys to finish an update process after it has been finished by an FINISHED action status.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3) })
    public void tryToFinishUpdateProcessMoreThanOnce() {
        final Long actionId = prepareFinishedUpdate().getId();

        // try with disabled late feedback
        repositoryProperties.setRejectActionStatusForClosedAction(true);
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.FINISHED));

        // test
        assertActionStatus(actionId, TestdataFactory.DEFAULT_CONTROLLER_ID, TargetUpdateStatus.IN_SYNC,
                Action.Status.FINISHED, Action.Status.FINISHED, false);

        // try with enabled late feedback - should not make a difference as it
        // only allows intermediate feedbacks and not multiple close
        repositoryProperties.setRejectActionStatusForClosedAction(false);
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.FINISHED));

        // test
        assertActionStatus(actionId, TestdataFactory.DEFAULT_CONTROLLER_ID, TargetUpdateStatus.IN_SYNC,
                Action.Status.FINISHED, Action.Status.FINISHED, false);

        assertThat(actionStatusRepository.count()).isEqualTo(3);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(3);

    }

    @Test
    @Description("Controller trys to send an update feedback after it has been finished which is reject as the repository is "
            + "configured to reject that.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3) })
    public void sendUpdatesForFinishUpdateProcessDropedIfDisabled() {
        repositoryProperties.setRejectActionStatusForClosedAction(true);

        final Action action = prepareFinishedUpdate();

        controllerManagement.addUpdateActionStatus(
                entityFactory.actionStatus().create(action.getId()).status(Action.Status.RUNNING));

        // nothing changed as "feedback after close" is disabled
        assertThat(targetManagement.getByControllerID(TestdataFactory.DEFAULT_CONTROLLER_ID).get().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.IN_SYNC);

        assertThat(actionStatusRepository.count()).isEqualTo(3);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, action.getId()).getNumberOfElements())
                .isEqualTo(3);
    }

    @Test
    @Description("Controller trys to send an update feedback after it has been finished which is accepted as the repository is "
            + "configured to accept them.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3) })
    public void sendUpdatesForFinishUpdateProcessAcceptedIfEnabled() {
        repositoryProperties.setRejectActionStatusForClosedAction(false);

        Action action = prepareFinishedUpdate();
        action = controllerManagement.addUpdateActionStatus(
                entityFactory.actionStatus().create(action.getId()).status(Action.Status.RUNNING));

        // nothing changed as "feedback after close" is disabled
        assertThat(targetManagement.getByControllerID(TestdataFactory.DEFAULT_CONTROLLER_ID).get().getUpdateStatus())
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
    public void updateTargetAttributes() throws Exception {
        final String controllerId = "test123";
        final Target target = testdataFactory.createTarget(controllerId);

        securityRule.runAs(WithSpringAuthorityRule.withController("controller", CONTROLLER_ROLE_ANONYMOUS), () -> {
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
        final Map<String, String> testData = Maps.newHashMapWithExpectedSize(1);
        testData.put("test1", "testdata1");
        controllerManagement.updateControllerAttributes(controllerId, testData, null);

        assertThat(targetManagement.getControllerAttributes(controllerId)).as("Controller Attributes are wrong")
                .isEqualTo(testData);
    }

    @Step
    private void addSecondAttributeAndVerify(final String controllerId) {
        final Map<String, String> testData = Maps.newHashMapWithExpectedSize(2);
        testData.put("test2", "testdata20");
        controllerManagement.updateControllerAttributes(controllerId, testData, null);

        testData.put("test1", "testdata1");
        assertThat(targetManagement.getControllerAttributes(controllerId)).as("Controller Attributes are wrong")
                .isEqualTo(testData);
    }

    @Step
    private void updateAttributeAndVerify(final String controllerId) {
        final Map<String, String> testData = Maps.newHashMapWithExpectedSize(2);
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
    public void updateTargetAttributesWithDifferentUpdateModes() {

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

        assertThatExceptionOfType(QuotaExceededException.class).isThrownBy(() -> securityRule
                .runAs(WithSpringAuthorityRule.withController("controller", CONTROLLER_ROLE_ANONYMOUS), () -> {
                    writeAttributes(controllerId, allowedAttributes + 1, "key", "value");
                    return null;
                })).withMessageContaining("" + allowedAttributes);

        // verify that no attributes have been written
        assertThat(targetManagement.getControllerAttributes(controllerId)).isEmpty();

        // Write allowed number of attributes twice with same key should result
        // in update but work
        securityRule.runAs(WithSpringAuthorityRule.withController("controller", CONTROLLER_ROLE_ANONYMOUS), () -> {
            writeAttributes(controllerId, allowedAttributes, "key", "value1");
            writeAttributes(controllerId, allowedAttributes, "key", "value2");
            return null;
        });
        assertThat(targetManagement.getControllerAttributes(controllerId)).hasSize(10);

        // Now rite one more
        assertThatExceptionOfType(QuotaExceededException.class).isThrownBy(() -> securityRule
                .runAs(WithSpringAuthorityRule.withController("controller", CONTROLLER_ROLE_ANONYMOUS), () -> {
                    writeAttributes(controllerId, 1, "additional", "value1");
                    return null;
                })).withMessageContaining("" + allowedAttributes);
        assertThat(targetManagement.getControllerAttributes(controllerId)).hasSize(10);

    }

    private void writeAttributes(final String controllerId, final int allowedAttributes, final String keyPrefix,
            final String valuePrefix) {
        final Map<String, String> testData = Maps.newHashMapWithExpectedSize(allowedAttributes);
        for (int i = 0; i < allowedAttributes; i++) {
            testData.put(keyPrefix + i, valuePrefix);
        }
        controllerManagement.updateControllerAttributes(controllerId, testData, null);
    }

    @Test
    @Description("Checks if invalid values of attribute-key and attribute-value are handled correctly")
    public void updateTargetAttributesFailsForInvalidAttributes() {
        final String keyTooLong = "123456789012345678901234567890123";
        final String keyValid = "12345678901234567890123456789012";
        final String valueTooLong = "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
        final String valueValid = "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678";
        final String keyNull = null;

        final String controllerId = "targetId123";
        testdataFactory.createTarget(controllerId);

        assertThatExceptionOfType(InvalidTargetAttributeException.class)
                .isThrownBy(() -> controllerManagement.updateControllerAttributes(controllerId,
                        Collections.singletonMap(keyTooLong, valueValid), null))
                .as("Attribute with key too long should not be created");

        assertThatExceptionOfType(InvalidTargetAttributeException.class)
                .isThrownBy(() -> controllerManagement.updateControllerAttributes(controllerId,
                        Collections.singletonMap(keyTooLong, valueTooLong), null))
                .as("Attribute with key too long and value too long should not be created");

        assertThatExceptionOfType(InvalidTargetAttributeException.class)
                .isThrownBy(() -> controllerManagement.updateControllerAttributes(controllerId,
                        Collections.singletonMap(keyValid, valueTooLong), null))
                .as("Attribute with value too long should not be created");

        assertThatExceptionOfType(InvalidTargetAttributeException.class)
                .isThrownBy(() -> controllerManagement.updateControllerAttributes(controllerId,
                        Collections.singletonMap(keyNull, valueValid), null))
                .as("Attribute with key NULL should not be created");
    }

    @Test
    @Description("Controller providing status entries fails if providing more than permitted by quota.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3) })
    public void controllerProvidesIntermediateFeedbackFailsIfQuotaHit() {
        final int allowStatusEntries = 10;
        final Long actionId = createTargetAndAssignDs();

        // Fails as one entry is already in there from the assignment
        assertThatExceptionOfType(QuotaExceededException.class).isThrownBy(() -> securityRule
                .runAs(WithSpringAuthorityRule.withController("controller", CONTROLLER_ROLE_ANONYMOUS), () -> {
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

        final Long actionId = assignDistributionSet(testDs, testTarget).getActions().get(0);

        controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(actionId)
                .status(Action.Status.RUNNING).messages(Lists.newArrayList("proceeding message 1")));
        controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(actionId)
                .status(Action.Status.RUNNING).messages(Lists.newArrayList("proceeding message 2")));

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
            @Expect(type = ActionCreatedEvent.class, count = 2), @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 2),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 6) })
    public void addActionStatusUpdatesUntilQuotaIsExceeded() {

        // any distribution set assignment causes 1 status entity to be created
        final int maxStatusEntries = quotaManagement.getMaxStatusEntriesPerAction() - 1;

        // test for informational status
        final Long actionId1 = assignDistributionSet(testdataFactory.createDistributionSet("ds1"),
                testdataFactory.createTargets(1, "t1")).getActions().get(0);
        assertThat(actionId1).isNotNull();
        for (int i = 0; i < maxStatusEntries; ++i) {
            controllerManagement.addInformationalActionStatus(entityFactory.actionStatus().create(actionId1)
                    .status(Status.WARNING).message("Msg " + i).occurredAt(System.currentTimeMillis()));
        }
        assertThatExceptionOfType(QuotaExceededException.class).isThrownBy(() -> controllerManagement
                .addInformationalActionStatus(entityFactory.actionStatus().create(actionId1).status(Status.WARNING)));

        // test for update status (and mixed case)
        final Long actionId2 = assignDistributionSet(testdataFactory.createDistributionSet("ds2"),
                testdataFactory.createTargets(1, "t2")).getActions().get(0);
        assertThat(actionId2).isNotEqualTo(actionId1);
        for (int i = 0; i < maxStatusEntries; ++i) {
            controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(actionId2)
                    .status(Status.WARNING).message("Msg " + i).occurredAt(System.currentTimeMillis()));
        }
        assertThatExceptionOfType(QuotaExceededException.class).isThrownBy(() -> controllerManagement
                .addInformationalActionStatus(entityFactory.actionStatus().create(actionId2).status(Status.WARNING)));

    }

    @Test
    @Description("Verifies that the quota specifying the maximum number of messages per action status is enforced.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3) })
    public void createActionStatusWithTooManyMessages() {

        final int maxMessages = quotaManagement.getMaxMessagesPerActionStatus();

        final Long actionId = assignDistributionSet(testdataFactory.createDistributionSet("ds1"),
                testdataFactory.createTargets(1)).getActions().get(0);
        assertThat(actionId).isNotNull();

        final List<String> messages = Lists.newArrayList();
        IntStream.range(0, maxMessages).forEach(i -> messages.add(i, "msg"));

        assertThat(controllerManagement.addInformationalActionStatus(
                entityFactory.actionStatus().create(actionId).messages(messages).status(Status.WARNING))).isNotNull();

        messages.add("msg");
        assertThatExceptionOfType(QuotaExceededException.class)
                .isThrownBy(() -> controllerManagement.addInformationalActionStatus(
                        entityFactory.actionStatus().create(actionId).messages(messages).status(Status.WARNING)));

    }

}
