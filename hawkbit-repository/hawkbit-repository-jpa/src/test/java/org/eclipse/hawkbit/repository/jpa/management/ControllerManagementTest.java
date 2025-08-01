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
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.eclipse.hawkbit.im.authentication.SpringEvalExpressions.CONTROLLER_ROLE;
import static org.eclipse.hawkbit.im.authentication.SpringEvalExpressions.CONTROLLER_ROLE_ANONYMOUS;
import static org.eclipse.hawkbit.repository.jpa.configuration.Constants.TX_RT_MAX;
import static org.eclipse.hawkbit.repository.model.Action.ActionType.DOWNLOAD_ONLY;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.runAs;
import static org.eclipse.hawkbit.repository.test.util.TestdataFactory.DEFAULT_CONTROLLER_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import jakarta.validation.ConstraintViolationException;

import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.TargetTypeManagement;
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
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTypeCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
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
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch;
import org.eclipse.hawkbit.repository.test.util.TargetTestData;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;

/**
 * Feature: Component Tests - Repository<br/>
 * Story: Controller Management
 */
class ControllerManagementTest extends AbstractJpaIntegrationTest {

    @Autowired
    private RepositoryProperties repositoryProperties;

    /**
     * Ensures that target attribute update fails if quota hits.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2) })
    void updateTargetAttributesFailsIfTooManyEntries() {
        final String controllerId = "test123";
        final int allowedAttributes = quotaManagement.getMaxAttributeEntriesPerTarget();
        testdataFactory.createTarget(controllerId);

        final WithUser withController = SecurityContextSwitch.withController("controller", CONTROLLER_ROLE_ANONYMOUS);
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> runAs(withController, () -> writeAttributes(controllerId, allowedAttributes + 1, "key", "value")))
                .withMessageContaining("" + allowedAttributes);

        // verify that no attributes have been written
        assertThat(targetManagement.getControllerAttributes(controllerId)).isEmpty();

        // Write allowed number of attributes twice with same key should result
        // in update but work
        SecurityContextSwitch.runAs(withController, () -> {
            writeAttributes(controllerId, allowedAttributes, "key", "value1");
            writeAttributes(controllerId, allowedAttributes, "key", "value2");
        });
        assertThat(targetManagement.getControllerAttributes(controllerId)).hasSize(10);

        // Now rite one more
        assertThatExceptionOfType(AssignmentQuotaExceededException.class).isThrownBy(() -> SecurityContextSwitch
                .getAs(withController, () -> {
                    writeAttributes(controllerId, 1, "additional", "value1");
                    return null;
                })).withMessageContaining("" + allowedAttributes);
        assertThat(targetManagement.getControllerAttributes(controllerId)).hasSize(10);

    }

    /**
     * Checks if invalid values of attribute-key and attribute-value are handled correctly
     */
    @Test
    void updateTargetAttributesFailsForInvalidAttributes() {
        final String controllerId = "targetId123";
        testdataFactory.createTarget(controllerId);

        final Map<String, String> attributesLV =
                Collections.singletonMap(TargetTestData.ATTRIBUTE_KEY_TOO_LONG, TargetTestData.ATTRIBUTE_VALUE_VALID);
        assertThatExceptionOfType(InvalidTargetAttributeException.class)
                .as("Attribute with key too long should not be created")
                .isThrownBy(() -> controllerManagement.updateControllerAttributes(controllerId, attributesLV, null));

        final Map<String, String> attributesLL =
                Collections.singletonMap(TargetTestData.ATTRIBUTE_KEY_TOO_LONG, TargetTestData.ATTRIBUTE_VALUE_TOO_LONG);
        assertThatExceptionOfType(InvalidTargetAttributeException.class)
                .as("Attribute with key too long and value too long should not be created")
                .isThrownBy(() -> controllerManagement.updateControllerAttributes(controllerId, attributesLL, null));

        final Map<String, String> attributesVL =
                Collections.singletonMap(TargetTestData.ATTRIBUTE_KEY_VALID, TargetTestData.ATTRIBUTE_VALUE_TOO_LONG);
        assertThatExceptionOfType(InvalidTargetAttributeException.class)
                .as("Attribute with value too long should not be created")
                .isThrownBy(() -> controllerManagement.updateControllerAttributes(controllerId, attributesVL, null));

        final Map<String, String> attributesNV = Collections.singletonMap(null, TargetTestData.ATTRIBUTE_VALUE_VALID);
        assertThatExceptionOfType(InvalidTargetAttributeException.class)
                .as("Attribute with key NULL should not be created")
                .isThrownBy(() -> controllerManagement.updateControllerAttributes(controllerId, attributesNV, null));
    }

    /**
     * Controller providing status entries fails if providing more than permitted by quota.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    void controllerProvidesIntermediateFeedbackFailsIfQuotaHit() {
        final int allowStatusEntries = 10;
        final Long actionId = createTargetAndAssignDs();

        SecurityContextSwitch
                .getAs(SecurityContextSwitch.withController("controller", CONTROLLER_ROLE_ANONYMOUS), () -> {
                    // Fails as one entry is already in there from the assignment
                    assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                            .isThrownBy(() -> writeStatus(actionId, allowStatusEntries))
                            .withMessageContaining(String.valueOf(allowStatusEntries));
                    return null;
                });
    }

    /**
     * Test to verify the storage and retrieval of action history.
     */
    @Test
    void findMessagesByActionStatusId() {
        final DistributionSet testDs = testdataFactory.createDistributionSet("1");
        final List<Target> testTarget = testdataFactory.createTargets(1);

        final Long actionId = getFirstAssignedActionId(assignDistributionSet(testDs, testTarget));

        controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(actionId)
                .status(Action.Status.RUNNING).occurredAt(System.currentTimeMillis())
                .messages(List.of("proceeding message 1")));

        waitNextMillis();
        controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(actionId)
                .status(Action.Status.RUNNING).occurredAt(System.currentTimeMillis())
                .messages(List.of("proceeding message 2")));

        final List<String> messages = controllerManagement.getActionHistoryMessages(actionId, 2);

        assertThat(deploymentManagement.findActionStatusByAction(actionId, PAGE).getTotalElements())
                .as("Two action-states in total").isEqualTo(3L);
        assertThat(messages.get(0)).as("Message of action-status").isEqualTo("proceeding message 2");
        assertThat(messages.get(1)).as("Message of action-status").isEqualTo("proceeding message 1");
    }

    /**
     * Verifies that the quota specifying the maximum number of status entries per action is enforced.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 2),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 6),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 2), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 6), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 2) })
    void addActionStatusUpdatesUntilQuotaIsExceeded() {
        // any distribution set assignment causes 1 status entity to be created
        final int maxStatusEntries = quotaManagement.getMaxStatusEntriesPerAction() - 1;

        // test for informational status
        final Long actionId1 = getFirstAssignedActionId(assignDistributionSet(
                testdataFactory.createDistributionSet("ds1"), testdataFactory.createTargets(1, "t1")));
        assertThat(actionId1).isNotNull();
        final ActionStatusCreate status = entityFactory.actionStatus().create(actionId1).status(Status.WARNING);
        for (int i = 0; i < maxStatusEntries; ++i) {
            controllerManagement.addInformationalActionStatus(status.message("Msg " + i).occurredAt(System.currentTimeMillis()));
        }
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> controllerManagement.addInformationalActionStatus(status));

        // test for update status (and mixed case)
        final Long actionId2 = getFirstAssignedActionId(assignDistributionSet(
                testdataFactory.createDistributionSet("ds2"), testdataFactory.createTargets(1, "t2")));
        assertThat(actionId2).isNotEqualTo(actionId1);
        final ActionStatusCreate statusWarning = entityFactory.actionStatus().create(actionId2).status(Status.WARNING);
        for (int i = 0; i < maxStatusEntries; ++i) {
            controllerManagement.addUpdateActionStatus(statusWarning.message("Msg " + i).occurredAt(System.currentTimeMillis()));
        }
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> controllerManagement.addInformationalActionStatus(statusWarning));
    }

    /**
     * Verifies that the quota specifying the maximum number of messages per action status is enforced.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    void createActionStatusWithTooManyMessages() {
        final int maxMessages = quotaManagement.getMaxMessagesPerActionStatus();

        final Long actionId = getFirstAssignedActionId(
                assignDistributionSet(testdataFactory.createDistributionSet("ds1"), testdataFactory.createTargets(1)));
        assertThat(actionId).isNotNull();

        final List<String> messages = new ArrayList<>();
        IntStream.range(0, maxMessages).forEach(i -> messages.add(i, "msg"));

        assertThat(controllerManagement.addInformationalActionStatus(
                entityFactory.actionStatus().create(actionId).messages(messages).status(Status.WARNING))).isNotNull();

        messages.add("msg");
        final ActionStatusCreate statusToManyMessages = entityFactory.actionStatus().create(actionId).messages(messages).status(Status.WARNING);
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> controllerManagement.addInformationalActionStatus(statusToManyMessages));
    }

    /**
     * Verifies that a DOWNLOAD_ONLY action is not marked complete when the controller reports DOWNLOAD
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    void controllerReportsDownloadForDownloadOnlyAction() {
        testdataFactory.createTarget();
        final Long actionId = createAndAssignDsAsDownloadOnly("downloadOnlyDs", DEFAULT_CONTROLLER_ID);
        assertThat(actionId).isNotNull();
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Status.DOWNLOAD));
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, Action.Status.RUNNING,
                Action.Status.DOWNLOAD, true);

        assertThat(actionStatusRepository.count()).isEqualTo(2);
        assertThat(controllerManagement.findActionStatusByAction(actionId, PAGE).getNumberOfElements()).isEqualTo(2);
        assertThat(activeActionExistsForControllerId(DEFAULT_CONTROLLER_ID)).isTrue();
    }

    /**
     * Verifies that management get access react as specified on calls for non existing entities by means
     * of Optional not present.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
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

    /**
     * Verifies that management queries react as specified on calls for non existing entities
     * by means of throwing EntityNotFoundException.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 1) })
    void entityQueriesReferringToNotExistingEntitiesThrowsException() {
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

        verifyThrownExceptionBy(() -> controllerManagement.findActionStatusByAction(NOT_EXIST_IDL, PAGE), "Action");
        verifyThrownExceptionBy(() -> controllerManagement.hasTargetArtifactAssigned(NOT_EXIST_IDL, "XXX"), "Target");

        verifyThrownExceptionBy(() -> controllerManagement.hasTargetArtifactAssigned(NOT_EXIST_ID, "XXX"), "Target");

        verifyThrownExceptionBy(() -> controllerManagement.registerRetrieved(NOT_EXIST_IDL, "test message"), "Action");

        verifyThrownExceptionBy(
                () -> controllerManagement.updateControllerAttributes(NOT_EXIST_ID, new HashMap<>(), null), "Target");
    }

    /**
     * Controller confirms successful update with FINISHED status.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 1),
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
        assertThat(controllerManagement.findActionStatusByAction(actionId, PAGE).getNumberOfElements()).isEqualTo(7);
    }

    /**
     * Controller confirmation fails with invalid messages.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    void controllerConfirmationFailsWithInvalidMessages() {
        final Long actionId = createTargetAndAssignDs();

        simulateIntermediateStatusOnUpdate(actionId);

        final ActionStatusCreate statusSingleMessage = entityFactory.actionStatus().create(actionId)
                .status(Status.FINISHED).message(INVALID_TEXT_HTML);
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set invalid description text should not be created")
                .isThrownBy(() -> controllerManagement.addUpdateActionStatus(statusSingleMessage));

        final ActionStatusCreate statusMulipleMessages = entityFactory.actionStatus().create(actionId)
                .status(Status.FINISHED).messages(Arrays.asList("this is valid.", INVALID_TEXT_HTML));
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set invalid description text should not be created")
                .isThrownBy(() -> controllerManagement.addUpdateActionStatus(statusMulipleMessages));

        assertThat(actionStatusRepository.count()).isEqualTo(6);
        assertThat(controllerManagement.findActionStatusByAction(actionId, PAGE).getNumberOfElements()).isEqualTo(6);
    }

    /**
     * Controller confirms successful update with FINISHED status on a action that is on canceling.
     * Reason: The decision to ignore the cancellation is in fact up to the controller.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 2),
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
        assertThat(controllerManagement.findActionStatusByAction(actionId, PAGE).getNumberOfElements()).isEqualTo(3);
    }

    /**
     * Update server rejects cancellation feedback if action is not in CANCELING state.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    void cancellationFeedbackRejectedIfActionIsNotInCanceling() {
        final Long actionId = createTargetAndAssignDs();

        final ActionStatusCreate status = entityFactory.actionStatus().create(actionId).status(Status.FINISHED);
        assertThatExceptionOfType(CancelActionNotAllowedException.class)
                .as("Expected " + CancelActionNotAllowedException.class.getName())
                .isThrownBy(() -> controllerManagement.addCancelActionStatus(status));

        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, Action.Status.RUNNING, Action.Status.RUNNING, true);

        assertThat(actionStatusRepository.count()).isEqualTo(1);
        assertThat(controllerManagement.findActionStatusByAction(actionId, PAGE).getNumberOfElements()).isEqualTo(1);
    }

    /**
     * Controller confirms action cancellation with FINISHED status.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 2),
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
        assertThat(controllerManagement.findActionStatusByAction(actionId, PAGE).getNumberOfElements()).isEqualTo(8);
    }

    /**
     * Controller confirms action cancellation with FINISHED status.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 2),
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
        assertThat(controllerManagement.findActionStatusByAction(actionId, PAGE).getNumberOfElements()).isEqualTo(8);
    }

    /**
     * Controller rejects action cancellation with CANCEL_REJECTED status. Action goes back to RUNNING status as it expects
     * that the controller will continue the original update.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 2),
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
        assertThat(controllerManagement.findActionStatusByAction(actionId, PAGE).getNumberOfElements()).isEqualTo(8);
    }

    /**
     * Controller rejects action cancellation with ERROR status. Action goes back to RUNNING status as it expects
     * that the controller will continue the original update.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 2),
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
        assertThat(controllerManagement.findActionStatusByAction(actionId, PAGE).getNumberOfElements()).isEqualTo(8);
    }

    /**
     * Verifies that assignment verification works based on SHA1 hash. By design it is not important which artifact
     * is actually used for the check as long as they have an identical binary, i.e. same SHA1 hash.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 6),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 5), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    void hasTargetArtifactAssignedIsTrueWithMultipleArtifacts() {
        final int artifactSize = 5 * 1024;
        final byte[] random = randomBytes(artifactSize);

        final DistributionSet ds = testdataFactory.createDistributionSet("");
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2");
        Target savedTarget = testdataFactory.createTarget();

        // create two artifacts with identical SHA1 hash
        final Artifact artifact = artifactManagement.create(new ArtifactUpload(new ByteArrayInputStream(random),
                findFirstModuleByType(ds, osType).orElseThrow().getId(), "file1", false, artifactSize));
        final Artifact artifact2 = artifactManagement.create(new ArtifactUpload(new ByteArrayInputStream(random),
                findFirstModuleByType(ds2, osType).orElseThrow().getId(), "file1", false, artifactSize));
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

    /**
     * Register a controller which does not exist
     */
    @Test
    @WithUser(principal = "controller", authorities = { CONTROLLER_ROLE })
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 2) })
    void findOrRegisterTargetIfItDoesNotExist() {
        final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST);
        assertThat(target).as("target should not be null").isNotNull();

        final Target sameTarget = controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST);
        assertThat(target.getId()).as("Target should ben equals").isEqualTo(sameTarget.getId());
        assertThat(targetRepository.count()).as("Only 1 target should be registered").isEqualTo(1L);
    }

    /**
     * Register a controller with name which does not exist and update its name
     */
    @Test
    @WithUser(principal = "controller", authorities = { CONTROLLER_ROLE })
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 1) })
    void findOrRegisterTargetIfItDoesNotExistWithName() {
        final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST, "TestName", null);
        final Target sameTarget = controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST,
                "ChangedTestName", null);
        assertThat(target.getId()).as("Target should be the equals").isEqualTo(sameTarget.getId());
        assertThat(target.getName()).as("Target names should be different").isNotEqualTo(sameTarget.getName());
        assertThat(sameTarget.getName()).as("Target name should be changed").isEqualTo("ChangedTestName");
        assertThat(targetRepository.count()).as("Only 1 target should be registred").isEqualTo(1L);
    }

    /**
     * Register a controller which does not exist with existing target type and update its target type to another existing one
     */
    @Test
    @WithUser(principal = "controller", authorities = { CONTROLLER_ROLE })
    @ExpectEvents({
            @Expect(type = TargetTypeCreatedEvent.class, count = 2),
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

    /**
     * Register a controller which does not exist with existing target type and update its target type to non existing one
     */
    @Test
    @WithUser(principal = "controller", authorities = { CONTROLLER_ROLE })
    @ExpectEvents({
            @Expect(type = TargetTypeCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 2) })
    void findOrRegisterTargetIfItDoesNotExistWithExistingTypeAndUpdateToNonExistingType() {
        createTargetType("knownTargetTypeName");
        final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST,
                null, "knownTargetTypeName");
        final Target sameTarget = controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST,
                null, "unknownTargetTypeName");
        assertThat(target.getId()).as("Target should be the same").isEqualTo(sameTarget.getId());
        assertThat(sameTarget.getTargetType().getName()).as("Target type should be unchanged")
                .isEqualTo("knownTargetTypeName");
        assertThat(targetRepository.count()).as("Only 1 target should be registred").isEqualTo(1L);
    }

    /**
     * Register a controller which does not exist with existing target type and unassign its target type
     */
    @Test
    @WithUser(principal = "controller", authorities = { CONTROLLER_ROLE })
    @ExpectEvents({
            @Expect(type = TargetTypeCreatedEvent.class, count = 1),
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

    /**
     * Register a controller which does not exist without target type and update its target type to existing one
     */
    @Test
    @WithUser(principal = "controller", authorities = { CONTROLLER_ROLE })
    @ExpectEvents({
            @Expect(type = TargetTypeCreatedEvent.class, count = 1),
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

    /**
     * Register a controller which does not exist with non existing target type and update its target type to existing one
     */
    @Test
    @WithUser(principal = "controller", authorities = { CONTROLLER_ROLE })
    @ExpectEvents({
            @Expect(type = TargetTypeCreatedEvent.class, count = 1),
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

    /**
     * Tries to register a target with an invalid controller id
     */
    @Test
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

        final String controllerId = randomString(Target.CONTROLLER_ID_MAX_SIZE + 1);
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("register target with too long controllerId should fail")
                .isThrownBy(() -> controllerManagement.findOrRegisterTargetIfItDoesNotExist(
                        controllerId, LOCALHOST));
    }

    /**
     * Register a controller which does not exist, when a ConcurrencyFailureException is raised, the
     * exception is rethrown after max retries
     */
    @Test
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

    /**
     * Register a controller which does not exist, when a ConcurrencyFailureException is raised, the
     * exception is not rethrown when the max retries are not yet reached
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
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

    /**
     * Register a controller which does not exist, then update the controller twice, first time by providing a name property and second time without a new name
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 3),
            @Expect(type = TargetUpdatedEvent.class, count = 1) })
    void findOrRegisterTargetIfItDoesNotExistDoesUpdateNameOnExistingTargetProperly() {
        final String controllerId = "12345";
        final String targetName = "UpdatedName";

        final Target newTarget = controllerManagement.findOrRegisterTargetIfItDoesNotExist(controllerId, LOCALHOST);
        assertThat(newTarget.getName()).isEqualTo(controllerId);

        final Target firstTimeUpdatedTarget = controllerManagement.findOrRegisterTargetIfItDoesNotExist(
                controllerId, LOCALHOST, targetName, null);
        assertThat(firstTimeUpdatedTarget.getName()).isEqualTo(targetName);

        // Name should not change to default (name=targetId) if target is updated without new name provided
        final Target secondTimeUpdatedTarget = controllerManagement.findOrRegisterTargetIfItDoesNotExist(controllerId, LOCALHOST);
        assertThat(secondTimeUpdatedTarget.getName()).isEqualTo(targetName);
    }

    /**
     * Register a controller which does not exist, if a EntityAlreadyExistsException is raised, the
     * exception is rethrown and no further retries will be attempted
     */
    @Test
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

    /**
     * Retry is aborted when an unchecked exception is thrown and the exception should also be
     * rethrown
     */
    @Test
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

    /**
     * Verify that targetVisible metadata is returned from repository
     */
    @Test
    @ExpectEvents({
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 6) })
    void findTargetVisibleMetaDataBySoftwareModuleId() {
        final DistributionSet set = testdataFactory.createDistributionSet();
        testdataFactory.addSoftwareModuleMetadata(set);

        final Map<Long, Map<String, String>> result = controllerManagement
                .findTargetVisibleMetaDataBySoftwareModuleId(set.getModules().stream().map(SoftwareModule::getId).toList());

        assertThat(result).hasSize(3);
        result.forEach((key, value) -> assertThat(value).hasSize(1));
    }

    /**
     * Verify that controller registration does not result in a TargetPollEvent if feature is disabled
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1) })
    @SuppressWarnings("java:S2699")
    // java:S2699 - test tests the fired events, no need for assert
    void targetPollEventNotSendIfDisabled() {
        repositoryProperties.setPublishTargetPollEvent(false);
        controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST);
        repositoryProperties.setPublishTargetPollEvent(true);
    }

    /**
     * Controller tries to finish an update process after it has been finished by an error action status.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    void tryToFinishWithErrorUpdateProcessMoreThanOnce() {
        final Long actionId = createTargetAndAssignDs();

        // test and verify
        controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.RUNNING));
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, Action.Status.RUNNING, Action.Status.RUNNING, true);

        controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.ERROR));
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.ERROR, Action.Status.ERROR, Action.Status.ERROR, false);

        // try with disabled late feedback
        withRejectActionStatusForClosedAction(true, () ->
                controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.FINISHED)));

        // test
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.ERROR, Action.Status.ERROR, Action.Status.ERROR, false);

        // try with enabled late feedback - should not make a difference as it
        // only allows intermediate feedback and not multiple close
        withRejectActionStatusForClosedAction(false,
                () -> controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.FINISHED)));

        // test
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.ERROR, Action.Status.ERROR, Action.Status.ERROR, false);

        assertThat(actionStatusRepository.count()).isEqualTo(3);
        assertThat(controllerManagement.findActionStatusByAction(actionId, PAGE).getNumberOfElements()).isEqualTo(3);
    }

    /**
     * Controller tries to finish an update process after it has been finished by an FINISHED action status.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1) })
    void tryToFinishUpdateProcessMoreThanOnce() {
        final Long actionId = prepareFinishedUpdate().getId();

        withRejectActionStatusForClosedAction(true,
                () -> controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.FINISHED)));

        // test
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.IN_SYNC, Action.Status.FINISHED,
                Action.Status.FINISHED, false);

        withRejectActionStatusForClosedAction(false,
                () -> controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Action.Status.FINISHED)));

        // test
        assertActionStatus(
                actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.IN_SYNC, Action.Status.FINISHED, Action.Status.FINISHED, false);
        assertThat(actionStatusRepository.count()).isEqualTo(3);
        assertThat(controllerManagement.findActionStatusByAction(actionId, PAGE).getNumberOfElements()).isEqualTo(3);
    }

    /**
     * Controller tries to send an update feedback after it has been finished which is reject as the repository is configured to reject that.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1) })
    void sendUpdatesForFinishUpdateProcessDroppedIfDisabled() {
        final Action action = prepareFinishedUpdate();
        withRejectActionStatusForClosedAction(true,
                () -> controllerManagement.addUpdateActionStatus(
                        entityFactory.actionStatus().create(action.getId()).status(Action.Status.RUNNING)));

        // nothing changed as "feedback after close" is disabled
        assertThat(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.IN_SYNC);
        assertThat(actionRepository.findById(action.getId()))
                .hasValueSatisfying(a -> assertThat(a.getStatus()).isEqualTo(Status.FINISHED));
        assertThat(actionStatusRepository.count()).isEqualTo(3);
        assertThat(controllerManagement.findActionStatusByAction(action.getId(), PAGE).getNumberOfElements())
                .isEqualTo(3);
    }

    /**
     * Controller tries to send an update feedback after it has been finished which is accepted as the repository is
     * configured to accept them.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
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
        Action action = prepareFinishedUpdate();
        withRejectActionStatusForClosedAction(false,
                () -> controllerManagement.addUpdateActionStatus(
                        entityFactory.actionStatus().create(action.getId()).status(Action.Status.RUNNING)));

        // nothing changed as "feedback after close" is disabled
        assertThat(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get().getUpdateStatus()).isEqualTo(TargetUpdateStatus.IN_SYNC);
        // however, additional action status has been stored
        assertThat(actionStatusRepository.findAll(PAGE).getNumberOfElements()).isEqualTo(4);
        assertThat(controllerManagement.findActionStatusByAction(action.getId(), PAGE).getNumberOfElements()).isEqualTo(4);
    }

    /**
     * Ensures that target attribute update is reflected by the repository.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 3) })
    void updateTargetAttributes() {
        final String controllerId = "test123";
        final Target target = testdataFactory.createTarget(controllerId);

        SecurityContextSwitch.getAs(SecurityContextSwitch.withController(
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

    /**
     * Ensures that target attributes can be updated using different update modes.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
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

    /**
     * Verifies that a DOWNLOAD_ONLY action is marked complete once the controller reports DOWNLOADED
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    void controllerReportsDownloadedForDownloadOnlyAction() {
        testdataFactory.createTarget();
        final Long actionId = createAndAssignDsAsDownloadOnly("downloadOnlyDs", DEFAULT_CONTROLLER_ID);
        assertThat(actionId).isNotNull();
        controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Status.DOWNLOADED));
        assertActionStatus(
                actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.IN_SYNC, Action.Status.DOWNLOADED, Action.Status.DOWNLOADED, false);

        assertThat(actionStatusRepository.count()).isEqualTo(2);
        assertThat(controllerManagement.findActionStatusByAction(actionId, PAGE).getNumberOfElements()).isEqualTo(2);
        assertThat(activeActionExistsForControllerId(DEFAULT_CONTROLLER_ID)).isFalse();
    }

    /**
     * Verifies that a controller can report a FINISHED event for a DOWNLOAD_ONLY non-active action.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 3),
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
        assertThat(controllerManagement.findActionStatusByAction(actionId, PAGE).getNumberOfElements()).isEqualTo(3);
        assertThat(activeActionExistsForControllerId(DEFAULT_CONTROLLER_ID)).isFalse();
    }

    /**
     * Verifies that multiple DOWNLOADED events for a DOWNLOAD_ONLY action are handled.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
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
        assertThat(controllerManagement.findActionStatusByAction(actionId, PAGE).getNumberOfElements()).isEqualTo(4);
        assertThat(activeActionExistsForControllerId(DEFAULT_CONTROLLER_ID)).isFalse();
    }

    /**
     * Verifies that quota is asserted when a controller reports too many DOWNLOADED events for a
     * DOWNLOAD_ONLY action.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 9),
            @Expect(type = ActionUpdatedEvent.class, count = 1) })
    void quotaExceptionWhenControllerReportsTooManyDownloadedMessagesForDownloadOnlyAction() {
        final int maxMessages = quotaManagement.getMaxMessagesPerActionStatus();
        testdataFactory.createTarget();
        final Long actionId = createAndAssignDsAsDownloadOnly("downloadOnlyDsTMD", DEFAULT_CONTROLLER_ID);
        assertThat(actionId).isNotNull();

        final IntConsumer op = i -> controllerManagement.addUpdateActionStatus(
                entityFactory.actionStatus().create(actionId).status(Status.DOWNLOADED));
        Assertions.assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> forNTimes(maxMessages, op));
    }

    /**
     * Verifies that quota is enforced for UpdateActionStatus events for DOWNLOAD_ONLY assignments.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 3),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 10),
            @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    void quotaExceededExceptionWhenControllerReportsTooManyUpdateActionStatusMessagesForDownloadOnlyAction() {
        final int maxMessages = quotaManagement.getMaxMessagesPerActionStatus();
        testdataFactory.createTarget();
        final Long actionId = createAndAssignDsAsDownloadOnly("downloadOnlyDsTMU", DEFAULT_CONTROLLER_ID);
        assertThat(actionId).isNotNull();

        // assert that too many intermediate statuses will throw quota exception
        final IntConsumer op = i -> controllerManagement.addUpdateActionStatus(
                entityFactory.actionStatus().create(actionId).status(Status.DOWNLOADED));
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .as("No QuotaExceededException thrown for too many DOWNLOADED updateActionStatus updates")
                .isThrownBy(() -> forNTimes(maxMessages, op));

        // assert that Final result is accepted even if quota is reached
        assertThatNoException().isThrownBy(() -> {
            Action updatedAction = controllerManagement.addUpdateActionStatus(
                    entityFactory.actionStatus().create(actionId).status(Status.FINISHED));
            // check if action really finished
            assertThat(updatedAction.isActive()).isFalse();
            // check if final status is updated accordingly
            assertThat(updatedAction.getStatus()).isEqualTo(Status.FINISHED);
        });

        // assert that additional final result is not accepted
        assertThatNoException().isThrownBy(() -> {
            Action updatedAction = controllerManagement
                    .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Status.ERROR));
            // check if action really finished
            assertThat(updatedAction.isActive()).isFalse();
            // check if final status is not changed - e.g. ERROR is not updated because action has already finished
            assertThat(updatedAction.getStatus()).isEqualTo(Status.FINISHED);
        });
    }

    /**
     * Verifies that quota is enforced for UpdateActionStatus events for FORCED assignments.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1) })
    void quotaExceededExceptionWhenControllerReportsTooManyUpdateActionStatusMessagesForForced() {
        final int maxMessages = quotaManagement.getMaxMessagesPerActionStatus();
        final Long actionId = createTargetAndAssignDs();
        assertThat(actionId).isNotNull();

        // assert that too many intermediate statuses will throw quota exception
        final IntConsumer op = i -> controllerManagement.addUpdateActionStatus(
                entityFactory.actionStatus().create(actionId).status(Status.DOWNLOADED));
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .as("No QuotaExceededException thrown for too many DOWNLOADED updateActionStatus updates")
                .isThrownBy(() -> forNTimes(maxMessages, op));

        // assert that Final result is accepted even if quota is reached
        assertThatNoException().isThrownBy(() -> {
            Action updatedAction = controllerManagement
                    .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Status.FINISHED));
            // check if action really finished
            assertThat(updatedAction.isActive()).isFalse();
            // check if final status is updated accordingly
            assertThat(updatedAction.getStatus()).isEqualTo(Status.FINISHED);
        });

        // assert that additional final result is not accepted
        assertThatNoException().isThrownBy(() -> {
            Action updatedAction = controllerManagement
                    .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Status.ERROR));
            // check if action really finished
            assertThat(updatedAction.isActive()).isFalse();
            // check if final status is not changed - e.g. ERROR is not updated because action has already finished
            assertThat(updatedAction.getStatus()).isEqualTo(Status.FINISHED);
        });
    }

    /**
     * Verify that the attaching externalRef to an action is properly stored
     */
    @Test
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

    /**
     * Verify that getting a single action using externalRef works
     */
    @Test
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

    /**
     * Verify that assigning version form target works
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3) }
    )
    void assignVersionToTarget() {

        final DistributionSet knownDistributionSet = testdataFactory.createDistributionSet();

        // GIVEN
        testdataFactory.createTarget(DEFAULT_CONTROLLER_ID).getId();

        // WHEN
        boolean updated1 = controllerManagement.updateOfflineAssignedVersion(DEFAULT_CONTROLLER_ID,
                knownDistributionSet.getName(), knownDistributionSet.getVersion());
        // if target is already assigned to a distribution then it shouldn't reassign the distribution
        boolean updated2 = controllerManagement.updateOfflineAssignedVersion(DEFAULT_CONTROLLER_ID,
                knownDistributionSet.getName(), knownDistributionSet.getVersion());

        // THEN
        assertAssignedDistributionSetId(DEFAULT_CONTROLLER_ID, knownDistributionSet.getId());
        assertInstalledDistributionSetId(DEFAULT_CONTROLLER_ID, knownDistributionSet.getId());
        assertThat(updated1).isTrue();
        assertThat(updated2).isFalse();
    }

    /**
     * Verify that a null externalRef cannot be assigned to an action
     */
    @Test
    void externalRefCannotBeNull() {
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("No ConstraintViolationException thrown when a null externalRef was set on an action")
                .isThrownBy(() -> controllerManagement.updateActionExternalRef(1L, null));
    }

    /**
     * Verifies that a target can report FINISHED/ERROR updates for DOWNLOAD_ONLY assignments regardless of
     * repositoryProperties.rejectActionStatusForClosedAction value.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 4),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 12),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 4), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 12), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 4),
            @Expect(type = TargetUpdatedEvent.class, count = 12),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 4),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 6),
            @Expect(type = ActionUpdatedEvent.class, count = 8) })
    void targetCanAlwaysReportFinishedOrErrorAfterActionIsClosedForDownloadOnlyAssignments() {
        testdataFactory.createTarget();

        // allow actionStatusUpdates for closed actions
        withRejectActionStatusForClosedAction(false,
                () -> {
                    final Long actionId = createAndAssignDsAsDownloadOnly("downloadOnlyDs1", DEFAULT_CONTROLLER_ID);
                    assertThat(actionId).isNotNull();
                    finishDownloadOnlyUpdateAndSendUpdateActionStatus(actionId, Status.FINISHED);

                    final Long actionId2 = createAndAssignDsAsDownloadOnly("downloadOnlyDs2", DEFAULT_CONTROLLER_ID);
                    assertThat(actionId2).isNotNull();
                    finishDownloadOnlyUpdateAndSendUpdateActionStatus(actionId2, Status.ERROR);
                });

        // disallow actionStatusUpdates for closed actions
        withRejectActionStatusForClosedAction(true,
                () -> {
                    final Long actionId3 = createAndAssignDsAsDownloadOnly("downloadOnlyDs3", DEFAULT_CONTROLLER_ID);
                    assertThat(actionId3).isNotNull();
                    finishDownloadOnlyUpdateAndSendUpdateActionStatus(actionId3, Status.FINISHED);

                    final Long actionId4 = createAndAssignDsAsDownloadOnly("downloadOnlyDs4", DEFAULT_CONTROLLER_ID);
                    assertThat(actionId4).isNotNull();
                    finishDownloadOnlyUpdateAndSendUpdateActionStatus(actionId4, Status.ERROR);

                    // actionStatusRepository should have 12 ActionStatusUpdates, 3 from
                    // each action
                    assertThat(actionStatusRepository.count()).isEqualTo(12L);
                });
    }

    /**
     * Verifies that a controller can report a FINISHED event for a DOWNLOAD_ONLY action after having
     * installed an intermediate update.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 6),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 2), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 6), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 5),
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

    /**
     * Actions are exposed according to thier weight in multi assignment mode.
     */
    @Test
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

    /**
     * Delete a target on requested target deletion from client side
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1),
            @Expect(type = TargetDeletedEvent.class, count = 1) })
    void deleteTargetWithValidThingId() {
        final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST);
        assertThat(target).as("target should not be null").isNotNull();
        assertThat(targetRepository.count()).as("target exists and is ready for deletion").isEqualTo(1L);

        controllerManagement.deleteExistingTarget(target.getControllerId());

        assertThat(targetRepository.count()).as("target should not exist anymore").isZero();
    }

    /**
     * Delete a target with a non existing thingId
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetDeletedEvent.class, count = 0) })
    void deleteTargetWithInvalidThingId() {
        assertThatExceptionOfType(EntityNotFoundException.class)
                .as("No EntityNotFoundException thrown when deleting a non-existing target")
                .isThrownBy(() -> controllerManagement.deleteExistingTarget("BB"));
        assertThat(targetRepository.count()).as("target should not exist").isZero();
    }

    /**
     * Delete a target after it has been deleted already
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1),
            @Expect(type = TargetDeletedEvent.class, count = 1) })
    void deleteTargetAfterItWasDeleted() {
        final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotExist("AA", LOCALHOST);
        assertThat(target).as("target should not be null").isNotNull();
        assertThat(targetRepository.count()).as("target exists and is ready for deletion").isEqualTo(1L);

        final String controllerId = target.getControllerId();
        controllerManagement.deleteExistingTarget(controllerId);
        assertThat(targetRepository.count()).as("target should not exist anymore").isZero();

        assertThatExceptionOfType(EntityNotFoundException.class)
                .as("No EntityNotFoundException thrown when deleting a non-existing target")
                .isThrownBy(() -> controllerManagement.deleteExistingTarget(controllerId));
    }

    /**
     * When action status code is provided in feedback it is also stored in the action field lastActionStatusCode
     */
    @Test
    void lastActionStatusCodeIsSet() {
        final Long actionId = createTargetAndAssignDs();

        addUpdateActionStatusAndAssert(actionId, Action.Status.RUNNING, 10);
        assertLastActionStatusCodeInAction(actionId, 10);

        addUpdateActionStatusAndAssert(actionId, Action.Status.RUNNING);
        assertLastActionStatusCodeInAction(actionId, null);

        addUpdateActionStatusAndAssert(actionId, Action.Status.RUNNING, 20);
        assertLastActionStatusCodeInAction(actionId, 20);
    }

    private static void forNTimes(final int n, final IntConsumer consumer) {
        IntStream.range(0, n).forEach(consumer);
    }

    private void withRejectActionStatusForClosedAction(final boolean rejectActionStatusForClosedAction, final Runnable runnable) {
        final boolean originalValue = repositoryProperties.isRejectActionStatusForClosedAction();
        if (originalValue == rejectActionStatusForClosedAction) {
            runnable.run();
        } else {
            repositoryProperties.setRejectActionStatusForClosedAction(rejectActionStatusForClosedAction);
            try {
                runnable.run();
            } finally {
                repositoryProperties.setRejectActionStatusForClosedAction(originalValue);
            }
        }
    }

    private Long createTargetAndAssignDs() {
        final Long dsId = testdataFactory.createDistributionSet().getId();
        testdataFactory.createTarget();
        assignDistributionSet(dsId, DEFAULT_CONTROLLER_ID);
        assertThat(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.PENDING);

        return deploymentManagement.findActiveActionsByTarget(DEFAULT_CONTROLLER_ID, PAGE).getContent().get(0).getId();
    }

    private Long createAndAssignDsAsDownloadOnly(final String dsName, final String defaultControllerId) {
        final DistributionSet ds = testdataFactory.createDistributionSet(dsName);
        final Long dsId = ds.getId();
        assignDistributionSet(dsId, defaultControllerId, DOWNLOAD_ONLY);
        assertThat(targetManagement.getByControllerID(defaultControllerId).get().getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);

        final Long id = deploymentManagement.findActiveActionsByTarget(defaultControllerId, PAGE).getContent().get(0).getId();
        assertThat(id).isNotNull();
        return id;
    }

    private Long assignDs(final Long dsId, final String defaultControllerId, final Action.ActionType actionType) {
        assignDistributionSet(dsId, defaultControllerId, actionType);
        assertThat(targetManagement.getByControllerID(defaultControllerId).get().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.PENDING);

        final Long id = deploymentManagement.findActiveActionsByTarget(defaultControllerId, PAGE).getContent().get(0)
                .getId();
        assertThat(id).isNotNull();
        return id;
    }

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

    private void assertActionStatus(
            final Long actionId, final String controllerId,
            final TargetUpdateStatus expectedTargetUpdateStatus, final Action.Status expectedActionActionStatus,
            final Action.Status expectedActionStatus, final boolean actionActive) {
        final TargetUpdateStatus targetStatus = targetManagement.getByControllerID(controllerId).get().getUpdateStatus();
        assertThat(targetStatus).isEqualTo(expectedTargetUpdateStatus);
        final Action action = deploymentManagement.findAction(actionId).get();
        assertThat(action.getStatus()).isEqualTo(expectedActionActionStatus);
        assertThat(action.isActive()).isEqualTo(actionActive);
        final List<ActionStatus> actionStatusList = controllerManagement.findActionStatusByAction(actionId, PAGE).getContent();
        assertThat(actionStatusList.get(actionStatusList.size() - 1).getStatus()).isEqualTo(expectedActionStatus);
        if (actionActive) {
            assertThat(controllerManagement.findActiveActionWithHighestWeight(controllerId).get().getId()).isEqualTo(actionId);
        }
    }

    private void createTargetType(String targetTypeName) {
        systemSecurityContext.runAsSystem(
                () -> targetTypeManagement.create(TargetTypeManagement.Create.builder().name(targetTypeName).build()));
    }

    private void addAttributeAndVerify(final String controllerId) {
        final Map<String, String> testData = new HashMap<>(1);
        testData.put("test1", "testdata1");
        controllerManagement.updateControllerAttributes(controllerId, testData, null);

        assertThat(targetManagement.getControllerAttributes(controllerId)).as("Controller Attributes are wrong")
                .isEqualTo(testData);
    }

    private void addSecondAttributeAndVerify(final String controllerId) {
        final Map<String, String> testData = new HashMap<>(2);
        testData.put("test2", "testdata20");
        controllerManagement.updateControllerAttributes(controllerId, testData, null);

        testData.put("test1", "testdata1");
        assertThat(targetManagement.getControllerAttributes(controllerId)).as("Controller Attributes are wrong")
                .isEqualTo(testData);
    }

    private void updateAttributeAndVerify(final String controllerId) {
        final Map<String, String> testData = new HashMap<>(2);
        testData.put("test1", "testdata12");

        controllerManagement.updateControllerAttributes(controllerId, testData, null);

        testData.put("test2", "testdata20");
        assertThat(targetManagement.getControllerAttributes(controllerId)).as("Controller Attributes are wrong")
                .isEqualTo(testData);
    }

    private void updateTargetAttributesWithUpdateModeRemove(final String controllerId) {
        final int previousSize = targetManagement.getControllerAttributes(controllerId).size();

        // update the attributes using update mode REMOVE
        final Map<String, String> removeAttributes = new HashMap<>();
        removeAttributes.put("k1", "foo");
        removeAttributes.put("k3", "bar");
        controllerManagement.updateControllerAttributes(controllerId, removeAttributes, UpdateMode.REMOVE);

        // verify attribute removal
        final Map<String, String> updatedAttributes = targetManagement.getControllerAttributes(controllerId);
        assertThat(updatedAttributes).hasSize(previousSize - 2);
        assertThat(updatedAttributes).doesNotContainKeys("k1", "k3");
    }

    private void updateTargetAttributesWithUpdateModeMerge(final String controllerId) {
        // get the current attributes
        final HashMap<String, String> attributes = new HashMap<>(targetManagement.getControllerAttributes(controllerId));

        // update the attributes using update mode MERGE
        final Map<String, String> mergeAttributes = new HashMap<>();
        mergeAttributes.put("k1", "v1_modified_again");
        mergeAttributes.put("k4", "v4");
        controllerManagement.updateControllerAttributes(controllerId, mergeAttributes, UpdateMode.MERGE);

        // verify attribute merge
        final Map<String, String> updatedAttributes = targetManagement.getControllerAttributes(controllerId);
        assertThat(updatedAttributes).hasSize(4);
        assertThat(updatedAttributes).containsAllEntriesOf(mergeAttributes);
        assertThat(updatedAttributes).containsEntry("k1", "v1_modified_again");
        attributes.keySet().forEach(assertThat(updatedAttributes)::containsKey);
    }

    private void updateTargetAttributesWithUpdateModeReplace(final String controllerId) {
        // get the current attributes
        final HashMap<String, String> attributes = new HashMap<>(targetManagement.getControllerAttributes(controllerId));

        // update the attributes using update mode REPLACE
        final Map<String, String> replacementAttributes = new HashMap<>();
        replacementAttributes.put("k1", "v1_modified");
        replacementAttributes.put("k2", "v2");
        replacementAttributes.put("k3", "v3");
        controllerManagement.updateControllerAttributes(controllerId, replacementAttributes, UpdateMode.REPLACE);

        // verify attribute replacement
        final Map<String, String> updatedAttributes = targetManagement.getControllerAttributes(controllerId);
        assertThat(updatedAttributes).hasSameSizeAs(replacementAttributes);
        assertThat(updatedAttributes).containsAllEntriesOf(replacementAttributes);
        assertThat(updatedAttributes).containsEntry("k1", "v1_modified");
        attributes.entrySet().forEach(assertThat(updatedAttributes)::doesNotContain);
    }

    private void updateTargetAttributesWithoutUpdateMode(final String controllerId) {
        // set the initial attributes
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("k0", "v0");
        attributes.put("k1", "v1");
        controllerManagement.updateControllerAttributes(controllerId, attributes, null);

        // verify initial attributes
        final Map<String, String> updatedAttributes = targetManagement.getControllerAttributes(controllerId);
        assertThat(updatedAttributes).hasSameSizeAs(attributes);
        assertThat(updatedAttributes).containsAllEntriesOf(attributes);
    }

    private void writeAttributes(final String controllerId, final int allowedAttributes, final String keyPrefix,
            final String valuePrefix) {
        final Map<String, String> testData = new HashMap<>(allowedAttributes);
        for (int i = 0; i < allowedAttributes; i++) {
            testData.put(keyPrefix + i, valuePrefix);
        }
        controllerManagement.updateControllerAttributes(controllerId, testData, null);
    }

    private void writeStatus(final Long actionId, final int allowedStatusEntries) {
        for (int i = 0; i < allowedStatusEntries; i++) {
            controllerManagement.addInformationalActionStatus(
                    entityFactory.actionStatus().create(actionId).status(Status.RUNNING).message("test" + i));
        }
    }

    private void finishDownloadOnlyUpdateAndSendUpdateActionStatus(final Long actionId, final Status status) {
        // finishing action
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(Status.DOWNLOADED));

        controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(status));
        assertThat(activeActionExistsForControllerId(DEFAULT_CONTROLLER_ID)).isFalse();
    }

    private void addUpdateActionStatus(final Long actionId, final String controllerId, final Status actionStatus) {
        controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(actionStatus));
        assertActionStatus(actionId, controllerId, TargetUpdateStatus.IN_SYNC, actionStatus, actionStatus, false);
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

    private void assertLastActionStatusCodeInAction(final Long actionId, final Integer expectedLastActionStatusCode) {
        final Optional<Action> action = actionRepository.findById(actionId).map(Action.class::cast);
        assertThat(action).isPresent();
        assertThat(action.get().getLastActionStatusCode()).isEqualTo(Optional.ofNullable(expectedLastActionStatusCode));
    }

    private void assertNoActiveActionsExistsForControllerId(final String controllerId) {
        assertThat(activeActionExistsForControllerId(controllerId)).isFalse();
    }

    private boolean activeActionExistsForControllerId(final String controllerId) {
        return actionRepository.exists(ActionSpecifications.byTargetControllerIdAndActive(controllerId, true));
    }
}
