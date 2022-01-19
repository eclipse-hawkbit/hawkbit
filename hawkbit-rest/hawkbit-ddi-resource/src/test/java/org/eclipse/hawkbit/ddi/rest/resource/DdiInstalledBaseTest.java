/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomUtils;
import org.eclipse.hawkbit.ddi.rest.api.DdiRestConstants;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAttributesRequestedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test installed base from the controller.
 */
@Feature("Component Tests - Direct Device Integration API")
@Story("Installed Base Resource")
public class DdiInstalledBaseTest extends AbstractDDiApiIntegrationTest {

    private static final int ARTIFACT_SIZE = 5 * 1024;
    private static final String CONTROLLER_ID = "4715";

    @Test
    @Description("Ensure that the installed base resource is available as CBOR")
    public void installedBaseResourceCbor() throws Exception {
        final Target target = testdataFactory.createTarget();
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        final Long actionId = getFirstAssignedActionId(assignDistributionSet(ds, target));
        postDeploymentFeedback(target.getControllerId(), actionId,
                JsonBuilder.deploymentActionFeedback(actionId.toString(), "closed"), status().isOk());

        // get installed base
        performGet(INSTALLED_BASE, MediaType.parseMediaType(DdiRestConstants.MEDIA_TYPE_CBOR), status().isOk(),
                tenantAware.getCurrentTenant(), target.getControllerId(), actionId.toString());

        final Long softwareModuleId = ds.getModules().stream().findAny().get().getId();
        testdataFactory.createArtifacts(softwareModuleId);
        // get artifacts
        performGet(SOFTWARE_MODULE_ARTIFACTS, MediaType.parseMediaType(DdiRestConstants.MEDIA_TYPE_CBOR),
                status().isOk(), tenantAware.getCurrentTenant(), target.getControllerId(),
                String.valueOf(softwareModuleId));
    }

    @Test
    @Description("Test several deployments to a controller. Checks that action is represented as installedBase after installation.")
    public void deploymentSeveralActionsInInstalledBase() throws Exception {
        // Prepare test data
        final Target target = createTargetAndAssertNoActiveActions();

        final DistributionSet ds1 = testdataFactory.createDistributionSet("1", true);
        final Artifact artifact1 = testdataFactory.createArtifact(RandomUtils.nextBytes(ARTIFACT_SIZE),
                getOsModule(ds1), "test1", ARTIFACT_SIZE);
        final Artifact artifactSignature1 = testdataFactory.createArtifact(RandomUtils.nextBytes(ARTIFACT_SIZE),
                getOsModule(ds1), "test1.signature", ARTIFACT_SIZE);

        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true);
        final Artifact artifact2 = testdataFactory.createArtifact(RandomUtils.nextBytes(ARTIFACT_SIZE),
                getOsModule(ds2), "test2", ARTIFACT_SIZE);
        final Artifact artifactSignature2 = testdataFactory.createArtifact(RandomUtils.nextBytes(ARTIFACT_SIZE),
                getOsModule(ds2), "test2.signature", ARTIFACT_SIZE);

        // Run test with 1st action
        final Long actionId1 = getFirstAssignedActionId(
                assignDistributionSet(ds1.getId(), target.getControllerId(), Action.ActionType.SOFT));
        performGet(CONTROLLER_BASE, MediaTypes.HAL_JSON, status().isOk(), tenantAware.getCurrentTenant(), CONTROLLER_ID)
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.installedBase.href").doesNotExist())
                .andExpect(jsonPath("$._links.deploymentBase.href",
                        startsWith(deploymentBaseLink(CONTROLLER_ID, actionId1.toString()))));

        getAndVerifyDeploymentBasePayload(CONTROLLER_ID, MediaType.APPLICATION_JSON, ds1, artifact1, artifactSignature1,
                actionId1, ds1.findFirstModuleByType(osType).get().getId(), Action.ActionType.SOFT);

        postDeploymentFeedback(target.getControllerId(), actionId1,
                JsonBuilder.deploymentActionFeedback(actionId1.toString(), "closed", "success", "Closed"),
                status().isOk());

        getAndVerifyInstalledBasePayload(CONTROLLER_ID, MediaType.APPLICATION_JSON, ds1, artifact1, artifactSignature1,
                actionId1, ds1.findFirstModuleByType(osType).get().getId(), Action.ActionType.SOFT);

        // Run test with 2nd action
        final Long actionId2 = getFirstAssignedActionId(
                assignDistributionSet(ds2.getId(), target.getControllerId(), Action.ActionType.FORCED));
        performGet(CONTROLLER_BASE, MediaTypes.HAL_JSON, status().isOk(), tenantAware.getCurrentTenant(), CONTROLLER_ID)
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.installedBase.href",
                        startsWith(installedBaseLink(CONTROLLER_ID, actionId1.toString()))))
                .andExpect(jsonPath("$._links.deploymentBase.href",
                        startsWith(deploymentBaseLink(CONTROLLER_ID, actionId2.toString()))));

        getAndVerifyDeploymentBasePayload(CONTROLLER_ID, MediaType.APPLICATION_JSON, ds2, artifact2, artifactSignature2,
                actionId2, ds2.findFirstModuleByType(osType).get().getId(), Action.ActionType.FORCED);

        postDeploymentFeedback(target.getControllerId(), actionId2,
                JsonBuilder.deploymentActionFeedback(actionId2.toString(), "closed", "success", "Closed"),
                status().isOk());

        getAndVerifyInstalledBasePayload(CONTROLLER_ID, MediaType.APPLICATION_JSON, ds2, artifact2, artifactSignature2,
                actionId2, ds2.findFirstModuleByType(osType).get().getId(), Action.ActionType.FORCED);

        performGet(CONTROLLER_BASE, MediaTypes.HAL_JSON, status().isOk(), tenantAware.getCurrentTenant(), CONTROLLER_ID)
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.installedBase.href",
                        startsWith(installedBaseLink(CONTROLLER_ID, actionId2.toString()))))
                .andExpect(jsonPath("$._links.deploymentBase.href").doesNotExist());
    }

    @Test
    @Description("Test open deployment to a controller. Checks that installedBase returns 404 for a pending action.")
    public void installedBaseReturns404ForPendingAction() throws Exception {
        // Prepare test data
        final Target target = createTargetAndAssertNoActiveActions();
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        final Long actionId = getFirstAssignedActionId(assignDistributionSet(ds, target));

        performGet(CONTROLLER_BASE, MediaTypes.HAL_JSON, status().isOk(), tenantAware.getCurrentTenant(), CONTROLLER_ID)
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.installedBase.href").doesNotExist())
                .andExpect(jsonPath("$._links.deploymentBase.href",
                        startsWith(deploymentBaseLink(CONTROLLER_ID, actionId.toString()))));

        performGet(DEPLOYMENT_BASE, MediaType.APPLICATION_JSON, status().isOk(), tenantAware.getCurrentTenant(),
                target.getControllerId(), actionId.toString());

        mvc.perform(MockMvcRequestBuilders.get(INSTALLED_BASE, tenantAware.getCurrentTenant(), target.getControllerId(),
                actionId.toString())).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());
    }

    @Test
    @Description("Ensures that artifacts are found, after the action was already closed.")
    public void artifactsOfInstalledActionExist() throws Exception {
        final Target target = createTargetAndAssertNoActiveActions();
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        final Long actionId = getFirstAssignedActionId(assignDistributionSet(ds, target));

        postDeploymentFeedback(target.getControllerId(), actionId,
                JsonBuilder.deploymentActionFeedback(actionId.toString(), "closed"), status().isOk());

        final Long softwareModuleId = ds.getModules().stream().findAny().get().getId();
        performGet(SOFTWARE_MODULE_ARTIFACTS, MediaType.APPLICATION_JSON, status().isOk(),
                tenantAware.getCurrentTenant(), target.getControllerId(), softwareModuleId.toString())
                        .andExpect(jsonPath("$", hasSize(0)));

        testdataFactory.createArtifacts(softwareModuleId);

        performGet(SOFTWARE_MODULE_ARTIFACTS, MediaType.APPLICATION_JSON, status().isOk(),
                tenantAware.getCurrentTenant(), target.getControllerId(), softwareModuleId.toString())
                        .andExpect(jsonPath("$", hasSize(3)))
                        .andExpect(jsonPath("$.[?(@.filename=='filename0')]", hasSize(1)))
                        .andExpect(jsonPath("$.[?(@.filename=='filename1')]", hasSize(1)))
                        .andExpect(jsonPath("$.[?(@.filename=='filename2')]", hasSize(1)));

    }

    private static Stream<Action.ActionType> actionTypeForDeployment() {
        return Stream.of(Action.ActionType.SOFT, Action.ActionType.FORCED);
    }

    @ParameterizedTest
    @MethodSource("org.eclipse.hawkbit.ddi.rest.resource.DdiInstalledBaseTest#actionTypeForDeployment")
    @Description("Test forced deployment to a controller. Checks that action is represented as installedBase after installation.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 2),
            @Expect(type = TargetPollEvent.class, count = 1) })
    public void deploymentActionInInstalledBase(final Action.ActionType actionType) throws Exception {
        // Prepare test data
        final Target target = createTargetAndAssertNoActiveActions();
        final DistributionSet ds = testdataFactory.createDistributionSet("", true);
        final Artifact artifact = testdataFactory.createArtifact(RandomUtils.nextBytes(ARTIFACT_SIZE), getOsModule(ds),
                "test1", ARTIFACT_SIZE);
        final Artifact artifactSignature = testdataFactory.createArtifact(RandomUtils.nextBytes(ARTIFACT_SIZE),
                getOsModule(ds), "test1.signature", ARTIFACT_SIZE);
        final Long actionId = getFirstAssignedActionId(
                assignDistributionSet(ds.getId(), target.getControllerId(), actionType));

        postDeploymentFeedback(target.getControllerId(), actionId,
                JsonBuilder.deploymentActionFeedback(actionId.toString(), "closed", "success", "Closed"),
                status().isOk());

        // Run test
        final ResultActions resultActions = performGet(CONTROLLER_BASE, MediaType.APPLICATION_JSON, status().isOk(),
                tenantAware.getCurrentTenant(), target.getControllerId());
        resultActions.andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$._links.deploymentBase.href").doesNotExist())
                .andExpect(jsonPath("$._links.installedBase.href",
                        containsString(String.format("/%s/controller/v1/%s/installedBase/%d",
                                tenantAware.getCurrentTenant(), target.getControllerId(), actionId))));

        getAndVerifyInstalledBasePayload(CONTROLLER_ID, MediaType.APPLICATION_JSON, ds, artifact, artifactSignature,
                actionId, ds.findFirstModuleByType(osType).get().getId(), actionType);

        getAndVerifyInstalledBasePayload(CONTROLLER_ID, MediaTypes.HAL_JSON, ds, artifact, artifactSignature, actionId,
                ds.findFirstModuleByType(osType).get().getId(), actionType);

        // Action is still finished after calling installedBase
        final Iterable<ActionStatus> actionStatusMessages = deploymentManagement
                .findActionStatusByAction(PageRequest.of(0, 100, Sort.Direction.DESC, "id"), actionId);
        assertThat(actionStatusMessages).hasSize(2);
        final ActionStatus actionStatusMessage = actionStatusMessages.iterator().next();
        assertThat(actionStatusMessage.getStatus()).isEqualTo(Action.Status.FINISHED);
    }

    @Test
    @Description("Test download-only deployment to a controller. Checks that download-only is not represented as installedBase.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = TargetPollEvent.class, count = 2) })
    public void deploymentDownloadOnlyActionNotInInstalledBase() throws Exception {
        // Prepare test data
        final Target target = testdataFactory.createTarget();
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        final Long actionId = getFirstAssignedActionId(
                assignDistributionSet(ds.getId(), target.getControllerId(), Action.ActionType.DOWNLOAD_ONLY));

        performGet(CONTROLLER_BASE, MediaType.APPLICATION_JSON, status().isOk(), tenantAware.getCurrentTenant(),
                target.getControllerId()).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                        .andExpect(jsonPath("$._links.deploymentBase.href").exists())
                        .andExpect(jsonPath("$._links.installedBase.href").doesNotExist());

        postDeploymentFeedback(target.getControllerId(), actionId,
                JsonBuilder.deploymentActionFeedback(actionId.toString(), "download"), status().isOk());
        postDeploymentFeedback(target.getControllerId(), actionId,
                JsonBuilder.deploymentActionFeedback(actionId.toString(), "downloaded"), status().isOk());

        // Test
        performGet(CONTROLLER_BASE, MediaType.APPLICATION_JSON, status().isOk(), tenantAware.getCurrentTenant(),
                target.getControllerId()).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                        .andExpect(jsonPath("$._links.deploymentBase.href").doesNotExist())
                        .andExpect(jsonPath("$._links.installedBase.href").doesNotExist());
    }

    @ParameterizedTest
    @MethodSource("org.eclipse.hawkbit.ddi.rest.resource.DdiInstalledBaseTest#actionTypeForDeployment")
    @Description("Test a failed deployment to a controller. Checks that closed action is not represented as installedBase.")
    public void deploymentActionFailedNotInInstalledBase(Action.ActionType actionType) throws Exception {
        // Prepare test data
        final Target target = testdataFactory.createTarget();
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        final Long actionId = getFirstAssignedActionId(
                assignDistributionSet(ds.getId(), target.getControllerId(), actionType));

        performGet(CONTROLLER_BASE, MediaType.APPLICATION_JSON, status().isOk(), tenantAware.getCurrentTenant(),
                target.getControllerId()).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                        .andExpect(jsonPath("$._links.deploymentBase.href").exists())
                        .andExpect(jsonPath("$._links.installedBase.href").doesNotExist());

        postDeploymentFeedback(target.getControllerId(), actionId,
                JsonBuilder.deploymentActionFeedback(actionId.toString(), "proceeding"), status().isOk());
        postDeploymentFeedback(target.getControllerId(), actionId,
                JsonBuilder.deploymentActionFeedback(actionId.toString(), "closed", "failure", "Installation failed"),
                status().isOk());

        // Test
        performGet(CONTROLLER_BASE, MediaType.APPLICATION_JSON, status().isOk(), tenantAware.getCurrentTenant(),
                target.getControllerId()).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                        .andExpect(jsonPath("$._links.deploymentBase.href").doesNotExist())
                        .andExpect(jsonPath("$._links.installedBase.href").doesNotExist());
    }

    @Test
    @Description("Test to verify that only a specific count of messages are returned based on the input actionHistory for getControllerInstalledAction endpoint.")
    public void testActionHistoryCount() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("911");
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));
        final Action savedAction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);

        postDeploymentFeedback(savedTarget.getControllerId(), savedAction.getId(), JsonBuilder.deploymentActionFeedback(
                savedAction.getId().toString(), "scheduled", "Installation scheduled"), status().isOk());

        postDeploymentFeedback(savedTarget.getControllerId(), savedAction.getId(), JsonBuilder.deploymentActionFeedback(
                savedAction.getId().toString(), "proceeding", "Installation proceeding"), status().isOk());
        // only this feedback triggers the ActionUpdateEvent
        postDeploymentFeedback(savedTarget.getControllerId(), savedAction.getId(), JsonBuilder.deploymentActionFeedback(
                savedAction.getId().toString(), "closed", "success", "Installation completed"), status().isOk());

        // Test
        // for zero input no action history is returned
        mvc.perform(get(INSTALLED_BASE + "?actionHistory", tenantAware.getCurrentTenant(), 911, savedAction.getId())
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.actionHistory.messages").doesNotExist());

        // depending on given query parameter value, only the latest messages are
        // returned
        mvc.perform(get(INSTALLED_BASE + "?actionHistory=2", tenantAware.getCurrentTenant(), 911, savedAction.getId())
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.actionHistory.messages", hasItem(containsString("Installation completed"))))
                .andExpect(jsonPath("$.actionHistory.messages", hasItem(containsString("Installation proceeding"))))
                .andExpect(
                        jsonPath("$.actionHistory.messages", not(hasItem(containsString("Installation scheduled")))));

        // for negative input the entire action history is returned
        mvc.perform(get(INSTALLED_BASE + "?actionHistory=-3", tenantAware.getCurrentTenant(), 911, savedAction.getId())
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.actionHistory.messages", hasItem(containsString("Installation completed"))))
                .andExpect(jsonPath("$.actionHistory.messages", hasItem(containsString("Installation proceeding"))))
                .andExpect(jsonPath("$.actionHistory.messages", hasItem(containsString("Installation scheduled"))));
    }

    @Test
    @Description("Test various invalid access attempts to the installed resource und the expected behaviour of the server.")
    public void badInstalledAction() throws Exception {
        final Target target = testdataFactory.createTarget(CONTROLLER_ID);

        // not allowed methods
        mvc.perform(post(INSTALLED_BASE, tenantAware.getCurrentTenant(), CONTROLLER_ID, "1"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        mvc.perform(put(INSTALLED_BASE, tenantAware.getCurrentTenant(), CONTROLLER_ID, "1"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        mvc.perform(delete(INSTALLED_BASE, tenantAware.getCurrentTenant(), CONTROLLER_ID, "1"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        // non existing target
        mvc.perform(MockMvcRequestBuilders.get(INSTALLED_BASE, tenantAware.getCurrentTenant(), "not-existing", "1"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // no deployment
        mvc.perform(MockMvcRequestBuilders.get(INSTALLED_BASE, tenantAware.getCurrentTenant(), CONTROLLER_ID, "1"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // wrong media type
        final List<Target> toAssign = Collections.singletonList(target);
        final DistributionSet savedSet = testdataFactory.createDistributionSet("");

        final Long actionId = getFirstAssignedActionId(assignDistributionSet(savedSet, toAssign));
        postDeploymentFeedback(CONTROLLER_ID, actionId,
                JsonBuilder.deploymentActionFeedback(actionId.toString(), "closed", "success"), status().isOk());
        mvc.perform(MockMvcRequestBuilders.get(INSTALLED_BASE, tenantAware.getCurrentTenant(), CONTROLLER_ID, actionId))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        mvc.perform(MockMvcRequestBuilders.get(INSTALLED_BASE, tenantAware.getCurrentTenant(), CONTROLLER_ID, actionId)
                .accept(MediaType.APPLICATION_ATOM_XML)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotAcceptable());
    }

    private Target createTargetAndAssertNoActiveActions() {
        final Target savedTarget = testdataFactory.createTarget(CONTROLLER_ID);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).isEmpty();
        assertThat(deploymentManagement.countActionsAll()).isZero();
        assertThat(deploymentManagement.countActionStatusAll()).isZero();
        return savedTarget;
    }

}
