/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.hawkbit.repository.ConfirmationManagement.CONFIRMATION_CODE_MSG_PREFIX;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.ddi.json.model.DdiActivateAutoConfirmation;
import org.eclipse.hawkbit.ddi.json.model.DdiConfirmationFeedback;
import org.eclipse.hawkbit.ddi.rest.api.DdiRootControllerRestApi;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TenantConfigurationCreatedEvent;
import org.eclipse.hawkbit.repository.jpa.repository.ActionStatusRepository;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Test confirmation base from the controller.
 * <p/>
 * Feature: Component Tests - Direct Device Integration API<br/>
 * Story: Confirmation Action Resource
 */
class DdiConfirmationBaseTest extends AbstractDDiApiIntegrationTest {

    private static final String DEFAULT_CONTROLLER_ID = "4747";

    /**
     * Forced deployment to a controller. Checks if the confirmation resource response payload for a given deployment is as expected.
     */
    @Test
    void verifyConfirmationReferencesInControllerBase(@Autowired ActionStatusRepository actionStatusRepository) throws Exception {
        enableConfirmationFlow();
        // Prepare test data
        final DistributionSet ds = testdataFactory.createDistributionSet("", true);
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true);
        final Artifact artifact = testdataFactory.createArtifact(nextBytes(ARTIFACT_SIZE), getOsModule(ds), "test1", ARTIFACT_SIZE);
        final Artifact artifactSignature = testdataFactory.createArtifact(
                nextBytes(ARTIFACT_SIZE), getOsModule(ds), "test1.signature", ARTIFACT_SIZE);

        final Target savedTarget = testdataFactory.createTarget(DdiConfirmationBaseTest.DEFAULT_CONTROLLER_ID);
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).isEmpty();

        final List<Target> targetsAssignedToDs = assignDistributionSet(
                ds.getId(), savedTarget.getControllerId(), Action.ActionType.FORCED).getAssignedEntity().stream()
                .map(Action::getTarget)
                .toList();
        implicitLock(ds);

        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).hasSize(1);

        final Action action = deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE).getContent().get(0);
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(1);

        assignDistributionSet(ds2, targetsAssignedToDs).getAssignedEntity();
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).hasSize(2);
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(2);

        final Action uaction = deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE).getContent().get(0);
        assertThat(uaction.getDistributionSet()).isEqualTo(ds);
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).hasSize(2);

        // Run test
        final long current = System.currentTimeMillis();
        final String expectedConfirmationBaseLink = String.format(
                "/%s/controller/v1/%s/confirmationBase/%d",
                AccessContext.tenant(), DEFAULT_CONTROLLER_ID, uaction.getId());

        performGet(CONTROLLER_BASE, MediaTypes.HAL_JSON, status().isOk(), AccessContext.tenant(), DEFAULT_CONTROLLER_ID)
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.confirmationBase.href", containsString(expectedConfirmationBaseLink)))
                .andExpect(jsonPath("$._links.deploymentBase.href").doesNotExist());

        assertThat(targetManagement.getByControllerId(DEFAULT_CONTROLLER_ID).getLastTargetQuery()).isGreaterThanOrEqualTo(current);
        assertThat(targetManagement.getByControllerId(DEFAULT_CONTROLLER_ID).getLastTargetQuery())
                .isLessThanOrEqualTo(System.currentTimeMillis());
        assertThat(actionStatusRepository.count()).isEqualTo(2);

        final DistributionSet findDistributionSetByAction = findDsByAction(action.getId()).orElseThrow();

        getAndVerifyConfirmationBasePayload(
                DEFAULT_CONTROLLER_ID, MediaType.APPLICATION_JSON, ds, artifact,
                artifactSignature, action.getId(),
                findFirstModuleByType(findDistributionSetByAction, osType).orElseThrow().getId(), "forced", "forced");

        // Retrieved is reported
        final Iterable<ActionStatus> actionStatus = deploymentManagement
                .findActionStatusByAction(uaction.getId(), PageRequest.of(0, 100, Sort.Direction.DESC, "id"));
        assertThat(actionStatus).hasSize(1)
                .allMatch(status -> status.getStatus() == Action.Status.WAIT_FOR_CONFIRMATION);
    }

    /**
     * Ensure that the deployment resource is available as CBOR
     */
    @Test
    void confirmationResourceCbor() throws Exception {
        enableConfirmationFlow();
        final Target target = testdataFactory.createTarget();
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("");

        final Long softwareModuleId = distributionSet.getModules().stream().findAny().orElseThrow().getId();
        testdataFactory.createArtifacts(softwareModuleId);

        assignDistributionSet(distributionSet.getId(), target.getName());

        final Action action = deploymentManagement.findActiveActionsByTarget(target.getControllerId(), PAGE).getContent().get(0);

        // get confirmation base
        performGet(CONFIRMATION_BASE_ACTION, MediaType.parseMediaType(DdiRootControllerRestApi.MEDIA_TYPE_APPLICATION_CBOR),
                status().isOk(), AccessContext.tenant(), target.getControllerId(), action.getId().toString());

        // get artifacts
        performGet(SOFTWARE_MODULE_ARTIFACTS, MediaType.parseMediaType(DdiRootControllerRestApi.MEDIA_TYPE_APPLICATION_CBOR),
                status().isOk(), AccessContext.tenant(), target.getControllerId(),
                String.valueOf(softwareModuleId));
    }

    /**
     * Ensure that the confirmation endpoint is not available.
     */
    @Test
    void confirmationEndpointNotExposed() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("988");
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));

        final String controllerId = savedTarget.getControllerId();
        final Action savedAction = deploymentManagement.findActiveActionsByTarget(controllerId, PAGE).getContent().get(0);

        mvc.perform(get(CONTROLLER_BASE, AccessContext.tenant(), controllerId).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.confirmationBase.href").doesNotExist());

        mvc.perform(get(CONFIRMATION_BASE_ACTION, AccessContext.tenant(), controllerId, savedAction.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    /**
     * Ensure that the deploymentBase endpoint is not available for action ins WFC state.
     */
    @Test
    void deploymentEndpointNotAccessibleForActionsWFC() throws Exception {
        enableConfirmationFlow();

        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("988");
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));

        final String controllerId = savedTarget.getControllerId();

        final Action savedAction = deploymentManagement.findActiveActionsByTarget(controllerId, PAGE).getContent().get(0);
        mvc.perform(get(CONTROLLER_BASE, AccessContext.tenant(), controllerId).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.confirmationBase.href").exists())
                .andExpect(jsonPath("$._links.deploymentBase.href").doesNotExist());

        mvc.perform(get(CONFIRMATION_BASE_ACTION, AccessContext.tenant(), controllerId, savedAction.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        mvc.perform(get(DEPLOYMENT_BASE, AccessContext.tenant(), controllerId, savedAction.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    /**
     * Ensure that the confirmation endpoints are still available after deactivating the confirmation flow.
     */
    @Test
    void verifyConfirmationBaseEndpointsArePresentAfterDisablingConfirmationFlow() throws Exception {
        enableConfirmationFlow();

        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("988");
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));

        final String controllerId = savedTarget.getControllerId();

        final Action savedAction = deploymentManagement.findActiveActionsByTarget(controllerId, PAGE).getContent().get(0);

        // disable confirmation flow
        disableConfirmationFlow();

        // confirmation base should still be exposed
        verifyActionInConfirmationBaseState(savedTarget.getControllerId(), savedAction.getId());

        // verify confirmation endpoint is still accessible
        sendConfirmationFeedback(
                savedTarget, savedAction, DdiConfirmationFeedback.Confirmation.DENIED, 20, "Action denied message.")
                .andExpect(status().isOk());

        // confirmation base should still be exposed
        verifyActionInConfirmationBaseState(savedTarget.getControllerId(), savedAction.getId());

        // verify confirmation endpoint is still accessible
        sendConfirmationFeedback(
                savedTarget, savedAction, DdiConfirmationFeedback.Confirmation.CONFIRMED, 10, "Action confirmed message.")
                .andExpect(status().isOk());

        // assert deployment link is exposed to the target
        verifyActionInDeploymentBaseState(controllerId, savedAction.getId());
    }

    /**
     * Controller sends a confirmed action state.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1), 
            @Expect(type = TargetPollEvent.class, count = 1),
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1) })
    void sendConfirmedActionStateFeedbackTest() throws Exception {
        enableConfirmationFlow();

        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("988");
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));

        String controllerId = savedTarget.getControllerId();

        final Action savedAction = deploymentManagement.findActiveActionsByTarget(controllerId, PAGE).getContent()
                .get(0);

        sendConfirmationFeedback(
                savedTarget, savedAction, DdiConfirmationFeedback.Confirmation.CONFIRMED, 10, "Action confirmed message.")
                .andExpect(status().isOk());

        // assert deployment link is exposed to the target
        verifyActionInDeploymentBaseState(controllerId, savedAction.getId());
    }

    /**
     * Confirmation base provides right values if auto-confirm not active.
     */
    @Test
    void getConfirmationBaseProvidesAutoConfirmStatusNotActive() throws Exception {
        enableConfirmationFlow();

        final String controllerId = testdataFactory.createTarget("989").getControllerId();
        assignDistributionSet(testdataFactory.createDistributionSet("").getId(), controllerId);
        final long actionId = deploymentManagement.findActiveActionsByTarget(controllerId, PAGE).getContent().get(0).getId();

        final String confirmationBaseActionLink = String.format(
                "/%s/controller/v1/%s/confirmationBase/%d",
                AccessContext.tenant(), controllerId, actionId);

        final String activateAutoConfLink = String.format(
                "/%s/controller/v1/%s/confirmationBase/activateAutoConfirm",
                AccessContext.tenant(), controllerId);

        mvc.perform(get(CONFIRMATION_BASE, AccessContext.tenant(), controllerId).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("autoConfirm.active", equalTo(Boolean.FALSE)))
                .andExpect(jsonPath("$._links.confirmationBase.href", containsString(confirmationBaseActionLink)))
                .andExpect(jsonPath("$._links.activateAutoConfirm.href", containsString(activateAutoConfLink)))
                .andExpect(jsonPath("$._links.deactivateAutoConfirm").doesNotExist());
    }

    /**
     * Confirmation base provides right values if auto-confirm is active.
     */
    @ParameterizedTest
    @MethodSource("possibleActiveStates")
    void getConfirmationBaseProvidesAutoConfirmStatusActive(final String initiator, final String remark) throws Exception {
        final String controllerId = testdataFactory.createTarget("988").getControllerId();

        confirmationManagement.activateAutoConfirmation(controllerId, initiator, remark);

        final String deactivateAutoConfLink = String.format(
                "/%s/controller/v1/%s/confirmationBase/deactivateAutoConfirm",
                AccessContext.tenant(), controllerId);

        mvc.perform(get(CONFIRMATION_BASE, AccessContext.tenant(), controllerId).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("autoConfirm.active", equalTo(Boolean.TRUE)))
                .andExpect(initiator == null
                        ? jsonPath("autoConfirm.initiator").doesNotExist()
                        : jsonPath("autoConfirm.initiator", equalTo(initiator)))
                .andExpect(remark == null
                        ? jsonPath("autoConfirm.remark").doesNotExist()
                        : jsonPath("autoConfirm.remark", equalTo(remark)))
                .andExpect(jsonPath("$._links.deactivateAutoConfirm.href", containsString(deactivateAutoConfLink)))
                .andExpect(jsonPath("$._links.activateAutoConfirm").doesNotExist());
    }

    /**
     * Verify auto-confirm activation is handled correctly.
     */
    @ParameterizedTest
    @MethodSource("possibleActiveStates")
    void activateAutoConfirmation(final String initiator, final String remark) throws Exception {
        final String controllerId = testdataFactory.createTarget("988").getControllerId();

        final DdiActivateAutoConfirmation body = new DdiActivateAutoConfirmation(initiator, remark);

        mvc.perform(post(ACTIVATE_AUTO_CONFIRM, AccessContext.tenant(), controllerId)
                        .content(getMapper().writeValueAsString(body)).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        assertThat(confirmationManagement.findStatus(controllerId)).hasValueSatisfying(status -> {
            assertThat(status.getInitiator()).isEqualTo(initiator);
            assertThat(status.getRemark()).isEqualTo(remark);
            assertThat(status.getCreatedBy()).isEqualTo("bumlux");
        });
    }

    /**
     * Verify auto-confirm deactivation is handled correctly.
     */
    @Test
    void deactivateAutoConfirmation() throws Exception {
        final String controllerId = testdataFactory.createTarget("988").getControllerId();

        confirmationManagement.activateAutoConfirmation(controllerId, null, null);

        mvc.perform(post(DEACTIVATE_AUTO_CONFIRM, AccessContext.tenant(), controllerId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        assertThat(confirmationManagement.findStatus(controllerId)).isEmpty();
    }

    /**
     * Controller sends a denied action state.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1),
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1) })
    void sendDeniedActionStateFeedbackTest() throws Exception {
        enableConfirmationFlow();

        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("989");
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));
        String controllerId = savedTarget.getControllerId();
        final Action savedAction = deploymentManagement.findActiveActionsByTarget(controllerId, PAGE).getContent().get(0);

        sendConfirmationFeedback(
                savedTarget, savedAction, DdiConfirmationFeedback.Confirmation.DENIED, 10, "Action denied message.")
                .andExpect(status().isOk());

        // asserts that deployment link is not available
        final String expectedConfirmationBaseLink = String.format(
                "/%s/controller/v1/%s/confirmationBase/%d",
                AccessContext.tenant(), controllerId, savedAction.getId());

        mvc.perform(get(CONTROLLER_BASE, AccessContext.tenant(), controllerId).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.deploymentBase.href").doesNotExist())
                .andExpect(jsonPath("$._links.confirmationBase.href", containsString(expectedConfirmationBaseLink)));

        mvc.perform(get(DEPLOYMENT_BASE, AccessContext.tenant(), controllerId, savedAction.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    /**
     * Test to verify that only a specific count of messages are returned based on the input actionHistory for getControllerDeploymentActionFeedback endpoint.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1) })
    void testActionHistoryCount() throws Exception {
        enableConfirmationFlow();

        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("990");
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));

        final String controllerId = savedTarget.getControllerId();

        final Action savedAction = deploymentManagement.findActiveActionsByTarget(controllerId, PAGE).getContent().get(0);
        final String confirmedMessage = "Action confirmed message.";
        final Integer confirmedCode = 10;
        sendConfirmationFeedback(
                savedTarget, savedAction, DdiConfirmationFeedback.Confirmation.CONFIRMED, confirmedCode, confirmedMessage)
                .andExpect(status().isOk());

        // confirmationBase not available in RUNNING state anymore
        mvc.perform(get(CONFIRMATION_BASE_ACTION, AccessContext.tenant(), savedTarget.getControllerId(),
                        savedAction.getId()).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // assert confirmed message against deploymentBase endpoint
        // this call will update the action due to retrieved action status update
        mvc.perform(get(DEPLOYMENT_BASE + "?actionHistory=2", AccessContext.tenant(), savedTarget.getControllerId(),
                        savedAction.getId()).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionHistory.messages", hasItem(containsString(confirmedMessage))))
                .andExpect(jsonPath("$.actionHistory.messages",
                        hasItem(containsString(String.format(CONFIRMATION_CODE_MSG_PREFIX, confirmedCode)))));
    }

    private static Stream<Arguments> possibleActiveStates() {
        return Stream.of(
                Arguments.of("someInitiator", "someRemark"),
                Arguments.of(null, "someRemark"),
                Arguments.of("someInitiator", null),
                Arguments.of(null, null));
    }

    private void verifyActionInDeploymentBaseState(final String controllerId, final long actionId) throws Exception {
        final String expectedDeploymentBaseLink = String.format(
                "/%s/controller/v1/%s/deploymentBase/%d",
                AccessContext.tenant(), controllerId, actionId);

        mvc.perform(get(CONTROLLER_BASE, AccessContext.tenant(), controllerId).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.deploymentBase.href", containsString(expectedDeploymentBaseLink)))
                .andExpect(jsonPath("$._links.confirmationBase.href").doesNotExist());

        // assert that deployment endpoint is working
        mvc.perform(get(expectedDeploymentBaseLink).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    private void verifyActionInConfirmationBaseState(final String controllerId, final long actionId) throws Exception {
        mvc.perform(get(CONTROLLER_BASE, AccessContext.tenant(), controllerId).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.confirmationBase.href").exists())
                .andExpect(jsonPath("$._links.deploymentBase.href").doesNotExist());

        mvc.perform(get(CONFIRMATION_BASE_ACTION, AccessContext.tenant(), controllerId, actionId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        mvc.perform(get(DEPLOYMENT_BASE, AccessContext.tenant(), controllerId, actionId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    private ResultActions sendConfirmationFeedback(
            final Target target, final Action action,
            final DdiConfirmationFeedback.Confirmation confirmation, Integer code, String message) throws Exception {
        if (message == null) {
            message = randomString(1000);
        }

        final String feedback = getJsonConfirmationFeedback(confirmation, code, Collections.singletonList(message));
        return mvc.perform(
                post(CONFIRMATION_FEEDBACK, AccessContext.tenant(), target.getControllerId(), action.getId())
                        .content(feedback).contentType(MediaType.APPLICATION_JSON));
    }
}