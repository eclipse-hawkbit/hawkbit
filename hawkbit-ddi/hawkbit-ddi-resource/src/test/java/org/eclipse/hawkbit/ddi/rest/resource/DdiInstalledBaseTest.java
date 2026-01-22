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

import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.ddi.json.model.DdiResult;
import org.eclipse.hawkbit.ddi.json.model.DdiStatus;
import org.eclipse.hawkbit.ddi.rest.api.DdiRestConstants;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAttributesRequestedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
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
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Test installed base from the controller.
 * <p/>
 * Feature: Component Tests - Direct Device Integration API<br/>
 * Story: Installed Base Resource
 */
class DdiInstalledBaseTest extends AbstractDDiApiIntegrationTest {

    @Autowired
    ActionStatusRepository actionStatusRepository;
    private static final int ARTIFACT_SIZE = 5 * 1024;
    private static final String CONTROLLER_ID = "4715";

    /**
     * Ensure that the installed base resource is available as CBOR
     */
    @Test
    void installedBaseResourceCbor() throws Exception {
        final Target target = testdataFactory.createTarget();
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        final Long softwareModuleId = ds.getModules().stream().findAny().get().getId();
        testdataFactory.createArtifacts(softwareModuleId);

        final Long actionId = getFirstAssignedActionId(assignDistributionSet(ds, target));
        postDeploymentFeedback(target.getControllerId(), actionId, getJsonClosedDeploymentActionFeedback(), status().isOk());

        // get installed base
        performGet(INSTALLED_BASE, MediaType.parseMediaType(DdiRestConstants.MEDIA_TYPE_APPLICATION_CBOR), status().isOk(),
                AccessContext.tenant(), target.getControllerId(), actionId.toString());

        // get artifacts
        performGet(SOFTWARE_MODULE_ARTIFACTS, MediaType.parseMediaType(DdiRestConstants.MEDIA_TYPE_APPLICATION_CBOR),
                status().isOk(), AccessContext.tenant(), target.getControllerId(),
                String.valueOf(softwareModuleId));
    }

    /**
     * Ensure that assigned version is self assigned version
     */
    @Test
    void installedVersion() throws Exception {
        final Target target = createTargetAndAssertNoActiveActions();
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        // update assigned version
        putInstalledBase(target.getControllerId(), getJsonInstalledBase(ds.getName(), ds.getVersion()), status().isCreated());

        assertThat(deploymentManagement.findAssignedDistributionSet(target.getControllerId()).get().getId()).isEqualTo(ds.getId());

        // update assigned version while version already assigned
        putInstalledBase(target.getControllerId(), getJsonInstalledBase(ds.getName(), ds.getVersion()), status().isOk());
    }

    /**
     * Ensure that installedVersion is version self assigned
     */
    @Test
    void installedVersionNotExist() throws Exception {
        final Target target = createTargetAndAssertNoActiveActions();
        final String dsName = "unknown";
        final String dsVersion = "1.0.0";

        // get installed base
        putInstalledBase(target.getControllerId(), getJsonInstalledBase(dsName, dsVersion), status().isNotFound());

        assertThat(deploymentManagement.findAssignedDistributionSet(target.getControllerId()).isEmpty()).isTrue();
    }

    /**
     * Test several deployments to a controller. Checks that action is represented as installedBase after installation.
     */
    @Test
    void deploymentSeveralActionsInInstalledBase() throws Exception {
        // Prepare test data
        final Target target = createTargetAndAssertNoActiveActions();

        final DistributionSet ds1 = testdataFactory.createDistributionSet("1", true);
        final Artifact artifact1 = testdataFactory.createArtifact(nextBytes(ARTIFACT_SIZE),
                getOsModule(ds1), "test1", ARTIFACT_SIZE);
        final Artifact artifactSignature1 = testdataFactory.createArtifact(nextBytes(ARTIFACT_SIZE),
                getOsModule(ds1), "test1.signature", ARTIFACT_SIZE);

        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true);
        final Artifact artifact2 = testdataFactory.createArtifact(nextBytes(ARTIFACT_SIZE),
                getOsModule(ds2), "test2", ARTIFACT_SIZE);
        final Artifact artifactSignature2 = testdataFactory.createArtifact(nextBytes(ARTIFACT_SIZE),
                getOsModule(ds2), "test2.signature", ARTIFACT_SIZE);

        // Run test with 1st action
        final Long actionId1 = getFirstAssignedActionId(
                assignDistributionSet(ds1.getId(), target.getControllerId(), Action.ActionType.SOFT));
        performGet(CONTROLLER_BASE, MediaTypes.HAL_JSON, status().isOk(), AccessContext.tenant(), CONTROLLER_ID)
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.installedBase.href").doesNotExist())
                .andExpect(jsonPath("$._links.deploymentBase.href",
                        startsWith(deploymentBaseLink(CONTROLLER_ID, actionId1.toString()))));

        getAndVerifyDeploymentBasePayload(CONTROLLER_ID, MediaType.APPLICATION_JSON, ds1, artifact1, artifactSignature1,
                actionId1, findFirstModuleByType(ds1, osType).orElseThrow().getId(), Action.ActionType.SOFT);

        postDeploymentFeedback(target.getControllerId(), actionId1,
                getJsonActionFeedback(DdiStatus.ExecutionStatus.CLOSED, DdiResult.FinalResult.SUCCESS, Collections.singletonList("Closed")),
                status().isOk());

        getAndVerifyInstalledBasePayload(CONTROLLER_ID, MediaType.APPLICATION_JSON, ds1, artifact1, artifactSignature1,
                actionId1, findFirstModuleByType(ds1, osType).orElseThrow().getId(), Action.ActionType.SOFT);

        // Run test with 2nd action
        final Long actionId2 = getFirstAssignedActionId(
                assignDistributionSet(ds2.getId(), target.getControllerId(), Action.ActionType.FORCED));
        performGet(CONTROLLER_BASE, MediaTypes.HAL_JSON, status().isOk(), AccessContext.tenant(), CONTROLLER_ID)
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.installedBase.href",
                        startsWith(installedBaseLink(CONTROLLER_ID, actionId1.toString()))))
                .andExpect(jsonPath("$._links.deploymentBase.href",
                        startsWith(deploymentBaseLink(CONTROLLER_ID, actionId2.toString()))));

        getAndVerifyDeploymentBasePayload(CONTROLLER_ID, MediaType.APPLICATION_JSON, ds2, artifact2, artifactSignature2,
                actionId2, findFirstModuleByType(ds2, osType).orElseThrow().getId(), Action.ActionType.FORCED);

        postDeploymentFeedback(target.getControllerId(), actionId2,
                getJsonActionFeedback(DdiStatus.ExecutionStatus.CLOSED, DdiResult.FinalResult.SUCCESS, Collections.singletonList("Closed")),
                status().isOk());

        getAndVerifyInstalledBasePayload(CONTROLLER_ID, MediaType.APPLICATION_JSON, ds2, artifact2, artifactSignature2,
                actionId2, findFirstModuleByType(ds2, osType).orElseThrow().getId(), Action.ActionType.FORCED);

        performGet(CONTROLLER_BASE, MediaTypes.HAL_JSON, status().isOk(), AccessContext.tenant(), CONTROLLER_ID)
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.installedBase.href",
                        startsWith(installedBaseLink(CONTROLLER_ID, actionId2.toString()))))
                .andExpect(jsonPath("$._links.deploymentBase.href").doesNotExist());

        // older installed action is still accessible, although not part of controller base
        getAndVerifyInstalledBasePayload(CONTROLLER_ID, MediaType.APPLICATION_JSON, ds1, artifact1, artifactSignature1,
                actionId1, findFirstModuleByType(ds1, osType).orElseThrow().getId(), Action.ActionType.SOFT);
    }

    /**
     * Test several deployments of same ds to a controller. Checks that cancelled action in history is not linked as installedBase.
     */
    @Test
    void deploymentActionsOfSameDsWithCancelledActionInHistory() throws Exception {
        // Prepare test data
        final Target target = createTargetAndAssertNoActiveActions();

        final DistributionSet ds1 = testdataFactory.createDistributionSet("1", true);
        final Artifact artifact1 = testdataFactory.createArtifact(nextBytes(ARTIFACT_SIZE),
                getOsModule(ds1), "test1", ARTIFACT_SIZE);
        final Artifact artifactSignature1 = testdataFactory.createArtifact(nextBytes(ARTIFACT_SIZE),
                getOsModule(ds1), "test1.signature", ARTIFACT_SIZE);

        // assign ds1, action1 - and provide cancel feedback
        final Long actionId1 = getFirstAssignedActionId(
                assignDistributionSet(ds1.getId(), target.getControllerId(), Action.ActionType.SOFT));
        deploymentManagement.cancelAction(actionId1);
        postCancelFeedback(target.getControllerId(), actionId1,
                getJsonActionFeedback(DdiStatus.ExecutionStatus.CLOSED, DdiResult.FinalResult.SUCCESS, Collections.singletonList("Canceled")),
                status().isOk());

        // assign ds1, action2 - and provide cancel feedback
        final Long actionId2 = getFirstAssignedActionId(
                assignDistributionSet(ds1.getId(), target.getControllerId(), Action.ActionType.FORCED));
        deploymentManagement.cancelAction(actionId2);
        postCancelFeedback(target.getControllerId(), actionId2,
                getJsonActionFeedback(DdiStatus.ExecutionStatus.CLOSED, DdiResult.FinalResult.SUCCESS, Collections.singletonList("Canceled")),
                status().isOk());

        // assign ds1, action 3 - and provide success feedback
        final Long actionId3 = getFirstAssignedActionId(
                assignDistributionSet(ds1.getId(), target.getControllerId(), Action.ActionType.SOFT));
        postDeploymentFeedback(target.getControllerId(), actionId3,
                getJsonActionFeedback(DdiStatus.ExecutionStatus.CLOSED, DdiResult.FinalResult.SUCCESS, Collections.singletonList("Canceled")),
                status().isOk());

        // Test: latest succeeded action is returned in installedBase
        performGet(CONTROLLER_BASE, MediaTypes.HAL_JSON, status().isOk(), AccessContext.tenant(), CONTROLLER_ID)
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.installedBase.href",
                        startsWith(installedBaseLink(CONTROLLER_ID, actionId3.toString()))))
                .andExpect(jsonPath("$._links.deploymentBase.href").doesNotExist());

        getAndVerifyInstalledBasePayload(CONTROLLER_ID, MediaType.APPLICATION_JSON, ds1, artifact1, artifactSignature1,
                actionId3, findFirstModuleByType(ds1, osType).orElseThrow().getId(), Action.ActionType.SOFT);

        // cancelled action are not accessible
        mvc.perform(MockMvcRequestBuilders.get(INSTALLED_BASE, AccessContext.tenant(), target.getControllerId(),
                        actionId1.toString()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
        mvc.perform(MockMvcRequestBuilders.get(INSTALLED_BASE, AccessContext.tenant(), target.getControllerId(),
                        actionId2.toString()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    /**
     * Test several deployments of same ds to a controller. Checks that latest cancelled action does not override actual installed ds.
     */
    @Test
    void deploymentActionsOfSameDsWithCancelledAction() throws Exception {
        // Prepare test data
        final Target target = createTargetAndAssertNoActiveActions();

        final DistributionSet ds1 = testdataFactory.createDistributionSet("1", true);
        final Artifact artifact1 = testdataFactory.createArtifact(nextBytes(ARTIFACT_SIZE),
                getOsModule(ds1), "test1", ARTIFACT_SIZE);
        final Artifact artifactSignature1 = testdataFactory.createArtifact(nextBytes(ARTIFACT_SIZE),
                getOsModule(ds1), "test1.signature", ARTIFACT_SIZE);

        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true);

        // assign ds1, action1 - and provide success feedback
        final Long actionId1 = getFirstAssignedActionId(
                assignDistributionSet(ds1.getId(), target.getControllerId(), Action.ActionType.SOFT));
        postDeploymentFeedback(target.getControllerId(), actionId1,
                getJsonActionFeedback(DdiStatus.ExecutionStatus.CLOSED, DdiResult.FinalResult.SUCCESS, Collections.singletonList("Success")),
                status().isOk());

        // assign ds2, action2 - assign ds1, action 3 - and cancel both
        final Long actionId2 = getFirstAssignedActionId(
                assignDistributionSet(ds2.getId(), target.getControllerId(), Action.ActionType.FORCED));
        final Long actionId3 = getFirstAssignedActionId(
                assignDistributionSet(ds1.getId(), target.getControllerId(), Action.ActionType.SOFT));
        deploymentManagement.cancelAction(actionId2);
        postCancelFeedback(target.getControllerId(), actionId2,
                getJsonActionFeedback(DdiStatus.ExecutionStatus.CLOSED, DdiResult.FinalResult.SUCCESS, Collections.singletonList("Canceled")),
                status().isOk());
        deploymentManagement.cancelAction(actionId3);
        postCancelFeedback(target.getControllerId(), actionId3,
                getJsonActionFeedback(DdiStatus.ExecutionStatus.CLOSED, DdiResult.FinalResult.SUCCESS, Collections.singletonList("Canceled")),
                status().isOk());

        // Test: the succeeded action is returned in installedBase instead of the latest
        // cancelled action
        performGet(CONTROLLER_BASE, MediaTypes.HAL_JSON, status().isOk(), AccessContext.tenant(), CONTROLLER_ID)
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.installedBase.href",
                        startsWith(installedBaseLink(CONTROLLER_ID, actionId1.toString()))))
                .andExpect(jsonPath("$._links.deploymentBase.href").doesNotExist());

        getAndVerifyInstalledBasePayload(CONTROLLER_ID, MediaType.APPLICATION_JSON, ds1, artifact1, artifactSignature1,
                actionId1, findFirstModuleByType(ds1, osType).orElseThrow().getId(), Action.ActionType.SOFT);

        // cancelled action are not accessible
        mvc.perform(MockMvcRequestBuilders.get(INSTALLED_BASE, AccessContext.tenant(), target.getControllerId(),
                        actionId2.toString()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
        mvc.perform(MockMvcRequestBuilders.get(INSTALLED_BASE, AccessContext.tenant(), target.getControllerId(),
                        actionId3.toString()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    /**
     * Test several deployments of same ds to a controller. Checks that latest running action does not override actual installed ds.
     */
    @Test
    void deploymentActionsOfSameDsWithRunningAction() throws Exception {
        // Prepare test data
        final Target target = createTargetAndAssertNoActiveActions();

        final DistributionSet ds1 = testdataFactory.createDistributionSet("1", true);
        final Artifact artifact1 = testdataFactory.createArtifact(nextBytes(ARTIFACT_SIZE),
                getOsModule(ds1), "test1", ARTIFACT_SIZE);
        final Artifact artifactSignature1 = testdataFactory.createArtifact(nextBytes(ARTIFACT_SIZE),
                getOsModule(ds1), "test1.signature", ARTIFACT_SIZE);

        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true);

        // assign ds1, action1 - and provide success feedback
        final Long actionId1 = getFirstAssignedActionId(
                assignDistributionSet(ds1.getId(), target.getControllerId(), Action.ActionType.SOFT));
        postDeploymentFeedback(target.getControllerId(), actionId1,
                getJsonActionFeedback(DdiStatus.ExecutionStatus.CLOSED, DdiResult.FinalResult.SUCCESS, Collections.singletonList("Success")),
                status().isOk());

        // assign ds2, action2 - assign ds1, action 3 - and cancel action 2
        final Long actionId2 = getFirstAssignedActionId(
                assignDistributionSet(ds2.getId(), target.getControllerId(), Action.ActionType.FORCED));
        final Long actionId3 = getFirstAssignedActionId(
                assignDistributionSet(ds1.getId(), target.getControllerId(), Action.ActionType.SOFT));
        deploymentManagement.cancelAction(actionId2);
        postCancelFeedback(target.getControllerId(), actionId2,
                getJsonActionFeedback(DdiStatus.ExecutionStatus.CLOSED, DdiResult.FinalResult.SUCCESS, Collections.singletonList("Canceled")),
                status().isOk());

        // Test: the succeeded action is returned in installedBase instead of the latest cancelled action
        performGet(CONTROLLER_BASE, MediaTypes.HAL_JSON, status().isOk(), AccessContext.tenant(), CONTROLLER_ID)
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.installedBase.href",
                        startsWith(installedBaseLink(CONTROLLER_ID, actionId1.toString()))))
                .andExpect(jsonPath("$._links.deploymentBase.href",
                        startsWith(deploymentBaseLink(CONTROLLER_ID, actionId3.toString()))));

        getAndVerifyInstalledBasePayload(CONTROLLER_ID, MediaType.APPLICATION_JSON, ds1, artifact1, artifactSignature1,
                actionId1, findFirstModuleByType(ds1, osType).orElseThrow().getId(), Action.ActionType.SOFT);

        // cancelled action are not accessible
        mvc.perform(MockMvcRequestBuilders.get(INSTALLED_BASE, AccessContext.tenant(), target.getControllerId(),
                        actionId2.toString()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
        mvc.perform(MockMvcRequestBuilders.get(INSTALLED_BASE, AccessContext.tenant(), target.getControllerId(),
                        actionId3.toString()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    /**
     * Test open deployment to a controller. Checks that installedBase returns 404 for a pending action.
     */
    @Test
    void installedBaseReturns404ForPendingAction() throws Exception {
        // Prepare test data
        final Target target = createTargetAndAssertNoActiveActions();
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        final Long actionId = getFirstAssignedActionId(assignDistributionSet(ds, target));

        performGet(CONTROLLER_BASE, MediaTypes.HAL_JSON, status().isOk(), AccessContext.tenant(), CONTROLLER_ID)
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.installedBase.href").doesNotExist())
                .andExpect(jsonPath("$._links.deploymentBase.href",
                        startsWith(deploymentBaseLink(CONTROLLER_ID, actionId.toString()))));

        performGet(DEPLOYMENT_BASE, MediaType.APPLICATION_JSON, status().isOk(), AccessContext.tenant(),
                target.getControllerId(), actionId.toString());

        mvc.perform(MockMvcRequestBuilders.get(INSTALLED_BASE, AccessContext.tenant(), target.getControllerId(),
                        actionId.toString()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    /**
     * Ensures that artifacts are found, after the action was already closed.
     */
    @Test
    void artifactsOfInstalledActionExist() throws Exception {
        final Target target = createTargetAndAssertNoActiveActions();
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        final Long softwareModuleId = ds.getModules().stream().findAny().get().getId();
        testdataFactory.createArtifacts(softwareModuleId);

        final Long actionId = getFirstAssignedActionId(assignDistributionSet(ds, target));

        postDeploymentFeedback(target.getControllerId(), actionId, getJsonClosedDeploymentActionFeedback(), status().isOk());

        performGet(SOFTWARE_MODULE_ARTIFACTS, MediaType.APPLICATION_JSON, status().isOk(),
                AccessContext.tenant(), target.getControllerId(), softwareModuleId.toString())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$.[?(@.filename=='filename0')]", hasSize(1)))
                .andExpect(jsonPath("$.[?(@.filename=='filename1')]", hasSize(1)))
                .andExpect(jsonPath("$.[?(@.filename=='filename2')]", hasSize(1)));
    }

    /**
     * Test forced deployment to a controller. Checks that action is represented as installedBase after installation.
     */
    @ParameterizedTest
    @MethodSource("org.eclipse.hawkbit.ddi.rest.resource.DdiInstalledBaseTest#actionTypeForDeployment")
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 5), // implicit lock
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1) })
    void deploymentActionInInstalledBase(final Action.ActionType actionType) throws Exception {
        // Prepare test data
        final Target target = createTargetAndAssertNoActiveActions();
        final DistributionSet ds = testdataFactory.createDistributionSet("", true);
        final Artifact artifact = testdataFactory.createArtifact(nextBytes(ARTIFACT_SIZE), getOsModule(ds),
                "test1", ARTIFACT_SIZE);
        final Artifact artifactSignature = testdataFactory.createArtifact(nextBytes(ARTIFACT_SIZE),
                getOsModule(ds), "test1.signature", ARTIFACT_SIZE);
        final Long actionId = getFirstAssignedActionId(
                assignDistributionSet(ds.getId(), target.getControllerId(), actionType));

        postDeploymentFeedback(target.getControllerId(), actionId,
                getJsonActionFeedback(DdiStatus.ExecutionStatus.CLOSED, DdiResult.FinalResult.SUCCESS, Collections.singletonList("Closed")),
                status().isOk());

        // Run test
        final ResultActions resultActions = performGet(CONTROLLER_BASE, MediaType.APPLICATION_JSON, status().isOk(),
                AccessContext.tenant(), target.getControllerId());
        resultActions.andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.deploymentBase.href").doesNotExist())
                .andExpect(jsonPath("$._links.installedBase.href", containsString(String.format(
                        "/%s/controller/v1/%s/installedBase/%d",
                        AccessContext.tenant(), target.getControllerId(), actionId))));

        getAndVerifyInstalledBasePayload(CONTROLLER_ID, MediaType.APPLICATION_JSON, ds, artifact, artifactSignature,
                actionId, findFirstModuleByType(ds, osType).orElseThrow().getId(), actionType);

        getAndVerifyInstalledBasePayload(CONTROLLER_ID, MediaTypes.HAL_JSON, ds, artifact, artifactSignature, actionId,
                findFirstModuleByType(ds, osType).orElseThrow().getId(), actionType);

        // Action is still finished after calling installedBase
        final Iterable<ActionStatus> actionStatusMessages = deploymentManagement
                .findActionStatusByAction(actionId, PageRequest.of(0, 100, Sort.Direction.DESC, "id"));
        assertThat(actionStatusMessages).hasSize(2);
        final ActionStatus actionStatusMessage = actionStatusMessages.iterator().next();
        assertThat(actionStatusMessage.getStatus()).isEqualTo(Action.Status.FINISHED);
    }

    /**
     * Test download-only deployment to a controller. Checks that download-only is not represented as installedBase.
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
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 2) })
    void deploymentDownloadOnlyActionNotInInstalledBase() throws Exception {
        // Prepare test data
        final Target target = testdataFactory.createTarget();
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        final Long actionId = getFirstAssignedActionId(
                assignDistributionSet(ds.getId(), target.getControllerId(), Action.ActionType.DOWNLOAD_ONLY));

        performGet(CONTROLLER_BASE, MediaType.APPLICATION_JSON, status().isOk(), AccessContext.tenant(),
                target.getControllerId())
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.deploymentBase.href").exists())
                .andExpect(jsonPath("$._links.installedBase.href").doesNotExist());

        postDeploymentFeedback(target.getControllerId(), actionId, getJsonDownloadDeploymentActionFeedback(), status().isOk());
        postDeploymentFeedback(target.getControllerId(), actionId, getJsonDownloadedDeploymentActionFeedback(), status().isOk());

        // Test
        performGet(CONTROLLER_BASE, MediaType.APPLICATION_JSON, status().isOk(), AccessContext.tenant(),
                target.getControllerId())
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.deploymentBase.href").doesNotExist())
                .andExpect(jsonPath("$._links.installedBase.href").doesNotExist());
    }

    /**
     * Test a failed deployment to a controller. Checks that closed action is not represented as installedBase.
     */
    @ParameterizedTest
    @MethodSource("org.eclipse.hawkbit.ddi.rest.resource.DdiInstalledBaseTest#actionTypeForDeployment")
    void deploymentActionFailedNotInInstalledBase(final Action.ActionType actionType) throws Exception {
        // Prepare test data
        final Target target = testdataFactory.createTarget();
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        final Long actionId = getFirstAssignedActionId(
                assignDistributionSet(ds.getId(), target.getControllerId(), actionType));

        performGet(CONTROLLER_BASE, MediaType.APPLICATION_JSON, status().isOk(), AccessContext.tenant(),
                target.getControllerId())
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.deploymentBase.href").exists())
                .andExpect(jsonPath("$._links.installedBase.href").doesNotExist());

        postDeploymentFeedback(target.getControllerId(), actionId,
                getJsonActionFeedback(DdiStatus.ExecutionStatus.PROCEEDING, DdiResult.FinalResult.NONE),
                status().isOk());
        postDeploymentFeedback(target.getControllerId(), actionId,
                getJsonActionFeedback(DdiStatus.ExecutionStatus.CLOSED, DdiResult.FinalResult.FAILURE,
                        Collections.singletonList("Installation failed")),
                status().isOk());

        // Test
        performGet(CONTROLLER_BASE, MediaType.APPLICATION_JSON, status().isOk(), AccessContext.tenant(),
                target.getControllerId())
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.deploymentBase.href").doesNotExist())
                .andExpect(jsonPath("$._links.installedBase.href").doesNotExist());
    }

    /**
     * Test to verify that only a specific count of messages are returned based on the input actionHistory for getControllerInstalledAction endpoint.
     */
    @Test
    void testActionHistoryCount() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("911");
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));
        final Action savedAction = deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE).getContent().get(0);

        postDeploymentFeedback(savedTarget.getControllerId(), savedAction.getId(),
                getJsonActionFeedback(DdiStatus.ExecutionStatus.SCHEDULED, DdiResult.FinalResult.NONE,
                        Collections.singletonList("Installation scheduled")),
                status().isOk());

        postDeploymentFeedback(savedTarget.getControllerId(), savedAction.getId(),
                getJsonActionFeedback(DdiStatus.ExecutionStatus.PROCEEDING, DdiResult.FinalResult.NONE,
                        Collections.singletonList("Installation proceeding")),
                status().isOk());
        // only this feedback triggers the ActionUpdateEvent
        postDeploymentFeedback(savedTarget.getControllerId(), savedAction.getId(),
                getJsonActionFeedback(DdiStatus.ExecutionStatus.CLOSED, DdiResult.FinalResult.SUCCESS,
                        Collections.singletonList("Installation completed")),
                status().isOk());

        // Test
        // for zero input no action history is returned
        mvc.perform(get(INSTALLED_BASE + "?actionHistory", AccessContext.tenant(), 911, savedAction.getId())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionHistory.messages").doesNotExist());

        // depending on given query parameter value, only the latest messages are returned
        mvc.perform(get(INSTALLED_BASE + "?actionHistory=2", AccessContext.tenant(), 911, savedAction.getId())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionHistory.messages", hasItem(containsString("Installation completed"))))
                .andExpect(jsonPath("$.actionHistory.messages", hasItem(containsString("Installation proceeding"))))
                .andExpect(
                        jsonPath("$.actionHistory.messages", not(hasItem(containsString("Installation scheduled")))));

        // for negative input the entire action history is returned
        mvc.perform(get(INSTALLED_BASE + "?actionHistory=-3", AccessContext.tenant(), 911, savedAction.getId())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionHistory.messages", hasItem(containsString("Installation completed"))))
                .andExpect(jsonPath("$.actionHistory.messages", hasItem(containsString("Installation proceeding"))))
                .andExpect(jsonPath("$.actionHistory.messages", hasItem(containsString("Installation scheduled"))));
    }

    /**
     * Test various invalid access attempts to the installed resource und the expected behaviour of the server.
     */
    @Test
    void badInstalledAction() throws Exception {
        final Target target = testdataFactory.createTarget(CONTROLLER_ID);

        // not allowed methods
        mvc.perform(post(INSTALLED_BASE, AccessContext.tenant(), CONTROLLER_ID, "1"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(put(INSTALLED_BASE, AccessContext.tenant(), CONTROLLER_ID, "1"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(delete(INSTALLED_BASE, AccessContext.tenant(), CONTROLLER_ID, "1"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        // non existing target
        mvc.perform(MockMvcRequestBuilders.get(INSTALLED_BASE, AccessContext.tenant(), "not-existing", "1"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // no deployment
        mvc.perform(MockMvcRequestBuilders.get(INSTALLED_BASE, AccessContext.tenant(), CONTROLLER_ID, "1"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // wrong media type
        final List<Target> toAssign = Collections.singletonList(target);
        final DistributionSet savedSet = testdataFactory.createDistributionSet("");

        final Long actionId = getFirstAssignedActionId(assignDistributionSet(savedSet, toAssign));
        postDeploymentFeedback(CONTROLLER_ID, actionId, getJsonClosedCancelActionFeedback(), status().isOk());
        mvc.perform(MockMvcRequestBuilders.get(INSTALLED_BASE, AccessContext.tenant(), CONTROLLER_ID, actionId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        mvc.perform(MockMvcRequestBuilders.get(INSTALLED_BASE, AccessContext.tenant(), CONTROLLER_ID, actionId)
                        .accept(MediaType.APPLICATION_ATOM_XML))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotAcceptable());
    }

    @SuppressWarnings("java:S1144") // java:S1144 - used in MethodSource, sonar my find it incorrectly as unused
    private static Stream<Action.ActionType> actionTypeForDeployment() {
        return Stream.of(Action.ActionType.SOFT, Action.ActionType.FORCED);
    }

    private Target createTargetAndAssertNoActiveActions() {
        final Target savedTarget = testdataFactory.createTarget(CONTROLLER_ID);
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).isEmpty();
        assertThat(deploymentManagement.countActionsAll()).isZero();
        assertThat(actionStatusRepository.count()).isZero();
        return savedTarget;
    }
}