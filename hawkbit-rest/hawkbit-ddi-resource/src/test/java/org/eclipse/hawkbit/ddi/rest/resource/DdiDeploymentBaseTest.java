/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.api.Condition;
import org.eclipse.hawkbit.ddi.rest.api.DdiRestConstants;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.rest.exception.MessageNotReadableException;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.jayway.jsonpath.JsonPath;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test deployment base from the controller.
 */
@Feature("Component Tests - Direct Device Integration API")
@Story("Deployment Action Resource")
public class DdiDeploymentBaseTest extends AbstractDDiApiIntegrationTest {

    private static final String HTTP_LOCALHOST = "http://localhost:8080/";
    private static final String CONTROLLER_BY_ID = "/{tenant}/controller/v1/{controllerId}";
    private static final String SOFTWARE_MODULE_ARTIFACTS = CONTROLLER_BY_ID
            + "/softwaremodules/{softwareModuleId}/artifacts";
    private static final String DEPLOYMENT_BASE = CONTROLLER_BY_ID + "/deploymentBase/";
    private static final int ARTIFACT_SIZE = 5 * 1024;
    private static final String DEFAULT_CONTROLLER_ID = "4712";

    @Test
    @Description("Ensure that the deployment resource is available as CBOR")
    public void deploymentResourceCbor() throws Exception {
        final Target target = testdataFactory.createTarget();
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("");

        assignDistributionSet(distributionSet.getId(), target.getName());
        final Action action = deploymentManagement.findActiveActionsByTarget(PAGE, target.getControllerId())
                .getContent().get(0);

        // get deployment base
        performGet(DEPLOYMENT_BASE + action.getId().toString(),
                MediaType.parseMediaType(DdiRestConstants.MEDIA_TYPE_CBOR), status().isOk(),
                tenantAware.getCurrentTenant(), target.getControllerId());

        final Long softwareModuleId = distributionSet.getModules().stream().findAny().get().getId();
        testdataFactory.createArtifacts(softwareModuleId);
        // get artifacts
        performGet(SOFTWARE_MODULE_ARTIFACTS, MediaType.parseMediaType(DdiRestConstants.MEDIA_TYPE_CBOR),
                status().isOk(), tenantAware.getCurrentTenant(), target.getControllerId(),
                String.valueOf(softwareModuleId));

        final byte[] feedback = jsonToCbor(
                JsonBuilder.deploymentActionFeedback(action.getId().toString(), "proceeding"));

        postFeedback(MediaType.parseMediaType(DdiRestConstants.MEDIA_TYPE_CBOR), target.getControllerId(),
                action.getId(), feedback, status().isOk());
    }

    @Test
    @Description("Ensures that artifacts are not found, when software module does not exists.")
    public void artifactsNotFound() throws Exception {
        final Target target = testdataFactory.createTarget();
        performGet(SOFTWARE_MODULE_ARTIFACTS, MediaType.APPLICATION_JSON, status().isNotFound(),
                tenantAware.getCurrentTenant(), target.getControllerId(), "1");
    }

    @Test
    @Description("Ensures that artifacts are found, when software module exists.")
    public void artifactsExists() throws Exception {
        final Target target = testdataFactory.createTarget();
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("");

        assignDistributionSet(distributionSet.getId(), target.getName());

        final Long softwareModuleId = distributionSet.getModules().stream().findAny().get().getId();
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

    @Test
    @Description("Forced deployment to a controller. Checks if the resource response payload for a given deployment is as expected.")
    public void deploymentForceAction() throws Exception {
        // Prepare test data
        final DistributionSet ds = testdataFactory.createDistributionSet("", true);
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true);
        final Artifact artifact = testdataFactory.createArtifact(RandomUtils.nextBytes(ARTIFACT_SIZE), getOsModule(ds),
                "test1", ARTIFACT_SIZE);
        final Artifact artifactSignature = testdataFactory.createArtifact(RandomUtils.nextBytes(ARTIFACT_SIZE),
                getOsModule(ds), "test1.signature", ARTIFACT_SIZE);
        final Target savedTarget = createTargetAndAssertNoActiveActions();

        final List<Target> targetsAssignedToDs = assignDistributionSet(ds.getId(), savedTarget.getControllerId(),
                ActionType.FORCED).getAssignedEntity().stream().map(Action::getTarget).collect(Collectors.toList());

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
        performGet(CONTROLLER_BY_ID, MediaTypes.HAL_JSON, status().isOk(), tenantAware.getCurrentTenant(),
                DEFAULT_CONTROLLER_ID)
                        .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                        .andExpect(jsonPath("$._links.deploymentBase.href",
                                startsWith("http://localhost/" + tenantAware.getCurrentTenant() + "/controller/v1/"
                                        + DEFAULT_CONTROLLER_ID + "/deploymentBase/" + uaction.getId())));
        assertThat(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);
        assertThat(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get().getLastTargetQuery())
                .isLessThanOrEqualTo(System.currentTimeMillis());
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(2);

        final DistributionSet findDistributionSetByAction = distributionSetManagement.getByAction(action.getId()).get();

        getAndVerifyDeploymentBasePayload(DEFAULT_CONTROLLER_ID, MediaType.APPLICATION_JSON, ds, artifact,
                artifactSignature, action.getId(),
                findDistributionSetByAction.findFirstModuleByType(osType).get().getId(), "forced", "forced");

        // Retrieved is reported
        final Iterable<ActionStatus> actionStatusMessages = deploymentManagement
                .findActionStatusByAction(PageRequest.of(0, 100, Direction.DESC, "id"), uaction.getId());
        assertThat(actionStatusMessages).hasSize(2);
        final ActionStatus actionStatusMessage = actionStatusMessages.iterator().next();
        assertThat(actionStatusMessage.getStatus()).isEqualTo(Status.RETRIEVED);
    }

    @Test
    @Description("Checks that the deploymentBase URL changes when the action is switched from soft to forced in TIMEFORCED case.")
    public void changeEtagIfActionSwitchesFromSoftToForced() throws Exception {
        // Prepare test data
        final Target target = testdataFactory.createTarget(DEFAULT_CONTROLLER_ID);
        final DistributionSet ds = testdataFactory.createDistributionSet("", true);

        final Long actionId = getFirstAssignedActionId(assignDistributionSet(ds.getId(), target.getControllerId(),
                ActionType.TIMEFORCED, System.currentTimeMillis() + 2_000));

        MvcResult mvcResult = performGet("/{tenant}/controller/v1/" + DEFAULT_CONTROLLER_ID, MediaTypes.HAL_JSON,
                status().isOk(), tenantAware.getCurrentTenant()).andReturn();

        final String urlBeforeSwitch = JsonPath.compile("_links.deploymentBase.href")
                .read(mvcResult.getResponse().getContentAsString()).toString();

        // Time is not yet over, so we should see the same URL
        mvcResult = performGet(CONTROLLER_BY_ID, MediaTypes.HAL_JSON, status().isOk(), tenantAware.getCurrentTenant(),
                DEFAULT_CONTROLLER_ID).andReturn();
        assertThat(JsonPath.compile("_links.deploymentBase.href").read(mvcResult.getResponse().getContentAsString())
                .toString()).isEqualTo(urlBeforeSwitch)
                        .startsWith("http://localhost/" + tenantAware.getCurrentTenant() + "/controller/v1/"
                                + DEFAULT_CONTROLLER_ID + "/deploymentBase/" + actionId);

        // After the time is over we should see a new etag
        TimeUnit.MILLISECONDS.sleep(2_000);

        mvcResult = performGet(CONTROLLER_BY_ID, MediaTypes.HAL_JSON, status().isOk(), tenantAware.getCurrentTenant(),
                DEFAULT_CONTROLLER_ID).andReturn();

        assertThat(JsonPath.compile("_links.deploymentBase.href").read(mvcResult.getResponse().getContentAsString())
                .toString()).isNotEqualTo(urlBeforeSwitch);
    }

    @Test
    @Description("Attempt/soft deployment to a controller. Checks if the resource response payload for a given deployment is as expected.")
    public void deploymentAttemptAction() throws Exception {
        // Prepare test data
        final DistributionSet ds = testdataFactory.createDistributionSet("", true);
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true);
        final String visibleMetadataOsKey = "metaDataVisible";
        final String visibleMetadataOsValue = "withValue";

        final Artifact artifact = testdataFactory.createArtifact(RandomUtils.nextBytes(ARTIFACT_SIZE), getOsModule(ds),
                "test1", ARTIFACT_SIZE);
        final Artifact artifactSignature = testdataFactory.createArtifact(RandomUtils.nextBytes(ARTIFACT_SIZE),
                getOsModule(ds), "test1.signature", ARTIFACT_SIZE);

        softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata().create(getOsModule(ds))
                .key(visibleMetadataOsKey).value(visibleMetadataOsValue).targetVisible(true));
        softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata().create(getOsModule(ds))
                .key("metaDataNotVisible").value("withValue").targetVisible(false));

        final Target savedTarget = createTargetAndAssertNoActiveActions();

        final List<Target> saved = assignDistributionSet(ds.getId(), savedTarget.getControllerId(), ActionType.SOFT)
                .getAssignedEntity().stream().map(Action::getTarget).collect(Collectors.toList());
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(1);

        final Action action = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(1);
        assignDistributionSet(ds2, saved).getAssignedEntity();
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(2);
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(2);

        final Action uaction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);
        assertThat(uaction.getDistributionSet()).isEqualTo(ds);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(2);

        // Run test

        final long current = System.currentTimeMillis();
        performGet(CONTROLLER_BY_ID, MediaTypes.HAL_JSON, status().isOk(), tenantAware.getCurrentTenant(),
                DEFAULT_CONTROLLER_ID)
                        .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                        .andExpect(jsonPath("$._links.deploymentBase.href",
                                startsWith("http://localhost/" + tenantAware.getCurrentTenant() + "/controller/v1/"
                                        + DEFAULT_CONTROLLER_ID + "/deploymentBase/" + uaction.getId())));
        assertThat(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);
        assertThat(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get().getLastTargetQuery())
                .isLessThanOrEqualTo(System.currentTimeMillis());
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(2);

        final DistributionSet findDistributionSetByAction = distributionSetManagement.getByAction(action.getId()).get();

        getAndVerifyDeploymentBasePayload(DEFAULT_CONTROLLER_ID, MediaType.APPLICATION_JSON, ds,
                visibleMetadataOsKey, visibleMetadataOsValue, artifact, artifactSignature, action.getId(), "attempt",
                "attempt", getOsModule(findDistributionSetByAction));

        // Retrieved is reported
        final List<ActionStatus> actionStatusMessages = deploymentManagement
                .findActionStatusByAction(PageRequest.of(0, 100, Direction.DESC, "id"), uaction.getId()).getContent();
        assertThat(actionStatusMessages).hasSize(2);
        final ActionStatus actionStatusMessage = actionStatusMessages.iterator().next();
        assertThat(actionStatusMessage.getStatus()).isEqualTo(Status.RETRIEVED);
    }

    @Test
    @Description("Attempt/soft deployment to a controller including automated switch to hard. Checks if the resource response payload for a given deployment is as expected.")
    public void deploymentAutoForceAction() throws Exception {
        // Prepare test data
        final DistributionSet ds = testdataFactory.createDistributionSet("", true);
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true);

        final Artifact artifact = testdataFactory.createArtifact(RandomUtils.nextBytes(ARTIFACT_SIZE), getOsModule(ds),
                "test1", ARTIFACT_SIZE);
        final Artifact artifactSignature = testdataFactory.createArtifact(RandomUtils.nextBytes(ARTIFACT_SIZE),
                getOsModule(ds), "test1.signature", ARTIFACT_SIZE);

        final Target savedTarget = createTargetAndAssertNoActiveActions();

        final List<Target> saved = assignDistributionSet(ds.getId(), savedTarget.getControllerId(),
                ActionType.TIMEFORCED)
                .getAssignedEntity().stream().map(Action::getTarget).collect(Collectors.toList());
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(1);

        final Action action = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(1);
        assignDistributionSet(ds2, saved).getAssignedEntity();
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(2);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(2);

        final Action uaction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);
        assertThat(uaction.getDistributionSet()).isEqualTo(ds);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(2);

        // Run test

        final long current = System.currentTimeMillis();
        performGet(CONTROLLER_BY_ID, MediaTypes.HAL_JSON, status().isOk(), tenantAware.getCurrentTenant(),
                DEFAULT_CONTROLLER_ID)
                        .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                        .andExpect(jsonPath("$._links.deploymentBase.href",
                                startsWith("http://localhost/" + tenantAware.getCurrentTenant() + "/controller/v1/"
                                        + DEFAULT_CONTROLLER_ID + "/deploymentBase/" + uaction.getId())));
        assertThat(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);
        assertThat(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get().getLastTargetQuery())
                .isLessThanOrEqualTo(System.currentTimeMillis());
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(2);

        final DistributionSet findDistributionSetByAction = distributionSetManagement.getByAction(action.getId()).get();

        getAndVerifyDeploymentBasePayload(DEFAULT_CONTROLLER_ID, MediaType.APPLICATION_JSON, ds, artifact,
                artifactSignature, action.getId(),
                findDistributionSetByAction.findFirstModuleByType(osType).get().getId(), "forced", "forced");

        getAndVerifyDeploymentBasePayload(DEFAULT_CONTROLLER_ID, MediaTypes.HAL_JSON, ds, artifact,
                artifactSignature, action.getId(),
                findDistributionSetByAction.findFirstModuleByType(osType).get().getId(), "forced", "forced");

        // Retrieved is reported
        final Iterable<ActionStatus> actionStatusMessages = deploymentManagement
                .findActionStatusByAction(PageRequest.of(0, 100, Direction.DESC, "id"), uaction.getId()).getContent();
        assertThat(actionStatusMessages).hasSize(2);
        final ActionStatus actionStatusMessage = actionStatusMessages.iterator().next();
        assertThat(actionStatusMessage.getStatus()).isEqualTo(Status.RETRIEVED);
    }

    @Test
    @Description("Test download-only (forced + skip) deployment to a controller. Checks if the resource response payload for a given deployment is as expected.")
    public void deploymentDownloadOnlyAction() throws Exception {
        // Prepare test data
        final DistributionSet ds = testdataFactory.createDistributionSet("", true);
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true);
        final Artifact artifact = testdataFactory.createArtifact(RandomUtils.nextBytes(ARTIFACT_SIZE), getOsModule(ds),
                "test1", ARTIFACT_SIZE);
        final Artifact artifactSignature = testdataFactory.createArtifact(RandomUtils.nextBytes(ARTIFACT_SIZE),
                getOsModule(ds), "test1.signature", ARTIFACT_SIZE);

        softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata().create(getOsModule(ds))
                .key("metaDataVisible").value("withValue").targetVisible(true));
        softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata().create(getOsModule(ds))
                .key("metaDataNotVisible").value("withValue").targetVisible(false));

        final Target savedTarget = createTargetAndAssertNoActiveActions();

        final List<Target> saved = assignDistributionSet(ds.getId(), savedTarget.getControllerId(),
                ActionType.DOWNLOAD_ONLY)
                .getAssignedEntity().stream().map(Action::getTarget).collect(Collectors.toList());
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(1);

        final Action action = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(1);
        assignDistributionSet(ds2, saved).getAssignedEntity();
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(2);
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(2);

        final Action uaction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);
        assertThat(uaction.getDistributionSet()).isEqualTo(ds);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(2);

        // Run test

        final long current = System.currentTimeMillis();
        performGet(CONTROLLER_BY_ID, MediaTypes.HAL_JSON, status().isOk(), tenantAware.getCurrentTenant(),
                DEFAULT_CONTROLLER_ID)
                        .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                        .andExpect(jsonPath("$._links.deploymentBase.href",
                                startsWith("http://localhost/" + tenantAware.getCurrentTenant() + "/controller/v1/"
                                        + DEFAULT_CONTROLLER_ID + "/deploymentBase/" + uaction.getId())));
        assertThat(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);
        assertThat(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get().getLastTargetQuery())
                .isLessThanOrEqualTo(System.currentTimeMillis());
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(2);

        final DistributionSet findDistributionSetByAction = distributionSetManagement.getByAction(action.getId()).get();

        getAndVerifyDeploymentBasePayload(DEFAULT_CONTROLLER_ID, MediaType.APPLICATION_JSON, ds, "metaDataVisible",
                "withValue", artifact, artifactSignature, action.getId(), "forced", "skip",
                getOsModule(findDistributionSetByAction));

        // Retrieved is reported
        final List<ActionStatus> actionStatusMessages = deploymentManagement
                .findActionStatusByAction(PageRequest.of(0, 100, Direction.DESC, "id"), uaction.getId()).getContent();
        assertThat(actionStatusMessages).hasSize(2);
        final ActionStatus actionStatusMessage = actionStatusMessages.iterator().next();
        assertThat(actionStatusMessage.getStatus()).isEqualTo(Status.RETRIEVED);

    }

    private void getAndVerifyDeploymentBasePayload(final String controllerId, final MediaType mediaType,
            final DistributionSet ds, final String visibleMetadataOsKey, final String visibleMetadataOsValue,
            final Artifact artifact, final Artifact artifactSignature, final Long actionId, final String downloadType,
            final String updateType, final Long osModuleId) throws Exception {
        getAndVerifyDeploymentBasePayload(controllerId, mediaType, ds, artifact, artifactSignature, actionId,
                osModuleId, downloadType, updateType).andExpect(
                        jsonPath("$.deployment.chunks[?(@.part=='os')].metadata[0].key").value(visibleMetadataOsKey))
                        .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].metadata[0].value")
                                .value(visibleMetadataOsValue));
    }

    private ResultActions getAndVerifyDeploymentBasePayload(final String controllerId, final MediaType mediaType,
            final DistributionSet ds, final Artifact artifact, final Artifact artifactSignature, final Long actionId,
            final Long osModuleId, final String downloadType, final String updateType) throws Exception {
        return performGet("/{tenant}/controller/v1/" + controllerId + "/deploymentBase/{actionId}", mediaType,
                status().isOk(), tenantAware.getCurrentTenant(), actionId.toString())
                        .andExpect(jsonPath("$.id", equalTo(String.valueOf(actionId))))
                        .andExpect(jsonPath("$.deployment.download", equalTo(downloadType)))
                        .andExpect(jsonPath("$.deployment.update", equalTo(updateType)))
                        .andExpect(jsonPath("$.deployment.chunks[?(@.part=='jvm')].name",
                                contains(ds.findFirstModuleByType(runtimeType).get().getName())))
                        .andExpect(jsonPath("$.deployment.chunks[?(@.part=='jvm')].version",
                                contains(ds.findFirstModuleByType(runtimeType).get().getVersion())))
                        .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].name",
                                contains(ds.findFirstModuleByType(osType).get().getName())))
                        .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].version",
                                contains(ds.findFirstModuleByType(osType).get().getVersion())))
                        .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].artifacts[0].size",
                                contains(ARTIFACT_SIZE)))
                        .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].artifacts[0].filename",
                                contains("test1")))
                        .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].artifacts[0].hashes.md5",
                                contains(artifact.getMd5Hash())))
                        .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].artifacts[0].hashes.sha1",
                                contains(artifact.getSha1Hash())))
                        .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].artifacts[0].hashes.sha256",
                                contains(artifact.getSha256Hash())))
                        .andExpect(jsonPath(
                                "$.deployment.chunks[?(@.part=='os')].artifacts[0]._links.download-http.href",
                                contains(HTTP_LOCALHOST + tenantAware.getCurrentTenant() + "/controller/v1/"
                                        + controllerId + "/softwaremodules/" + osModuleId + "/artifacts/test1")))
                        .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].artifacts[0]._links.md5sum-http.href",
                                contains(HTTP_LOCALHOST + tenantAware.getCurrentTenant() + "/controller/v1/"
                                        + controllerId + "/softwaremodules/" + osModuleId + "/artifacts/test1.MD5SUM")))
                        .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].artifacts[1].size",
                                contains(ARTIFACT_SIZE)))
                        .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].artifacts[1].filename",
                                contains("test1.signature")))
                        .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].artifacts[1].hashes.md5",
                                contains(artifactSignature.getMd5Hash())))
                        .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].artifacts[1].hashes.sha1",
                                contains(artifactSignature.getSha1Hash())))
                        .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].artifacts[1].hashes.sha256",
                                contains(artifactSignature.getSha256Hash())))
                        .andExpect(
                                jsonPath("$.deployment.chunks[?(@.part=='os')].artifacts[1]._links.download-http.href",
                                        contains(HTTP_LOCALHOST + tenantAware.getCurrentTenant() + "/controller/v1/"
                                                + controllerId + "/softwaremodules/" + osModuleId
                                                + "/artifacts/test1.signature")))
                        .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].artifacts[1]._links.md5sum-http.href",
                                contains(HTTP_LOCALHOST + tenantAware.getCurrentTenant() + "/controller/v1/"
                                        + controllerId + "/softwaremodules/" + osModuleId
                                        + "/artifacts/test1.signature.MD5SUM")))
                        .andExpect(jsonPath("$.deployment.chunks[?(@.part=='bApp')].version",
                                contains(ds.findFirstModuleByType(appType).get().getVersion())))
                        .andExpect(jsonPath("$.deployment.chunks[?(@.part=='bApp')].metadata").doesNotExist())
                        .andExpect(jsonPath("$.deployment.chunks[?(@.part=='bApp')].name")
                                .value(ds.findFirstModuleByType(appType).get().getName()));
    }

    @Test
    @Description("Test various invalid access attempts to the deployment resource und the expected behaviour of the server.")
    public void badDeploymentAction() throws Exception {
        final Target target = testdataFactory.createTarget(DEFAULT_CONTROLLER_ID);

        // not allowed methods
        mvc.perform(post(DEPLOYMENT_BASE + "1", tenantAware.getCurrentTenant(), DEFAULT_CONTROLLER_ID))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        mvc.perform(put(DEPLOYMENT_BASE + "1", tenantAware.getCurrentTenant(), DEFAULT_CONTROLLER_ID))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        mvc.perform(delete(DEPLOYMENT_BASE + "1", tenantAware.getCurrentTenant(), DEFAULT_CONTROLLER_ID))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        // non existing target
        mvc.perform(MockMvcRequestBuilders.get(DEPLOYMENT_BASE + "1", tenantAware.getCurrentTenant(), "not-existing"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // no deployment
        mvc.perform(MockMvcRequestBuilders.get(DEPLOYMENT_BASE + "1", tenantAware.getCurrentTenant(),
                DEFAULT_CONTROLLER_ID)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // wrong media type
        final List<Target> toAssign = Collections.singletonList(target);
        final DistributionSet savedSet = testdataFactory.createDistributionSet("");

        final Long actionId = getFirstAssignedActionId(assignDistributionSet(savedSet, toAssign));
        mvc.perform(MockMvcRequestBuilders.get(DEPLOYMENT_BASE + actionId, tenantAware.getCurrentTenant(),
                DEFAULT_CONTROLLER_ID)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        mvc.perform(MockMvcRequestBuilders
                .get(DEPLOYMENT_BASE + actionId, tenantAware.getCurrentTenant(), DEFAULT_CONTROLLER_ID)
                .accept(MediaType.APPLICATION_ATOM_XML)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotAcceptable());
    }

    @Test
    @Description("The server protects itself against to many feedback upload attempts. The test verifies that "
            + "it is not possible to exceed the configured maximum number of feedback uploads.")
    public void tooMuchDeploymentActionFeedback() throws Exception {
        final Target target = testdataFactory.createTarget(DEFAULT_CONTROLLER_ID);
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        assignDistributionSet(ds.getId(), DEFAULT_CONTROLLER_ID);
        final Action action = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent()
                .get(0);

        final String feedback = JsonBuilder.deploymentActionFeedback(action.getId().toString(), "proceeding");
        // assign distribution set creates an action status, so only 99 left
        for (int i = 0; i < 99; i++) {
            postFeedback(MediaType.APPLICATION_JSON, DEFAULT_CONTROLLER_ID, action.getId(), feedback, status().isOk());
        }

        postFeedback(MediaType.APPLICATION_JSON, DEFAULT_CONTROLLER_ID, action.getId(), feedback,
                status().isForbidden());
    }

    @Test
    @Description("The server protects itself against too large feedback bodies. The test verifies that "
            + "it is not possible to exceed the configured maximum number of feedback details.")
    public void tooMuchDeploymentActionMessagesInFeedback() throws Exception {
        final Target target = testdataFactory.createTarget(DEFAULT_CONTROLLER_ID);
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        assignDistributionSet(ds.getId(), DEFAULT_CONTROLLER_ID);
        final Action action = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent()
                .get(0);

        final List<String> messages = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            messages.add(String.valueOf(i));
        }

        final String feedback = JsonBuilder.deploymentActionFeedback(action.getId().toString(), "proceeding", "none",
                messages);
        postFeedback(MediaType.APPLICATION_JSON, DEFAULT_CONTROLLER_ID, action.getId(), feedback,
                status().isForbidden());
    }

    @Test
    @Description("Multiple uploads of deployment status feedback to the server.")
    public void multipleDeploymentActionFeedback() throws Exception {
        testdataFactory.createTarget(DEFAULT_CONTROLLER_ID);
        testdataFactory.createTarget("4713");
        testdataFactory.createTarget("4714");

        final DistributionSet ds1 = testdataFactory.createDistributionSet("1", true);
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true);
        final DistributionSet ds3 = testdataFactory.createDistributionSet("3", true);

        final Long actionId1 = getFirstAssignedActionId(assignDistributionSet(ds1.getId(), DEFAULT_CONTROLLER_ID));
        final Long actionId2 = getFirstAssignedActionId(assignDistributionSet(ds2.getId(), DEFAULT_CONTROLLER_ID));
        final Long actionId3 = getFirstAssignedActionId(assignDistributionSet(ds3.getId(), DEFAULT_CONTROLLER_ID));

        findTargetAndAssertUpdateStatus(Optional.of(ds3), TargetUpdateStatus.PENDING, 3, Optional.empty());
        assertThat(targetManagement.findByUpdateStatus(PageRequest.of(0, 10), TargetUpdateStatus.UNKNOWN)).hasSize(2);

        // action1 done
        postFeedback(MediaType.APPLICATION_JSON, DEFAULT_CONTROLLER_ID, actionId1,
                JsonBuilder.deploymentActionFeedback(actionId1.toString(), "closed"), status().isOk());

        findTargetAndAssertUpdateStatus(Optional.of(ds3), TargetUpdateStatus.PENDING, 2, Optional.of(ds1));
        assertStatusMessagesCount(4);

        // action2 done
        postFeedback(MediaType.APPLICATION_JSON, DEFAULT_CONTROLLER_ID, actionId2,
                JsonBuilder.deploymentActionFeedback(actionId2.toString(), "closed"), status().isOk());

        findTargetAndAssertUpdateStatus(Optional.of(ds3), TargetUpdateStatus.PENDING, 1, Optional.of(ds2));
        assertStatusMessagesCount(5);

        // action3 done
        postFeedback(MediaType.APPLICATION_JSON, DEFAULT_CONTROLLER_ID, actionId3,
                JsonBuilder.deploymentActionFeedback(actionId3.toString(), "closed"), status().isOk());

        findTargetAndAssertUpdateStatus(Optional.of(ds3), TargetUpdateStatus.IN_SYNC, 0, Optional.of(ds3));
        assertStatusMessagesCount(6);

    }

    @Test
    @Description("Verifies that an update action is correctly set to error if the controller provides error feedback.")
    public void rootRsSingleDeploymentActionWithErrorFeedback() throws Exception {
        DistributionSet ds = testdataFactory.createDistributionSet("");
        final Target savedTarget = testdataFactory.createTarget(DEFAULT_CONTROLLER_ID);

        assertThat(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.UNKNOWN);
        assignDistributionSet(ds, Collections.singletonList(savedTarget));
        final Action action = deploymentManagement.findActionsByDistributionSet(PAGE, ds.getId()).getContent().get(0);

        postFeedback(MediaType.APPLICATION_JSON, DEFAULT_CONTROLLER_ID, action.getId(),
                JsonBuilder.deploymentActionFeedback(action.getId().toString(), "closed", "failure", "error message"),
                status().isOk());

        findTargetAndAssertUpdateStatus(Optional.empty(), TargetUpdateStatus.ERROR, 0, Optional.empty());
        assertThat(deploymentManagement.countActionsByTarget(DEFAULT_CONTROLLER_ID)).isEqualTo(1);
        assertTargetCountByStatus(0, 1, 0);

        // redo
        ds = distributionSetManagement.getWithDetails(ds.getId()).get();
        assignDistributionSet(ds,
                Collections.singletonList(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get()));
        final Action action2 = deploymentManagement.findActiveActionsByTarget(PAGE, DEFAULT_CONTROLLER_ID).getContent()
                .get(0);
        postFeedback(MediaType.APPLICATION_JSON, DEFAULT_CONTROLLER_ID, action2.getId(),
                JsonBuilder.deploymentActionFeedback(action2.getId().toString(), "closed", "SUCCESS"), status().isOk());
        findTargetAndAssertUpdateStatus(Optional.of(ds), TargetUpdateStatus.IN_SYNC, 0, Optional.of(ds));
        assertTargetCountByStatus(0, 0, 1);
        assertThat(deploymentManagement.findInActiveActionsByTarget(PAGE, DEFAULT_CONTROLLER_ID)).hasSize(2);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(4);
        assertThat(deploymentManagement.findActionStatusByAction(PAGE, action.getId()).getContent()).haveAtLeast(1,
                new ActionStatusCondition(Status.ERROR));
        assertThat(deploymentManagement.findActionStatusByAction(PAGE, action2.getId()).getContent()).haveAtLeast(1,
                new ActionStatusCondition(Status.FINISHED));

    }

    @Test
    @Description("Verifies that the controller can provided as much feedback entries as necessary as long as it is in the configured limits.")
    public void rootRsSingleDeploymentActionFeedback() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        final Long actionId = getFirstAssignedActionId(assignDistributionSet(ds,
                Collections.singletonList(testdataFactory.createTarget(DEFAULT_CONTROLLER_ID))));
        findTargetAndAssertUpdateStatus(Optional.of(ds), TargetUpdateStatus.PENDING, 1, Optional.empty());

        // Now valid Feedback
        for (int i = 0; i < 4; i++) {
            postFeedback(MediaType.APPLICATION_JSON, DEFAULT_CONTROLLER_ID, actionId,
                    JsonBuilder.deploymentActionFeedback(actionId.toString(), "proceeding"), status().isOk());
            assertActionStatusCount(i + 2, i);

        }

        postFeedback(MediaType.APPLICATION_JSON, DEFAULT_CONTROLLER_ID, actionId,
                JsonBuilder.deploymentActionFeedback(actionId.toString(), "scheduled"), status().isOk());
        assertActionStatusCount(6, 5);

        postFeedback(MediaType.APPLICATION_JSON, DEFAULT_CONTROLLER_ID, actionId,
                JsonBuilder.deploymentActionFeedback(actionId.toString(), "resumed"), status().isOk());
        assertActionStatusCount(7, 6);

        postFeedback(MediaType.APPLICATION_JSON, DEFAULT_CONTROLLER_ID, actionId,
                JsonBuilder.deploymentActionFeedback(actionId.toString(), "canceled"), status().isOk());
        assertStatusAndActiveActionsCount(TargetUpdateStatus.PENDING, 1);
        assertActionStatusCount(8, 7, 0, 0, 1);
        assertTargetCountByStatus(1, 0, 0);

        postFeedback(MediaType.APPLICATION_JSON, DEFAULT_CONTROLLER_ID, actionId,
                JsonBuilder.deploymentActionFeedback(actionId.toString(), "rejected"), status().isOk());
        assertStatusAndActiveActionsCount(TargetUpdateStatus.PENDING, 1);
        assertActionStatusCount(9, 6, 1, 0, 1);

        postFeedback(MediaType.APPLICATION_JSON, DEFAULT_CONTROLLER_ID, actionId,
                JsonBuilder.deploymentActionFeedback(actionId.toString(), "closed"), status().isOk());
        assertStatusAndActiveActionsCount(TargetUpdateStatus.IN_SYNC, 0);
        assertActionStatusCount(10, 7, 1, 1, 1);
        assertTargetCountByStatus(0, 0, 1);

        assertThat(targetManagement.findByInstalledDistributionSet(PAGE, ds.getId())).hasSize(1);
        assertThat(targetManagement.findByAssignedDistributionSet(PAGE, ds.getId())).hasSize(1);
    }

    @Test
    @Description("Various forbidden request attempts on the feedback resource. Ensures correct answering behaviour as expected to these kind of errors.")
    public void badDeploymentActionFeedback() throws Exception {
        final DistributionSet savedSet = testdataFactory.createDistributionSet("");
        final DistributionSet savedSet2 = testdataFactory.createDistributionSet("1");

        // target does not exist

        postFeedback(MediaType.APPLICATION_JSON, DEFAULT_CONTROLLER_ID, 1234L,
                JsonBuilder.deploymentActionInProgressFeedback("1234"), status().isNotFound());

        final Target savedTarget = testdataFactory.createTarget(DEFAULT_CONTROLLER_ID);

        // Action does not exists
        postFeedback(MediaType.APPLICATION_JSON, "4713", 1234L, JsonBuilder.deploymentActionInProgressFeedback("1234"),
                status().isNotFound());

        assignDistributionSet(savedSet, Collections.singletonList(savedTarget)).getAssignedEntity().iterator().next();
        assignDistributionSet(savedSet2, Collections.singletonList(testdataFactory.createTarget("4713")));

        final Action updateAction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);

        // action exists but is not assigned to this target
        postFeedback(MediaType.APPLICATION_JSON, "4713", updateAction.getId(),
                JsonBuilder.deploymentActionInProgressFeedback(updateAction.getId().toString()), status().isNotFound());

        // not allowed methods
        mvc.perform(MockMvcRequestBuilders.get(DEPLOYMENT_BASE + "2/feedback", tenantAware.getCurrentTenant(),
                DEFAULT_CONTROLLER_ID)).andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        mvc.perform(put(DEPLOYMENT_BASE + "2/feedback", tenantAware.getCurrentTenant(), DEFAULT_CONTROLLER_ID))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        mvc.perform(delete(DEPLOYMENT_BASE + "2/feedback", tenantAware.getCurrentTenant(), DEFAULT_CONTROLLER_ID))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

    }

    @Test
    @Description("Ensures that an invalid id in feedback body returns a bad request.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 1)})
    public void invalidIdInFeedbackReturnsBadRequest() throws Exception {
        final Target target = testdataFactory.createTarget("1080");
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        assignDistributionSet(ds.getId(), "1080");
        final Action action = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent()
                .get(0);
        postFeedback(MediaType.APPLICATION_JSON, "1080", action.getId(),
                JsonBuilder.deploymentActionInProgressFeedback("AAAA"), status().isBadRequest());
    }

    @Test
    @Description("Ensures that a missing feedback result in feedback body returns a bad request.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 1)})
    public void missingResultAttributeInFeedbackReturnsBadRequest() throws Exception {

        final Target target = testdataFactory.createTarget("1080");
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        assignDistributionSet(ds.getId(), "1080");
        final Action action = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent()
                .get(0);
        final String missingResultInFeedback = JsonBuilder.missingResultInFeedback(action.getId().toString(), "closed",
                "test");
        postFeedback(MediaType.APPLICATION_JSON, "1080", action.getId(), missingResultInFeedback,
                status().isBadRequest()).andExpect(jsonPath("$.*", hasSize(3))).andExpect(
                        jsonPath("$.exceptionClass", equalTo(MessageNotReadableException.class.getCanonicalName())));
    }

    @Test
    @Description("Ensures that a missing finished result in feedback body returns a bad request.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 1)})
    public void missingFinishedAttributeInFeedbackReturnsBadRequest() throws Exception {

        final Target target = testdataFactory.createTarget("1080");
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        assignDistributionSet(ds.getId(), "1080");

        final Action action = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent()
                .get(0);
        final String missingFinishedResultInFeedback = JsonBuilder
                .missingFinishedResultInFeedback(action.getId().toString(), "closed", "test");
        postFeedback(MediaType.APPLICATION_JSON, "1080", action.getId(), missingFinishedResultInFeedback,
                status().isBadRequest()).andExpect(jsonPath("$.*", hasSize(3))).andExpect(
                        jsonPath("$.exceptionClass", equalTo(MessageNotReadableException.class.getCanonicalName())));
    }

    private void assertActionStatusCount(final int actionStatusCount, final int minActionStatusCountInPage) {
        final Target target = targetManagement.getByControllerID(DdiDeploymentBaseTest.DEFAULT_CONTROLLER_ID).get();
        assertThat(target.getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);

        assertTargetCountByStatus(1, 0, 0);

        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, target.getControllerId())).hasSize(1);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(actionStatusCount);
        assertThat(deploymentManagement.findActionStatusAll(PAGE).getContent()).haveAtLeast(minActionStatusCountInPage,
                new ActionStatusCondition(Status.RUNNING));
    }

    private class ActionStatusCondition extends Condition<ActionStatus> {
        private final Status status;

        public ActionStatusCondition(final Status status) {
            this.status = status;
        }

        @Override
        public boolean matches(final ActionStatus value) {
            return value.getStatus() == status;
        }
    }

    private ResultActions performGet(final String url, final MediaType mediaType, final ResultMatcher statusMatcher,
            final String... values) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get(url, values).accept(mediaType))
                .andDo(MockMvcResultPrinter.print()).andExpect(statusMatcher)
                .andExpect(content().contentTypeCompatibleWith(mediaType));
    }

    private ResultActions postFeedback(final MediaType mediaType, final String controllerId, final Long id,
            final String content, final ResultMatcher statusMatcher) throws Exception {
        return postFeedback(mediaType, controllerId, id, content.getBytes(), statusMatcher);
    }

    private ResultActions postFeedback(final MediaType mediaType, final String controllerId, final Long id,
            final byte[] content, final ResultMatcher statusMatcher) throws Exception {
        return mvc
                .perform(post(DEPLOYMENT_BASE + id + "/feedback", tenantAware.getCurrentTenant(), controllerId)
                        .content(content).contentType(mediaType).accept(mediaType))
                .andDo(MockMvcResultPrinter.print()).andExpect(statusMatcher);
    }

    private Target createTargetAndAssertNoActiveActions() {
        final Target savedTarget = testdataFactory.createTarget(DdiDeploymentBaseTest.DEFAULT_CONTROLLER_ID);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).isEmpty();
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(0);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(0);
        return savedTarget;
    }

    private void assertStatusMessagesCount(final int actionStatusMessagesCount) {
        final Iterable<ActionStatus> actionStatusMessages;
        actionStatusMessages = deploymentManagement.findActionStatusAll(PageRequest.of(0, 100, Direction.DESC, "id"))
                .getContent();
        assertThat(actionStatusMessages).hasSize(actionStatusMessagesCount);
        assertThat(actionStatusMessages).haveAtLeast(1, new ActionStatusCondition(Status.FINISHED));
    }

    private void findTargetAndAssertUpdateStatus(final Optional<DistributionSet> ds,
            final TargetUpdateStatus updateStatus, final int activeActions,
            final Optional<DistributionSet> installedDs) {
        final Target myT = targetManagement.getByControllerID(DdiDeploymentBaseTest.DEFAULT_CONTROLLER_ID).get();
        assertThat(myT.getUpdateStatus()).isEqualTo(updateStatus);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, myT.getControllerId())).hasSize(activeActions);
        assertThat(deploymentManagement.getAssignedDistributionSet(myT.getControllerId())).isEqualTo(ds);
        assertThat(deploymentManagement.getInstalledDistributionSet(myT.getControllerId())).isEqualTo(installedDs);
    }

    private void assertTargetCountByStatus(final int pending, final int error, final int inSync) {
        assertThat(targetManagement.findByUpdateStatus(PageRequest.of(0, 10), TargetUpdateStatus.PENDING))
                .hasSize(pending);
        assertThat(targetManagement.findByUpdateStatus(PageRequest.of(0, 10), TargetUpdateStatus.ERROR)).hasSize(error);
        assertThat(targetManagement.findByUpdateStatus(PageRequest.of(0, 10), TargetUpdateStatus.IN_SYNC))
                .hasSize(inSync);
    }

    private void assertActionStatusCount(final int total, final int running, final int warning, final int finished,
            final int canceled) {
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(total);
        assertThat(deploymentManagement.findActionStatusAll(PAGE).getContent()).haveAtLeast(running,
                new ActionStatusCondition(Status.RUNNING));
        assertThat(deploymentManagement.findActionStatusAll(PAGE).getContent()).haveAtLeast(warning,
                new ActionStatusCondition(Status.WARNING));
        assertThat(deploymentManagement.findActionStatusAll(PAGE).getContent()).haveAtLeast(canceled,
                new ActionStatusCondition(Status.CANCELED));
        assertThat(deploymentManagement.findActionStatusAll(PAGE).getContent()).haveAtLeast(finished,
                new ActionStatusCondition(Status.FINISHED));
    }

    private void assertStatusAndActiveActionsCount(final TargetUpdateStatus status, final int activeActions) {
        final Target target = targetManagement.getByControllerID(DdiDeploymentBaseTest.DEFAULT_CONTROLLER_ID).get();
        assertThat(target.getUpdateStatus()).isEqualTo(status);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, target.getControllerId()))
                .hasSize(activeActions);
    }
}
