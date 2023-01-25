/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.eclipse.hawkbit.ddi.json.model.DdiActivateAutoConfirmation;
import org.eclipse.hawkbit.ddi.json.model.DdiConfirmationFeedback;
import org.eclipse.hawkbit.ddi.rest.api.DdiRestConstants;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TenantConfigurationCreatedEvent;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.hawkbit.repository.jpa.JpaConfirmationManagement.CONFIRMATION_CODE_MSG_PREFIX;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test confirmation base from the controller.
 */
@Feature("Component Tests - Direct Device Integration API")
@Story("Confirmation Action Resource")
public class DdiConfirmationBaseTest extends AbstractDDiApiIntegrationTest {

    private static final String DEFAULT_CONTROLLER_ID = "4747";

    @Test
    @Description("Forced deployment to a controller. Checks if the confirmation resource response payload for a given"
            + " deployment is as expected.")
    public void verifyConfirmationReferencesInControllerBase() throws Exception {
        enableConfirmationFlow();
        // Prepare test data
        final DistributionSet ds = testdataFactory.createDistributionSet("", true);
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true);
        final Artifact artifact = testdataFactory.createArtifact(RandomUtils.nextBytes(ARTIFACT_SIZE), getOsModule(ds),
                "test1", ARTIFACT_SIZE);
        final Artifact artifactSignature = testdataFactory.createArtifact(RandomUtils.nextBytes(ARTIFACT_SIZE),
                getOsModule(ds), "test1.signature", ARTIFACT_SIZE);

        final Target savedTarget = testdataFactory.createTarget(DdiConfirmationBaseTest.DEFAULT_CONTROLLER_ID);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).isEmpty();

        final List<Target> targetsAssignedToDs = assignDistributionSet(ds.getId(), savedTarget.getControllerId(),
                Action.ActionType.FORCED).getAssignedEntity().stream().map(Action::getTarget)
                        .collect(Collectors.toList());

        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(1);

        final Action action = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(1);

        assignDistributionSet(ds2, targetsAssignedToDs).getAssignedEntity();
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(2);
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(2);

        final Action uaction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);
        assertThat(uaction.getDistributionSet()).isEqualTo(ds);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(2);

        // Run test
        final long current = System.currentTimeMillis();
        final String expectedConfirmationBaseLink = String.format("/%s/controller/v1/%s/confirmationBase/%d",
                tenantAware.getCurrentTenant(), DEFAULT_CONTROLLER_ID, uaction.getId());

        performGet(CONTROLLER_BASE, MediaTypes.HAL_JSON, status().isOk(), tenantAware.getCurrentTenant(),
                DEFAULT_CONTROLLER_ID)
                        .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                        .andExpect(jsonPath("$._links.confirmationBase.href",
                                containsString(expectedConfirmationBaseLink)))
                        .andExpect(jsonPath("$._links.deploymentBase.href").doesNotExist());

        assertThat(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);
        assertThat(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get().getLastTargetQuery())
                .isLessThanOrEqualTo(System.currentTimeMillis());
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(2);

        final DistributionSet findDistributionSetByAction = distributionSetManagement.getByAction(action.getId()).get();

        getAndVerifyConfirmationBasePayload(DEFAULT_CONTROLLER_ID, MediaType.APPLICATION_JSON, ds, artifact,
                artifactSignature, action.getId(),
                findDistributionSetByAction.findFirstModuleByType(osType).get().getId(), "forced", "forced");

        // Retrieved is reported
        final Iterable<ActionStatus> actionStatus = deploymentManagement
                .findActionStatusByAction(PageRequest.of(0, 100, Sort.Direction.DESC, "id"), uaction.getId());
        assertThat(actionStatus).hasSize(1)
                .allMatch(status -> status.getStatus() == Action.Status.WAIT_FOR_CONFIRMATION);
    }

    @Test
    @Description("Ensure that the deployment resource is available as CBOR")
    public void confirmationResourceCbor() throws Exception {
        enableConfirmationFlow();
        final Target target = testdataFactory.createTarget();
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("");

        assignDistributionSet(distributionSet.getId(), target.getName());
        final Action action = deploymentManagement.findActiveActionsByTarget(PAGE, target.getControllerId())
                .getContent().get(0);

        // get confirmation base
        performGet(CONFIRMATION_BASE_ACTION, MediaType.parseMediaType(DdiRestConstants.MEDIA_TYPE_CBOR),
                status().isOk(), tenantAware.getCurrentTenant(), target.getControllerId(), action.getId().toString());

        final Long softwareModuleId = distributionSet.getModules().stream().findAny().get().getId();
        testdataFactory.createArtifacts(softwareModuleId);
        // get artifacts
        performGet(SOFTWARE_MODULE_ARTIFACTS, MediaType.parseMediaType(DdiRestConstants.MEDIA_TYPE_CBOR),
                status().isOk(), tenantAware.getCurrentTenant(), target.getControllerId(),
                String.valueOf(softwareModuleId));

    }

    @Test
    @Description("Ensure that the confirmation endpoint is not available.")
    public void confirmationEndpointNotExposed() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("988");
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));

        final String controllerId = savedTarget.getControllerId();

        final Action savedAction = deploymentManagement.findActiveActionsByTarget(PAGE, controllerId).getContent()
                .get(0);

        mvc.perform(
                get(CONTROLLER_BASE, tenantAware.getCurrentTenant(), controllerId).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$._links.confirmationBase.href").doesNotExist());

        mvc.perform(get(CONFIRMATION_BASE_ACTION, tenantAware.getCurrentTenant(), controllerId, savedAction.getId())
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Ensure that the deploymentBase endpoint is not available for action ins WFC state.")
    public void deploymentEndpointNotAccessibleForActionsWFC() throws Exception {
        enableConfirmationFlow();

        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("988");
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));

        final String controllerId = savedTarget.getControllerId();

        final Action savedAction = deploymentManagement.findActiveActionsByTarget(PAGE, controllerId).getContent()
                .get(0);

        mvc.perform(
                get(CONTROLLER_BASE, tenantAware.getCurrentTenant(), controllerId).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$._links.confirmationBase.href").exists())
                .andExpect(jsonPath("$._links.deploymentBase.href").doesNotExist());

        mvc.perform(get(CONFIRMATION_BASE_ACTION, tenantAware.getCurrentTenant(), controllerId, savedAction.getId())
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        mvc.perform(get(DEPLOYMENT_BASE, tenantAware.getCurrentTenant(), controllerId, savedAction.getId())
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Ensure that the confirmation endpoints are still available after deactivating the confirmation flow.")
    public void verifyConfirmationBaseEndpointsArePresentAfterDisablingConfirmationFlow() throws Exception {
        enableConfirmationFlow();

        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("988");
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));

        final String controllerId = savedTarget.getControllerId();

        final Action savedAction = deploymentManagement.findActiveActionsByTarget(PAGE, controllerId).getContent()
                .get(0);

        // disable confirmation flow
        disableConfirmationFlow();

        // confirmation base should still be exposed
        verifyActionInConfirmationBaseState(savedTarget.getControllerId(), savedAction.getId());

        // verify confirmation endpoint is still accessible
        sendConfirmationFeedback(savedTarget, savedAction, DdiConfirmationFeedback.Confirmation.DENIED, 20,
              "Action denied message.").andExpect(status().isOk());

        // confirmation base should still be exposed
        verifyActionInConfirmationBaseState(savedTarget.getControllerId(), savedAction.getId());

        // verify confirmation endpoint is still accessible
        sendConfirmationFeedback(savedTarget, savedAction, DdiConfirmationFeedback.Confirmation.CONFIRMED, 10,
              "Action confirmed message.").andExpect(status().isOk());

        // assert deployment link is exposed to the target
        verifyActionInDeploymentBaseState(controllerId, savedAction.getId());
    }

    @Test
    @Description("Controller sends a confirmed action state.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = TargetUpdatedEvent.class, count = 1), @Expect(type = TargetPollEvent.class, count = 1),
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1) })
    void sendConfirmedActionStateFeedbackTest() throws Exception {
        enableConfirmationFlow();

        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("988");
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));

        String controllerId = savedTarget.getControllerId();

        final Action savedAction = deploymentManagement.findActiveActionsByTarget(PAGE, controllerId).getContent()
                .get(0);

        sendConfirmationFeedback(savedTarget, savedAction, DdiConfirmationFeedback.Confirmation.CONFIRMED, 10,
                "Action confirmed message.").andExpect(status().isOk());

        // assert deployment link is exposed to the target
        verifyActionInDeploymentBaseState(controllerId, savedAction.getId());
    }

    @Step
    private void verifyActionInDeploymentBaseState(final String controllerId, final long actionId) throws Exception {
        final String expectedDeploymentBaseLink = String.format("/%s/controller/v1/%s/deploymentBase/%d",
              tenantAware.getCurrentTenant(), controllerId, actionId);

        mvc.perform(
                    get(CONTROLLER_BASE, tenantAware.getCurrentTenant(), controllerId).accept(MediaType.APPLICATION_JSON))
              .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
              .andExpect(jsonPath("$._links.deploymentBase.href", containsString(expectedDeploymentBaseLink)))
              .andExpect(jsonPath("$._links.confirmationBase.href").doesNotExist());

        // assert that deployment endpoint is working
        mvc.perform(get(expectedDeploymentBaseLink).accept(MediaType.APPLICATION_JSON))
              .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
    }

    @Step
    private void verifyActionInConfirmationBaseState(final String controllerId, final long actionId) throws Exception {
        mvc.perform(
                get(CONTROLLER_BASE, tenantAware.getCurrentTenant(), controllerId).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$._links.confirmationBase.href").exists())
                .andExpect(jsonPath("$._links.deploymentBase.href").doesNotExist());

        mvc.perform(get(CONFIRMATION_BASE_ACTION, tenantAware.getCurrentTenant(), controllerId, actionId)
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        mvc.perform(get(DEPLOYMENT_BASE, tenantAware.getCurrentTenant(), controllerId, actionId)
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Confirmation base provides right values if auto-confirm not active.")
    void getConfirmationBaseProvidesAutoConfirmStatusNotActive() throws Exception {
        enableConfirmationFlow();

        final String controllerId = testdataFactory.createTarget("989").getControllerId();
        assignDistributionSet(testdataFactory.createDistributionSet("").getId(), controllerId);
        final long actionId = deploymentManagement.findActiveActionsByTarget(PAGE, controllerId).getContent().get(0)
                .getId();

        final String confirmationBaseActionLink = String.format("/%s/controller/v1/%s/confirmationBase/%d",
                tenantAware.getCurrentTenant(), controllerId, actionId);

        final String activateAutoConfLink = String.format("/%s/controller/v1/%s/confirmationBase/activateAutoConfirm",
                tenantAware.getCurrentTenant(), controllerId);

        mvc.perform(
                get(CONFIRMATION_BASE, tenantAware.getCurrentTenant(), controllerId).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("autoConfirm.active", equalTo(Boolean.FALSE)))
                .andExpect(jsonPath("$._links.confirmationBase.href", containsString(confirmationBaseActionLink)))
                .andExpect(jsonPath("$._links.activateAutoConfirm.href", containsString(activateAutoConfLink)))
                .andExpect(jsonPath("$._links.deactivateAutoConfirm").doesNotExist());
    }

    @ParameterizedTest
    @MethodSource("possibleActiveStates")
    @Description("Confirmation base provides right values if auto-confirm is active.")
    void getConfirmationBaseProvidesAutoConfirmStatusActive(final String initiator, final String remark)
            throws Exception {
        final String controllerId = testdataFactory.createTarget("988").getControllerId();

        confirmationManagement.activateAutoConfirmation(controllerId, initiator, remark);

        final String deactivateAutoConfLink = String.format(
                "/%s/controller/v1/%s/confirmationBase/deactivateAutoConfirm", tenantAware.getCurrentTenant(),
                controllerId);

        mvc.perform(
                get(CONFIRMATION_BASE, tenantAware.getCurrentTenant(), controllerId).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("autoConfirm.active", equalTo(Boolean.TRUE)))
                .andExpect(initiator == null ? jsonPath("autoConfirm.initiator").doesNotExist()
                        : jsonPath("autoConfirm.initiator", equalTo(initiator)))
                .andExpect(remark == null ? jsonPath("autoConfirm.remark").doesNotExist()
                        : jsonPath("autoConfirm.remark", equalTo(remark)))
                .andExpect(jsonPath("$._links.deactivateAutoConfirm.href", containsString(deactivateAutoConfLink)))
                .andExpect(jsonPath("$._links.activateAutoConfirm").doesNotExist());
    }

    private static Stream<Arguments> possibleActiveStates() {
        return Stream.of(Arguments.of("someInitiator", "someRemark"), Arguments.of(null, "someRemark"),
                Arguments.of("someInitiator", null), Arguments.of(null, null));
    }

    @ParameterizedTest
    @MethodSource("possibleActiveStates")
    @Description("Verify auto-confirm activation is handled correctly.")
    void activateAutoConfirmation(final String initiator, final String remark) throws Exception {
        final String controllerId = testdataFactory.createTarget("988").getControllerId();

        final DdiActivateAutoConfirmation body = new DdiActivateAutoConfirmation(initiator, remark);

        mvc.perform(post(ACTIVATE_AUTO_CONFIRM, tenantAware.getCurrentTenant(), controllerId)
                .content(getMapper().writeValueAsString(body)).contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        assertThat(confirmationManagement.getStatus(controllerId)).hasValueSatisfying(status -> {
            assertThat(status.getInitiator()).isEqualTo(initiator);
            assertThat(status.getRemark()).isEqualTo(remark);
            assertThat(status.getCreatedBy()).isEqualTo("bumlux");
        });
    }

    @Test
    @Description("Verify auto-confirm deactivation is handled correctly.")
    void deactivateAutoConfirmation() throws Exception {
        final String controllerId = testdataFactory.createTarget("988").getControllerId();

        confirmationManagement.activateAutoConfirmation(controllerId, null, null);

        mvc.perform(post(DEACTIVATE_AUTO_CONFIRM, tenantAware.getCurrentTenant(), controllerId))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        assertThat(confirmationManagement.getStatus(controllerId)).isEmpty();
    }

    @Test
    @Description("Controller sends a denied action state.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1),
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1) })
    void sendDeniedActionStateFeedbackTest() throws Exception {
        enableConfirmationFlow();

        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("989");
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));
        String controllerId = savedTarget.getControllerId();
        final Action savedAction = deploymentManagement.findActiveActionsByTarget(PAGE, controllerId).getContent()
                .get(0);

        sendConfirmationFeedback(savedTarget, savedAction, DdiConfirmationFeedback.Confirmation.DENIED, 10,
                "Action denied message.").andExpect(status().isOk());

        // asserts that deployment link is not available
        final String expectedConfirmationBaseLink = String.format("/%s/controller/v1/%s/confirmationBase/%d",
                tenantAware.getCurrentTenant(), controllerId, savedAction.getId());

        mvc.perform(
                get(CONTROLLER_BASE, tenantAware.getCurrentTenant(), controllerId).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$._links.deploymentBase.href").doesNotExist())
                .andExpect(jsonPath("$._links.confirmationBase.href", containsString(expectedConfirmationBaseLink)));

        mvc.perform(get(DEPLOYMENT_BASE, tenantAware.getCurrentTenant(), controllerId, savedAction.getId())
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    private ResultActions sendConfirmationFeedback(final Target target, final Action action,
            final DdiConfirmationFeedback.Confirmation confirmation, Integer code, String message) throws Exception {

        if (message == null) {
            message = RandomStringUtils.randomAlphanumeric(1000);
        }

        final String feedback = getJsonConfirmationFeedback(confirmation, code, Collections.singletonList(message));
        return mvc.perform(
                post(CONFIRMATION_FEEDBACK, tenantAware.getCurrentTenant(), target.getControllerId(), action.getId())
                        .content(feedback).contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @Description("Test to verify that only a specific count of messages are returned based on the input actionHistory for getControllerDeploymentActionFeedback endpoint.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1) })
    void testActionHistoryCount() throws Exception {
        enableConfirmationFlow();

        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("990");
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));

        String controllerId = savedTarget.getControllerId();

        final Action savedAction = deploymentManagement.findActiveActionsByTarget(PAGE, controllerId).getContent()
                .get(0);
        final String CONFIRMED_MESSAGE = "Action confirmed message.";
        final Integer CONFIRMED_CODE = 10;
        sendConfirmationFeedback(savedTarget, savedAction, DdiConfirmationFeedback.Confirmation.CONFIRMED,
                CONFIRMED_CODE, CONFIRMED_MESSAGE).andExpect(status().isOk());

        // confirmationBase not available in RUNNING state anymore
        mvc.perform(get(CONFIRMATION_BASE_ACTION, tenantAware.getCurrentTenant(), savedTarget.getControllerId(),
                savedAction.getId()).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // assert confirmed message against deploymentBase endpoint
        // this call will update the action due to retrieved action status update
        mvc.perform(
                get(DEPLOYMENT_BASE + "?actionHistory=2", tenantAware.getCurrentTenant(), savedTarget.getControllerId(),
                        savedAction.getId()).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.actionHistory.messages", hasItem(containsString(CONFIRMED_MESSAGE))))
                .andExpect(jsonPath("$.actionHistory.messages",
                        hasItem(containsString(String.format(CONFIRMATION_CODE_MSG_PREFIX, CONFIRMED_CODE)))));
    }

}
