/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.jayway.jsonpath.JsonPath;
import org.assertj.core.api.Condition;
import org.eclipse.hawkbit.ddi.json.model.DdiResult;
import org.eclipse.hawkbit.ddi.json.model.DdiStatus;
import org.eclipse.hawkbit.ddi.rest.api.DdiRestConstants;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.repository.ActionStatusRepository;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule.MetadataValueCreate;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.rest.exception.MessageNotReadableException;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Test deployment base from the controller.
 * <p/>
 * Feature: Component Tests - Direct Device Integration API<br/>
 * Story: Deployment Action Resource
 */
class DdiDeploymentBaseTest extends AbstractDDiApiIntegrationTest {

    private static final String DEFAULT_CONTROLLER_ID = "4712";

    @Autowired
    private ActionRepository actionRepository;
    @Autowired
    private ActionStatusRepository actionStatusRepository;

    /**
     * Ensure that the deployment resource is available as CBOR
     */
    @Test
    void deploymentResourceCbor() throws Exception {
        final Target target = testdataFactory.createTarget();
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("");

        final Long softwareModuleId = distributionSet.getModules().stream().findAny().orElseThrow().getId();
        testdataFactory.createArtifacts(softwareModuleId);

        assignDistributionSet(distributionSet.getId(), target.getName());
        final Action action = deploymentManagement.findActiveActionsByTarget(target.getControllerId(), PAGE).getContent().get(0);

        // get deployment base
        performGet(DEPLOYMENT_BASE, MediaType.parseMediaType(DdiRestConstants.MEDIA_TYPE_CBOR), status().isOk(),
                TenantAware.getCurrentTenant(), target.getControllerId(), action.getId().toString());

        // get artifacts
        performGet(SOFTWARE_MODULE_ARTIFACTS, MediaType.parseMediaType(DdiRestConstants.MEDIA_TYPE_CBOR),
                status().isOk(), TenantAware.getCurrentTenant(), target.getControllerId(),
                String.valueOf(softwareModuleId));

        final byte[] feedback = jsonToCbor(getJsonProceedingDeploymentActionFeedback());
        postDeploymentFeedback(
                MediaType.parseMediaType(DdiRestConstants.MEDIA_TYPE_CBOR), target.getControllerId(), action.getId(), feedback,
                status().isOk());
    }

    /**
     * Ensures that artifacts are not found, when software module does not exists.
     */
    @Test
    void artifactsNotFound() throws Exception {
        final Target target = testdataFactory.createTarget();
        performGet(SOFTWARE_MODULE_ARTIFACTS, MediaType.APPLICATION_JSON, status().isNotFound(), TenantAware.getCurrentTenant(),
                target.getControllerId(), "1");
    }

    /**
     * Ensures that artifacts are found, when software module exists.
     */
    @Test
    void artifactsExists() throws Exception {
        final Target target = testdataFactory.createTarget();
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("");

        final Long softwareModuleId = distributionSet.getModules().stream().findAny().orElseThrow().getId();
        testdataFactory.createArtifacts(softwareModuleId);

        assignDistributionSet(distributionSet.getId(), target.getName());

        performGet(SOFTWARE_MODULE_ARTIFACTS, MediaType.APPLICATION_JSON, status().isOk(), TenantAware.getCurrentTenant(),
                target.getControllerId(), softwareModuleId.toString())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$.[?(@.filename=='filename0')]", hasSize(1)))
                .andExpect(jsonPath("$.[?(@.filename=='filename1')]", hasSize(1)))
                .andExpect(jsonPath("$.[?(@.filename=='filename2')]", hasSize(1)));
    }

    /**
     * Forced deployment to a controller. Checks if the resource response payload for a given deployment is as expected.
     */
    @Test
    void deploymentForceAction() throws Exception {
        // Prepare test data
        final DistributionSet ds = testdataFactory.createDistributionSet("", true);
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true);
        final Artifact artifact = testdataFactory.createArtifact(nextBytes(ARTIFACT_SIZE), getOsModule(ds), "test1", ARTIFACT_SIZE);
        final Artifact artifactSignature = testdataFactory.createArtifact(
                nextBytes(ARTIFACT_SIZE), getOsModule(ds), "test1.signature", ARTIFACT_SIZE);
        final Target savedTarget = createTargetAndAssertNoActiveActions();

        final List<Target> targetsAssignedToDs = assignDistributionSet(ds.getId(), savedTarget.getControllerId(), ActionType.FORCED)
                .getAssignedEntity().stream()
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
        performGet(CONTROLLER_BASE, MediaTypes.HAL_JSON, status().isOk(), TenantAware.getCurrentTenant(),
                DEFAULT_CONTROLLER_ID)
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath(
                        "$._links.deploymentBase.href",
                        startsWith(deploymentBaseLink(DEFAULT_CONTROLLER_ID, uaction.getId().toString()))));
        assertLastTargetQueryUpdated(current);
        assertThat(countActionStatusAll()).isEqualTo(2);

        final DistributionSet findDistributionSetByAction = findDsByAction(action.getId()).get();
        getAndVerifyDeploymentBasePayload(DEFAULT_CONTROLLER_ID, MediaType.APPLICATION_JSON, ds, artifact,
                artifactSignature, action.getId(),
                findFirstModuleByType(findDistributionSetByAction, osType).orElseThrow().getId(), "forced", "forced");

        // Retrieved is reported
        final Iterable<ActionStatus> actionStatusMessages = deploymentManagement
                .findActionStatusByAction(uaction.getId(), PageRequest.of(0, 100, Direction.DESC, "id"));
        assertThat(actionStatusMessages).hasSize(2);
        final ActionStatus actionStatusMessage = actionStatusMessages.iterator().next();
        assertThat(actionStatusMessage.getStatus()).isEqualTo(Status.RETRIEVED);
    }

    /**
     * Checks that the deploymentBase URL changes when the action is switched from soft to forced in TIMEFORCED case.
     */
    @Test
    void changeEtagIfActionSwitchesFromSoftToForced() throws Exception {
        // Prepare test data
        final Target target = testdataFactory.createTarget(DEFAULT_CONTROLLER_ID);
        final DistributionSet ds = testdataFactory.createDistributionSet("", true);

        final Long actionId = getFirstAssignedActionId(assignDistributionSet(ds.getId(), target.getControllerId(),
                ActionType.TIMEFORCED, System.currentTimeMillis() + 2_000));

        MvcResult mvcResult = performGet(CONTROLLER_BASE, MediaTypes.HAL_JSON, status().isOk(),
                TenantAware.getCurrentTenant(), DEFAULT_CONTROLLER_ID)
                .andReturn();

        final String urlBeforeSwitch = JsonPath.compile("_links.deploymentBase.href")
                .read(mvcResult.getResponse().getContentAsString()).toString();

        // Time is not yet over, so we should see the same URL
        mvcResult = performGet(CONTROLLER_BASE, MediaTypes.HAL_JSON, status().isOk(), TenantAware.getCurrentTenant(),
                DEFAULT_CONTROLLER_ID)
                .andReturn();
        assertThat(JsonPath.compile("_links.deploymentBase.href").read(mvcResult.getResponse().getContentAsString())
                .toString()).isEqualTo(urlBeforeSwitch)
                .startsWith(deploymentBaseLink(DEFAULT_CONTROLLER_ID, actionId.toString()));

        // After the time is over we should see a new etag
        TimeUnit.MILLISECONDS.sleep(2_000);

        mvcResult = performGet(CONTROLLER_BASE, MediaTypes.HAL_JSON, status().isOk(), TenantAware.getCurrentTenant(),
                DEFAULT_CONTROLLER_ID)
                .andReturn();

        assertThat(JsonPath.compile("_links.deploymentBase.href").read(mvcResult.getResponse().getContentAsString())
                .toString()).isNotEqualTo(urlBeforeSwitch);
    }

    /**
     * Attempt/soft deployment to a controller. Checks if the resource response payload for a given deployment is as expected.
     */
    @Test
    void deploymentAttemptAction() throws Exception {
        // Prepare test data
        final DistributionSet ds = testdataFactory.createDistributionSet("", true);
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true);
        final String visibleMetadataOsKey = "metaDataVisible";
        final String visibleMetadataOsValue = "withValue";

        final Artifact artifact = testdataFactory.createArtifact(nextBytes(ARTIFACT_SIZE), getOsModule(ds),
                "test1", ARTIFACT_SIZE);
        final Artifact artifactSignature = testdataFactory.createArtifact(nextBytes(ARTIFACT_SIZE),
                getOsModule(ds), "test1.signature", ARTIFACT_SIZE);

        softwareModuleManagement.createMetadata(getOsModule(ds), visibleMetadataOsKey, new MetadataValueCreate(visibleMetadataOsValue, true));
        softwareModuleManagement.createMetadata(getOsModule(ds), "metaDataNotVisible", new MetadataValueCreate("withValue", false));

        final Target savedTarget = createTargetAndAssertNoActiveActions();

        final List<Target> saved = assignDistributionSet(ds.getId(), savedTarget.getControllerId(), ActionType.SOFT)
                .getAssignedEntity().stream().map(Action::getTarget).toList();
        implicitLock(ds);
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).hasSize(1);

        final Action action = deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE).getContent().get(0);
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(1);
        assignDistributionSet(ds2, saved).getAssignedEntity();
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).hasSize(2);
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(2);

        final Action uaction = deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE).getContent().get(0);
        assertThat(uaction.getDistributionSet()).isEqualTo(ds);
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).hasSize(2);

        // Run test
        final long current = System.currentTimeMillis();
        performGet(CONTROLLER_BASE, MediaTypes.HAL_JSON, status().isOk(), TenantAware.getCurrentTenant(),
                DEFAULT_CONTROLLER_ID)
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.deploymentBase.href",
                        startsWith(deploymentBaseLink(DEFAULT_CONTROLLER_ID, uaction.getId().toString()))));
        assertLastTargetQueryUpdated(current);
        assertThat(countActionStatusAll()).isEqualTo(2);

        final DistributionSet findDistributionSetByAction = findDsByAction(action.getId()).get();

        getAndVerifyDeploymentBasePayload(DEFAULT_CONTROLLER_ID, MediaType.APPLICATION_JSON, ds, visibleMetadataOsKey,
                visibleMetadataOsValue, artifact, artifactSignature, action.getId(), "attempt", "attempt",
                getOsModule(findDistributionSetByAction));

        // Retrieved is reported
        final List<ActionStatus> actionStatusMessages = deploymentManagement
                .findActionStatusByAction(uaction.getId(), PageRequest.of(0, 100, Direction.DESC, "id")).getContent();
        assertThat(actionStatusMessages).hasSize(2);
        final ActionStatus actionStatusMessage = actionStatusMessages.iterator().next();
        assertThat(actionStatusMessage.getStatus()).isEqualTo(Status.RETRIEVED);
    }

    /**
     * Attempt/soft deployment to a controller including automated switch to hard. Checks if the resource response payload for a given deployment is as expected.
     */
    @Test
    void deploymentAutoForceAction() throws Exception {
        // Prepare test data
        final DistributionSet ds = testdataFactory.createDistributionSet("", true);
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true);

        final Artifact artifact = testdataFactory.createArtifact(nextBytes(ARTIFACT_SIZE), getOsModule(ds), "test1", ARTIFACT_SIZE);
        final Artifact artifactSignature = testdataFactory.createArtifact(nextBytes(ARTIFACT_SIZE),
                getOsModule(ds), "test1.signature", ARTIFACT_SIZE);

        final Target savedTarget = createTargetAndAssertNoActiveActions();

        final List<Target> saved = assignDistributionSet(ds.getId(), savedTarget.getControllerId(),
                ActionType.TIMEFORCED).getAssignedEntity().stream().map(Action::getTarget).toList();
        implicitLock(ds);
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).hasSize(1);

        final Action action = deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE).getContent().get(0);
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(1);
        assignDistributionSet(ds2, saved).getAssignedEntity();
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).hasSize(2);
        assertThat(countActionStatusAll()).isEqualTo(2);

        final Action uaction = deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE).getContent().get(0);
        assertThat(uaction.getDistributionSet()).isEqualTo(ds);
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).hasSize(2);

        // Run test
        final long current = System.currentTimeMillis();
        performGet(CONTROLLER_BASE, MediaTypes.HAL_JSON, status().isOk(), TenantAware.getCurrentTenant(),
                DEFAULT_CONTROLLER_ID)
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.deploymentBase.href",
                        startsWith(deploymentBaseLink(DEFAULT_CONTROLLER_ID, uaction.getId().toString()))));
        assertLastTargetQueryUpdated(current);
        assertThat(countActionStatusAll()).isEqualTo(2);

        final DistributionSet findDistributionSetByAction = findDsByAction(action.getId()).get();
        getAndVerifyDeploymentBasePayload(DEFAULT_CONTROLLER_ID, MediaType.APPLICATION_JSON, ds, artifact,
                artifactSignature, action.getId(),
                findFirstModuleByType(findDistributionSetByAction, osType).orElseThrow().getId(), "forced", "forced");

        getAndVerifyDeploymentBasePayload(DEFAULT_CONTROLLER_ID, MediaTypes.HAL_JSON, ds, artifact, artifactSignature,
                action.getId(), findFirstModuleByType(findDistributionSetByAction, osType).orElseThrow().getId(), "forced",
                "forced");

        // Retrieved is reported
        final Iterable<ActionStatus> actionStatusMessages = deploymentManagement
                .findActionStatusByAction(uaction.getId(), PageRequest.of(0, 100, Direction.DESC, "id")).getContent();
        assertThat(actionStatusMessages).hasSize(2);
        final ActionStatus actionStatusMessage = actionStatusMessages.iterator().next();
        assertThat(actionStatusMessage.getStatus()).isEqualTo(Status.RETRIEVED);
    }

    /**
     * Test download-only (forced + skip) deployment to a controller. Checks if the resource response payload for a given deployment is as expected.
     */
    @Test
    void deploymentDownloadOnlyAction() throws Exception {
        // Prepare test data
        final DistributionSet ds = testdataFactory.createDistributionSet("", true);
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true);
        final Artifact artifact = testdataFactory.createArtifact(nextBytes(ARTIFACT_SIZE), getOsModule(ds), "test1", ARTIFACT_SIZE);
        final Artifact artifactSignature = testdataFactory.createArtifact(
                nextBytes(ARTIFACT_SIZE), getOsModule(ds), "test1.signature", ARTIFACT_SIZE);

        softwareModuleManagement.createMetadata(getOsModule(ds), "metaDataVisible", new MetadataValueCreate("withValue", true));
        softwareModuleManagement.createMetadata(getOsModule(ds), "metaDataNotVisible", new MetadataValueCreate("withValue", false));

        final Target savedTarget = createTargetAndAssertNoActiveActions();

        final List<Target> saved = assignDistributionSet(ds.getId(), savedTarget.getControllerId(), ActionType.DOWNLOAD_ONLY)
                .getAssignedEntity().stream()
                .map(Action::getTarget)
                .toList();
        implicitLock(ds);
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).hasSize(1);

        final Action action = deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE).getContent().get(0);
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(1);
        assignDistributionSet(ds2, saved).getAssignedEntity();
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).hasSize(2);
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(2);

        final Action uaction = deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE).getContent().get(0);
        assertThat(uaction.getDistributionSet()).isEqualTo(ds);
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).hasSize(2);

        // Run test

        final long current = System.currentTimeMillis();
        performGet(CONTROLLER_BASE, MediaTypes.HAL_JSON, status().isOk(), TenantAware.getCurrentTenant(), DEFAULT_CONTROLLER_ID)
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.deploymentBase.href",
                        startsWith(deploymentBaseLink(DEFAULT_CONTROLLER_ID, uaction.getId().toString()))));
        assertLastTargetQueryUpdated(current);
        assertThat(countActionStatusAll()).isEqualTo(2);

        final DistributionSet findDistributionSetByAction = findDsByAction(action.getId()).get();
        getAndVerifyDeploymentBasePayload(DEFAULT_CONTROLLER_ID, MediaType.APPLICATION_JSON, ds, "metaDataVisible",
                "withValue", artifact, artifactSignature, action.getId(), "forced", "skip",
                getOsModule(findDistributionSetByAction));

        // Retrieved is reported
        final List<ActionStatus> actionStatusMessages = deploymentManagement
                .findActionStatusByAction(uaction.getId(), PageRequest.of(0, 100, Direction.DESC, "id")).getContent();
        assertThat(actionStatusMessages).hasSize(2);
        final ActionStatus actionStatusMessage = actionStatusMessages.iterator().next();
        assertThat(actionStatusMessage.getStatus()).isEqualTo(Status.RETRIEVED);
    }

    /**
     * Test various invalid access attempts to the deployment resource und the expected behaviour of the server.
     */
    @Test
    void badDeploymentAction() throws Exception {
        final Target target = testdataFactory.createTarget(DEFAULT_CONTROLLER_ID);

        // not allowed methods
        mvc.perform(post(DEPLOYMENT_BASE, TenantAware.getCurrentTenant(), DEFAULT_CONTROLLER_ID, "1"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(put(DEPLOYMENT_BASE, TenantAware.getCurrentTenant(), DEFAULT_CONTROLLER_ID, "1"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(delete(DEPLOYMENT_BASE, TenantAware.getCurrentTenant(), DEFAULT_CONTROLLER_ID, "1"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        // non existing target
        mvc.perform(MockMvcRequestBuilders.get(DEPLOYMENT_BASE, TenantAware.getCurrentTenant(), "not-existing", "1"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // no deployment
        mvc.perform(MockMvcRequestBuilders.get(DEPLOYMENT_BASE, TenantAware.getCurrentTenant(), DEFAULT_CONTROLLER_ID, "1"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // wrong media type
        final List<Target> toAssign = Collections.singletonList(target);
        final DistributionSet savedSet = testdataFactory.createDistributionSet("");

        final Long actionId = getFirstAssignedActionId(assignDistributionSet(savedSet, toAssign));
        mvc.perform(MockMvcRequestBuilders.get(DEPLOYMENT_BASE, TenantAware.getCurrentTenant(), DEFAULT_CONTROLLER_ID,
                        actionId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        mvc.perform(MockMvcRequestBuilders
                        .get(DEPLOYMENT_BASE, TenantAware.getCurrentTenant(), DEFAULT_CONTROLLER_ID, actionId)
                        .accept(MediaType.APPLICATION_ATOM_XML))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotAcceptable());
    }

    /**
     * The server protects itself against to many feedback upload attempts. The test verifies that
     * it is not possible to exceed the configured maximum number of feedback uploads.
     */
    @Test
    void tooMuchDeploymentActionFeedback() throws Exception {
        final Target target = testdataFactory.createTarget(DEFAULT_CONTROLLER_ID);
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        assignDistributionSet(ds.getId(), DEFAULT_CONTROLLER_ID);
        final Action action = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent().get(0);

        final String feedback = getJsonProceedingDeploymentActionFeedback();
        // assign distribution set creates an action status, so only 99 left
        for (int i = 0; i < 99; i++) {
            postDeploymentFeedback(DEFAULT_CONTROLLER_ID, action.getId(), feedback, status().isOk());
        }

        postDeploymentFeedback(DEFAULT_CONTROLLER_ID, action.getId(), feedback, status().isTooManyRequests());
    }

    /**
     * The server protects itself against too large feedback bodies. The test verifies that
     * it is not possible to exceed the configured maximum number of feedback details.
     */
    @Test
    void tooMuchDeploymentActionMessagesInFeedback() throws Exception {
        final Target target = testdataFactory.createTarget(DEFAULT_CONTROLLER_ID);
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        assignDistributionSet(ds.getId(), DEFAULT_CONTROLLER_ID);
        final Action action = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent().get(0);

        final List<String> messages = new ArrayList<>();
        for (int i = 0; i < quotaManagement.getMaxMessagesPerActionStatus() + 1; i++) {
            messages.add(String.valueOf(i));
        }

        final String feedback = getJsonActionFeedback(DdiStatus.ExecutionStatus.PROCEEDING, DdiResult.FinalResult.NONE, null, messages);
        postDeploymentFeedback(DEFAULT_CONTROLLER_ID, action.getId(), feedback, status().isTooManyRequests());
    }

    /**
     * Multiple uploads of deployment status feedback to the server.
     */
    @Test
    void multipleDeploymentActionFeedback() throws Exception {
        testdataFactory.createTarget(DEFAULT_CONTROLLER_ID);
        testdataFactory.createTarget("4713");
        testdataFactory.createTarget("4714");

        final DistributionSet ds1 = testdataFactory.createDistributionSet("1", true);
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true);
        final DistributionSet ds3 = testdataFactory.createDistributionSet("3", true);

        final Long actionId1 = getFirstAssignedActionId(assignDistributionSet(ds1.getId(), DEFAULT_CONTROLLER_ID));
        implicitLock(ds1);
        final Long actionId2 = getFirstAssignedActionId(assignDistributionSet(ds2.getId(), DEFAULT_CONTROLLER_ID));
        implicitLock(ds2);
        final Long actionId3 = getFirstAssignedActionId(assignDistributionSet(ds3.getId(), DEFAULT_CONTROLLER_ID));
        implicitLock(ds3);

        findTargetAndAssertUpdateStatus(Optional.of(ds3), TargetUpdateStatus.PENDING, 3, Optional.empty());
        assertThat(findByUpdateStatus(TargetUpdateStatus.UNKNOWN, PageRequest.of(0, 10))).hasSize(2);

        // action1 done
        postDeploymentFeedback(DEFAULT_CONTROLLER_ID, actionId1, getJsonClosedDeploymentActionFeedback(), status().isOk());

        findTargetAndAssertUpdateStatus(Optional.of(ds3), TargetUpdateStatus.PENDING, 2, Optional.of(ds1));
        assertStatusMessagesCount(4);

        // action2 done
        postDeploymentFeedback(DEFAULT_CONTROLLER_ID, actionId2, getJsonClosedDeploymentActionFeedback(), status().isOk());

        findTargetAndAssertUpdateStatus(Optional.of(ds3), TargetUpdateStatus.PENDING, 1, Optional.of(ds2));
        assertStatusMessagesCount(5);

        // action3 done
        postDeploymentFeedback(DEFAULT_CONTROLLER_ID, actionId3, getJsonClosedDeploymentActionFeedback(), status().isOk());

        findTargetAndAssertUpdateStatus(Optional.of(ds3), TargetUpdateStatus.IN_SYNC, 0, Optional.of(ds3));
        assertStatusMessagesCount(6);
    }

    /**
     * Verifies that an update action is correctly set to error if the controller provides error feedback.
     */
    @Test
    void rootRsSingleDeploymentActionWithErrorFeedback() throws Exception {
        DistributionSet ds = testdataFactory.createDistributionSet("");
        final Target savedTarget = testdataFactory.createTarget(DEFAULT_CONTROLLER_ID);

        assertThat(targetManagement.getByControllerId(DEFAULT_CONTROLLER_ID).getUpdateStatus()).isEqualTo(TargetUpdateStatus.UNKNOWN);
        assignDistributionSet(ds, Collections.singletonList(savedTarget));
        final Action action = actionRepository
                .findAll(byDistributionSetId(ds.getId()), PAGE)
                .map(Action.class::cast).getContent().get(0);

        postDeploymentFeedback(DEFAULT_CONTROLLER_ID, action.getId(),
                getJsonActionFeedback(
                        DdiStatus.ExecutionStatus.CLOSED, DdiResult.FinalResult.FAILURE, Collections.singletonList("Error message")),
                status().isOk());

        findTargetAndAssertUpdateStatus(Optional.empty(), TargetUpdateStatus.ERROR, 0, Optional.empty());
        assertThat(deploymentManagement.countActionsByTarget(DEFAULT_CONTROLLER_ID)).isEqualTo(1);
        assertTargetCountByStatus(0, 1, 0);

        // redo
        ds = distributionSetManagement.getWithDetails(ds.getId());
        assignDistributionSet(ds, Collections.singletonList(targetManagement.getByControllerId(DEFAULT_CONTROLLER_ID)));
        final Action action2 = deploymentManagement.findActiveActionsByTarget(DEFAULT_CONTROLLER_ID, PAGE).getContent().get(0);
        postDeploymentFeedback(DEFAULT_CONTROLLER_ID, action2.getId(), getJsonClosedCancelActionFeedback(), status().isOk());
        findTargetAndAssertUpdateStatus(Optional.of(ds), TargetUpdateStatus.IN_SYNC, 0, Optional.of(ds));
        assertTargetCountByStatus(0, 0, 1);
        assertThat(countActionStatusAll()).isEqualTo(4);
        assertThat(deploymentManagement.findActionStatusByAction(action.getId(), PAGE).getContent())
                .haveAtLeast(1, new ActionStatusCondition(Status.ERROR));
        assertThat(deploymentManagement.findActionStatusByAction(action2.getId(), PAGE).getContent())
                .haveAtLeast(1, new ActionStatusCondition(Status.FINISHED));
    }

    /**
     * Verifies that the controller can provided as much feedback entries as necessary as long as it is in the configured limits.
     */
    @Test
    void rootRsSingleDeploymentActionFeedback() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        final Long actionId = getFirstAssignedActionId(
                assignDistributionSet(ds,
                        Collections.singletonList(testdataFactory.createTarget(DEFAULT_CONTROLLER_ID))));
        implicitLock(ds);
        findTargetAndAssertUpdateStatus(Optional.of(ds), TargetUpdateStatus.PENDING, 1, Optional.empty());

        // Now valid Feedback
        for (int i = 0; i < 4; i++) {
            postDeploymentFeedback(DEFAULT_CONTROLLER_ID, actionId, getJsonProceedingDeploymentActionFeedback(), status().isOk());
            assertActionStatusCount(i + 2, i);
        }

        postDeploymentFeedback(DEFAULT_CONTROLLER_ID, actionId, getJsonScheduledDeploymentActionFeedback(), status().isOk());
        assertActionStatusCount(6, 5);

        postDeploymentFeedback(DEFAULT_CONTROLLER_ID, actionId, getJsonResumedDeploymentActionFeedback(), status().isOk());
        assertActionStatusCount(7, 6);

        postDeploymentFeedback(DEFAULT_CONTROLLER_ID, actionId, getJsonCanceledDeploymentActionFeedback(), status().isOk());
        assertStatusAndActiveActionsCount(TargetUpdateStatus.PENDING, 1);
        assertActionStatusCount(8, 7, 0, 0, 1);
        assertTargetCountByStatus(1, 0, 0);

        postDeploymentFeedback(DEFAULT_CONTROLLER_ID, actionId, getJsonRejectedDeploymentActionFeedback(), status().isOk());
        assertStatusAndActiveActionsCount(TargetUpdateStatus.PENDING, 1);
        assertActionStatusCount(9, 6, 1, 0, 1);

        postDeploymentFeedback(DEFAULT_CONTROLLER_ID, actionId, getJsonClosedDeploymentActionFeedback(), status().isOk());
        assertStatusAndActiveActionsCount(TargetUpdateStatus.IN_SYNC, 0);
        assertActionStatusCount(10, 7, 1, 1, 1);
        assertTargetCountByStatus(0, 0, 1);

        assertThat(targetManagement.findByInstalledDistributionSet(ds.getId(), PAGE)).hasSize(1);
        assertThat(targetManagement.findByAssignedDistributionSet(ds.getId(), PAGE)).hasSize(1);
    }

    /**
     * Various forbidden request attempts on the feedback resource. Ensures correct answering behaviour as expected to these kind of errors.
     */
    @Test
    void badDeploymentActionFeedback() throws Exception {
        final DistributionSet savedSet = testdataFactory.createDistributionSet("");
        final DistributionSet savedSet2 = testdataFactory.createDistributionSet("1");

        // target does not exist
        postDeploymentFeedback(DEFAULT_CONTROLLER_ID, 1234L, getJsonProceedingDeploymentActionFeedback(), status().isNotFound());

        final Target savedTarget = testdataFactory.createTarget(DEFAULT_CONTROLLER_ID);

        // Action does not exist
        postDeploymentFeedback("4713", 1234L, getJsonProceedingDeploymentActionFeedback(), status().isNotFound());

        assignDistributionSet(savedSet, Collections.singletonList(savedTarget)).getAssignedEntity().iterator().next();
        assignDistributionSet(savedSet2, Collections.singletonList(testdataFactory.createTarget("4713")));

        final Action updateAction = deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE).getContent().get(0);

        // action exists but is not assigned to this target
        postDeploymentFeedback("4713", updateAction.getId(), getJsonActionFeedback(DdiStatus.ExecutionStatus.PROCEEDING,
                DdiResult.FinalResult.NONE, Collections.singletonList("")), status().isNotFound());

        // not allowed methods
        mvc.perform(MockMvcRequestBuilders.get(DEPLOYMENT_FEEDBACK, TenantAware.getCurrentTenant(),
                        DEFAULT_CONTROLLER_ID, "2"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(put(DEPLOYMENT_FEEDBACK, TenantAware.getCurrentTenant(), DEFAULT_CONTROLLER_ID, "2"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(delete(DEPLOYMENT_FEEDBACK, TenantAware.getCurrentTenant(), DEFAULT_CONTROLLER_ID, "2"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
    }

    /**
     * Ensures that an invalid id in feedback body returns a bad request.
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
            @Expect(type = TargetUpdatedEvent.class, count = 1) })
    void invalidIdInFeedbackReturnsBadRequest() throws Exception {
        final Target target = testdataFactory.createTarget("1080");
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        assignDistributionSet(ds.getId(), "1080");
        final Action action = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent().get(0);
        final String invalidFeedback = "{\"id\":\"AAAA\",\"status\":{\"execution\":\"proceeding\",\"result\":{\"finished\":\"none\",\"progress\":{\"cnt\":2,\"of\":5}},\"details\":\"details\"]}}";
        postDeploymentFeedback("1080", action.getId(), invalidFeedback, status().isBadRequest());
    }

    /**
     * Ensures that a missing feedback result in feedback body returns a bad request.
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
            @Expect(type = TargetUpdatedEvent.class, count = 1) })
    void missingResultAttributeInFeedbackReturnsBadRequest() throws Exception {
        final Target target = testdataFactory.createTarget("1080");
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        assignDistributionSet(ds.getId(), "1080");
        final Action action = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent().get(0);

        final String missingResultInFeedback = getJsonActionFeedback(DdiStatus.ExecutionStatus.CLOSED, (DdiResult) null,
                Collections.singletonList("test"));
        postDeploymentFeedback("1080", action.getId(), missingResultInFeedback, status().isBadRequest())
                .andExpect(jsonPath("$.*", hasSize(3)))
                .andExpect(jsonPath("$.exceptionClass", equalTo(MessageNotReadableException.class.getName())));
    }

    /**
     * Ensures that a missing finished result in feedback body returns a bad request.
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
            @Expect(type = TargetUpdatedEvent.class, count = 1) })
    void missingFinishedAttributeInFeedbackReturnsBadRequest() throws Exception {
        final Target target = testdataFactory.createTarget("1080");
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        assignDistributionSet(ds.getId(), "1080");

        final Action action = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent().get(0);
        final String missingFinishedResultInFeedback = getJsonActionFeedback(
                DdiStatus.ExecutionStatus.CLOSED, new DdiResult(null, null), Collections.singletonList("test"));

        postDeploymentFeedback("1080", action.getId(), missingFinishedResultInFeedback, status().isBadRequest())
                .andExpect(jsonPath("$.*", hasSize(3)))
                .andExpect(jsonPath("$.exceptionClass", equalTo(MessageNotReadableException.class.getName())));
    }

    private long countActionStatusAll() {
        return actionStatusRepository.count();
    }

    private void getAndVerifyDeploymentBasePayload(
            final String controllerId, final MediaType mediaType,
            final DistributionSet ds, final String visibleMetadataOsKey, final String visibleMetadataOsValue,
            final Artifact artifact, final Artifact artifactSignature, final Long actionId, final String downloadType,
            final String updateType, final Long osModuleId) throws Exception {
        getAndVerifyDeploymentBasePayload(controllerId, mediaType, ds, artifact, artifactSignature, actionId,
                osModuleId, downloadType, updateType)
                .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].metadata[0].key").value(visibleMetadataOsKey))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].metadata[0].value").value(visibleMetadataOsValue));
    }

    private void assertActionStatusCount(final int actionStatusCount, final int minActionStatusCountInPage) {
        final Target target = targetManagement.getByControllerId(DdiDeploymentBaseTest.DEFAULT_CONTROLLER_ID);
        assertThat(target.getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);

        assertTargetCountByStatus(1, 0, 0);

        assertThat(deploymentManagement.findActiveActionsByTarget(target.getControllerId(), PAGE)).hasSize(1);
        assertThat(countActionStatusAll()).isEqualTo(actionStatusCount);
        assertThat(findActionStatusAll(PAGE).getContent()).haveAtLeast(minActionStatusCountInPage, new ActionStatusCondition(Status.RUNNING));
    }

    private Target createTargetAndAssertNoActiveActions() {
        final Target savedTarget = testdataFactory.createTarget(DdiDeploymentBaseTest.DEFAULT_CONTROLLER_ID);
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).isEmpty();
        assertThat(deploymentManagement.countActionsAll()).isZero();
        assertThat(countActionStatusAll()).isZero();
        return savedTarget;
    }

    private void assertStatusMessagesCount(final int actionStatusMessagesCount) {
        final Iterable<ActionStatus> actionStatusMessages;
        actionStatusMessages = findActionStatusAll(PageRequest.of(0, 100, Direction.DESC, "id")).getContent();
        assertThat(actionStatusMessages).hasSize(actionStatusMessagesCount);
        assertThat(actionStatusMessages).haveAtLeast(1, new ActionStatusCondition(Status.FINISHED));
    }

    private void findTargetAndAssertUpdateStatus(
            final Optional<DistributionSet> ds,
            final TargetUpdateStatus updateStatus, final int activeActions,
            final Optional<DistributionSet> installedDs) {
        final Target myT = targetManagement.getByControllerId(DdiDeploymentBaseTest.DEFAULT_CONTROLLER_ID);
        assertThat(myT.getUpdateStatus()).isEqualTo(updateStatus);
        assertThat(deploymentManagement.findActiveActionsByTarget(myT.getControllerId(), PAGE)).hasSize(activeActions);
        assertThat(deploymentManagement.findAssignedDistributionSet(myT.getControllerId())).isEqualTo(ds);
        assertThat(deploymentManagement.findInstalledDistributionSet(myT.getControllerId())).isEqualTo(installedDs);
    }

    private void assertTargetCountByStatus(final int pending, final int error, final int inSync) {
        assertThat(findByUpdateStatus(TargetUpdateStatus.PENDING, PageRequest.of(0, 10))).hasSize(pending);
        assertThat(findByUpdateStatus(TargetUpdateStatus.ERROR, PageRequest.of(0, 10))).hasSize(error);
        assertThat(findByUpdateStatus(TargetUpdateStatus.IN_SYNC, PageRequest.of(0, 10))).hasSize(inSync);
    }

    private void assertActionStatusCount(final int total, final int running, final int warning, final int finished, final int canceled) {
        assertThat(countActionStatusAll()).isEqualTo(total);
        assertThat(findActionStatusAll(PAGE).getContent()).satisfies(findActionStatusAll -> {
            assertThat(findActionStatusAll).haveAtLeast(running, new ActionStatusCondition(Status.RUNNING));
            assertThat(findActionStatusAll).haveAtLeast(warning, new ActionStatusCondition(Status.WARNING));
            assertThat(findActionStatusAll).haveAtLeast(canceled, new ActionStatusCondition(Status.CANCELED));
            assertThat(findActionStatusAll).haveAtLeast(finished, new ActionStatusCondition(Status.FINISHED));
        });
    }

    private void assertLastTargetQueryUpdated(final long current) {
        assertThat(targetManagement.getByControllerId(DEFAULT_CONTROLLER_ID)).satisfies(target -> {
            assertThat(target.getLastTargetQuery()).isGreaterThanOrEqualTo(current);
            assertThat(target.getLastTargetQuery()).isLessThanOrEqualTo(System.currentTimeMillis());
        });
    }

    private void assertStatusAndActiveActionsCount(final TargetUpdateStatus status, final int activeActions) {
        assertThat(targetManagement.getByControllerId(DdiDeploymentBaseTest.DEFAULT_CONTROLLER_ID)).satisfies(target -> {
            assertThat(target.getUpdateStatus()).isEqualTo(status);
            assertThat(deploymentManagement.findActiveActionsByTarget(target.getControllerId(), PAGE)).hasSize(activeActions);
        });
    }

    private Page<ActionStatus> findActionStatusAll(final Pageable pageable) {
        return JpaManagementHelper.findAllWithCountBySpec(actionStatusRepository, null, pageable);
    }

    private static class ActionStatusCondition extends Condition<ActionStatus> {

        private final Action.Status status;

        private ActionStatusCondition(final Action.Status status) {
            this.status = status;
        }

        @Override
        public boolean matches(final ActionStatus value) {
            return value.getStatus() == status;
        }
    }
}