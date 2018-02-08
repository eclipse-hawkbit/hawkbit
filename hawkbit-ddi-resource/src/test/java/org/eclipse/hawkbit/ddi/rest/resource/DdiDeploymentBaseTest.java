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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.api.Condition;
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
import org.eclipse.hawkbit.repository.model.RepositoryModelConstants;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test deployment base from the controller.
 */
@Features("Component Tests - Direct Device Integration API")
@Stories("Deployment Action Resource")
public class DdiDeploymentBaseTest extends AbstractDDiApiIntegrationTest {

    private static final String HTTP_LOCALHOST = "http://localhost:8080/";

    @Test
    @Description("Ensures that artifacts are not found, when softare module does not exists.")
    public void artifactsNotFound() throws Exception {
        final Target target = testdataFactory.createTarget();
        final Long softwareModuleIdNotExist = 1l;
        mvc.perform(get("/{tenant}/controller/v1/{targetNotExist}/softwaremodules/{softwareModuleId}/artifacts",
                tenantAware.getCurrentTenant(), target.getName(), softwareModuleIdNotExist))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());
    }

    @Test
    @Description("Ensures that artifacts are found, when software module exists.")
    public void artifactsExists() throws Exception {
        final Target target = testdataFactory.createTarget();
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("");

        assignDistributionSet(distributionSet.getId(), target.getName());

        final Long softwareModuleId = distributionSet.getModules().stream().findAny().get().getId();
        mvc.perform(get("/{tenant}/controller/v1/{targetNotExist}/softwaremodules/{softwareModuleId}/artifacts",
                tenantAware.getCurrentTenant(), target.getName(), softwareModuleId)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));

        testdataFactory.createArtifacts(softwareModuleId);

        mvc.perform(get("/{tenant}/controller/v1/{targetNotExist}/softwaremodules/{softwareModuleId}/artifacts",
                tenantAware.getCurrentTenant(), target.getName(), softwareModuleId)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$.[?(@.filename==filename0)]", hasSize(1)))
                .andExpect(jsonPath("$.[?(@.filename==filename1)]", hasSize(1)))
                .andExpect(jsonPath("$.[?(@.filename==filename2)]", hasSize(1)));

    }

    @Test
    @Description("Forced deployment to a controller. Checks if the resource reponse payload for a given deployment is as expected.")
    public void deplomentForceAction() throws Exception {
        // Prepare test data
        final DistributionSet ds = testdataFactory.createDistributionSet("", true);
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true);

        final byte random[] = RandomUtils.nextBytes(5 * 1024);
        final Artifact artifact = artifactManagement.create(new ByteArrayInputStream(random), getOsModule(ds), "test1",
                false);
        final Artifact artifactSignature = artifactManagement.create(new ByteArrayInputStream(random), getOsModule(ds),
                "test1.signature", false);

        final Target savedTarget = testdataFactory.createTarget("4712");

        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).isEmpty();
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(0);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(0);

        List<Target> saved = deploymentManagement.assignDistributionSet(ds.getId(), ActionType.FORCED,
                RepositoryModelConstants.NO_FORCE_TIME, Arrays.asList(savedTarget.getControllerId()))
                .getAssignedEntity();
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(1);

        final Action action = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(1);
        saved = assignDistributionSet(ds2, saved).getAssignedEntity();
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(2);
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(2);

        final Action uaction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);
        assertThat(uaction.getDistributionSet()).isEqualTo(ds);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(2);

        // Run test
        long current = System.currentTimeMillis();
        mvc.perform(get("/{tenant}/controller/v1/4712", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_HAL_UTF))
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.deploymentBase.href", startsWith("http://localhost/"
                        + tenantAware.getCurrentTenant() + "/controller/v1/4712/deploymentBase/" + uaction.getId())));
        assertThat(targetManagement.getByControllerID("4712").get().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);
        assertThat(targetManagement.getByControllerID("4712").get().getLastTargetQuery())
                .isLessThanOrEqualTo(System.currentTimeMillis());
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(2);

        current = System.currentTimeMillis();

        final DistributionSet findDistributionSetByAction = distributionSetManagement.getByAction(action.getId()).get();

        mvc.perform(
                get("/{tenant}/controller/v1/4712/deploymentBase/" + uaction.getId(), tenantAware.getCurrentTenant())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", equalTo(String.valueOf(action.getId()))))
                .andExpect(jsonPath("$.deployment.download", equalTo("forced")))
                .andExpect(jsonPath("$.deployment.update", equalTo("forced")))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==jvm)].name",
                        contains(ds.findFirstModuleByType(runtimeType).get().getName())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==jvm)].version",
                        contains(ds.findFirstModuleByType(runtimeType).get().getVersion())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].name",
                        contains(ds.findFirstModuleByType(osType).get().getName())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].version",
                        contains(ds.findFirstModuleByType(osType).get().getVersion())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[0].size", contains(5 * 1024)))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[0].filename", contains("test1")))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[0].hashes.md5",
                        contains(artifact.getMd5Hash())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[0].hashes.sha1",
                        contains(artifact.getSha1Hash())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[0]._links.download-http.href",
                        contains(
                                HTTP_LOCALHOST + tenantAware.getCurrentTenant() + "/controller/v1/4712/softwaremodules/"
                                        + findDistributionSetByAction.findFirstModuleByType(osType).get().getId()
                                        + "/artifacts/test1")))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[0]._links.md5sum-http.href",
                        contains(
                                HTTP_LOCALHOST + tenantAware.getCurrentTenant() + "/controller/v1/4712/softwaremodules/"
                                        + findDistributionSetByAction.findFirstModuleByType(osType).get().getId()
                                        + "/artifacts/test1.MD5SUM")))

                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[1].size", contains(5 * 1024)))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[1].filename",
                        contains("test1.signature")))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[1].hashes.md5",
                        contains(artifactSignature.getMd5Hash())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[1].hashes.sha1",
                        contains(artifactSignature.getSha1Hash())))

                .andExpect(
                        jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[1]._links.download-http.href",
                                contains(HTTP_LOCALHOST + tenantAware.getCurrentTenant()
                                        + "/controller/v1/4712/softwaremodules/"
                                        + findDistributionSetByAction.findFirstModuleByType(osType).get().getId()
                                        + "/artifacts/test1.signature")))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[1]._links.md5sum-http.href",
                        contains(
                                HTTP_LOCALHOST + tenantAware.getCurrentTenant() + "/controller/v1/4712/softwaremodules/"
                                        + findDistributionSetByAction.findFirstModuleByType(osType).get().getId()
                                        + "/artifacts/test1.signature.MD5SUM")))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==bApp)].version",
                        contains(ds.findFirstModuleByType(appType).get().getVersion())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==bApp)].name",
                        contains(ds.findFirstModuleByType(appType).get().getName())));

        // Retrieved is reported
        final Iterable<ActionStatus> actionStatusMessages = deploymentManagement
                .findActionStatusByAction(new PageRequest(0, 100, Direction.DESC, "id"), uaction.getId());
        assertThat(actionStatusMessages).hasSize(2);
        final ActionStatus actionStatusMessage = actionStatusMessages.iterator().next();
        assertThat(actionStatusMessage.getStatus()).isEqualTo(Status.RETRIEVED);
    }

    @Test
    @Description("Checks that the deployementBase URL changes when the action is switched from soft to forced in TIMEFORCED case.")
    public void changeEtagIfActionSwitchesFromSoftToForced() throws Exception {
        // Prepare test data
        final Target target = testdataFactory.createTarget("4712");
        final DistributionSet ds = testdataFactory.createDistributionSet("", true);

        final Long actionId = deploymentManagement.assignDistributionSet(ds.getId(), ActionType.TIMEFORCED,
                System.currentTimeMillis() + 2_000, Arrays.asList(target.getControllerId())).getActions().get(0);

        MvcResult mvcResult = mvc.perform(get("/{tenant}/controller/v1/4712", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_HAL_UTF)).andReturn();

        final String urlBeforeSwitch = JsonPath.compile("_links.deploymentBase.href")
                .read(mvcResult.getResponse().getContentAsString()).toString();

        // Time is not yet over, so we should see the same URL
        mvcResult = mvc.perform(get("/{tenant}/controller/v1/4712", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_HAL_UTF)).andReturn();
        assertThat(JsonPath.compile("_links.deploymentBase.href").read(mvcResult.getResponse().getContentAsString())
                .toString()).isEqualTo(urlBeforeSwitch)
                        .startsWith("http://localhost/" + tenantAware.getCurrentTenant()
                                + "/controller/v1/4712/deploymentBase/" + actionId);

        // After the time is over we should see a new etag
        TimeUnit.MILLISECONDS.sleep(2_000);

        mvcResult = mvc.perform(get("/{tenant}/controller/v1/4712", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_HAL_UTF)).andReturn();

        assertThat(JsonPath.compile("_links.deploymentBase.href").read(mvcResult.getResponse().getContentAsString())
                .toString()).isNotEqualTo(urlBeforeSwitch);
    }

    @Test
    @Description("Attempt/soft deployment to a controller. Checks if the resource reponse payload  for a given deployment is as expected.")
    public void deplomentAttemptAction() throws Exception {
        // Prepare test data
        final DistributionSet ds = testdataFactory.createDistributionSet("", true);
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true);
        final String visibleMetadataOsKey = "metaDataVisible";
        final String visibleMetadataOsValue = "withValue";

        final byte random[] = RandomUtils.nextBytes(5 * 1024);
        final Artifact artifact = artifactManagement.create(new ByteArrayInputStream(random), getOsModule(ds), "test1",
                false);
        final Artifact artifactSignature = artifactManagement.create(new ByteArrayInputStream(random), getOsModule(ds),
                "test1.signature", false);

        softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata().create(getOsModule(ds))
                .key(visibleMetadataOsKey).value(visibleMetadataOsValue).targetVisible(true));
        softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata().create(getOsModule(ds))
                .key("metaDataNotVisible").value("withValue").targetVisible(false));

        final Target savedTarget = testdataFactory.createTarget("4712");

        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).isEmpty();
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(0);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(0);

        List<Target> saved = deploymentManagement.assignDistributionSet(ds.getId(), ActionType.SOFT,
                RepositoryModelConstants.NO_FORCE_TIME, Arrays.asList(savedTarget.getControllerId()))
                .getAssignedEntity();
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(1);

        final Action action = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(1);
        saved = assignDistributionSet(ds2, saved).getAssignedEntity();
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(2);
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(2);

        final Action uaction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);
        assertThat(uaction.getDistributionSet()).isEqualTo(ds);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(2);

        // Run test

        final long current = System.currentTimeMillis();
        mvc.perform(get("/{tenant}/controller/v1/4712", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_HAL_UTF))
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.deploymentBase.href", startsWith("http://localhost/"
                        + tenantAware.getCurrentTenant() + "/controller/v1/4712/deploymentBase/" + uaction.getId())));
        assertThat(targetManagement.getByControllerID("4712").get().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);
        assertThat(targetManagement.getByControllerID("4712").get().getLastTargetQuery())
                .isLessThanOrEqualTo(System.currentTimeMillis());
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(2);

        final DistributionSet findDistributionSetByAction = distributionSetManagement.getByAction(action.getId()).get();

        mvc.perform(
                get("/{tenant}/controller/v1/4712/deploymentBase/" + uaction.getId(), tenantAware.getCurrentTenant())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", equalTo(String.valueOf(action.getId()))))
                .andExpect(jsonPath("$.deployment.download", equalTo("attempt")))
                .andExpect(jsonPath("$.deployment.update", equalTo("attempt")))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==jvm)].name",
                        contains(ds.findFirstModuleByType(runtimeType).get().getName())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==jvm)].version",
                        contains(ds.findFirstModuleByType(runtimeType).get().getVersion())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].name",
                        contains(ds.findFirstModuleByType(osType).get().getName())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].version",
                        contains(ds.findFirstModuleByType(osType).get().getVersion())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].metadata[0].key").value(visibleMetadataOsKey))
                .andExpect(
                        jsonPath("$.deployment.chunks[?(@.part==os)].metadata[0].value").value(visibleMetadataOsValue))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[0].size", contains(5 * 1024)))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[0].filename", contains("test1")))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[0].hashes.md5",
                        contains(artifact.getMd5Hash())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[0].hashes.sha1",
                        contains(artifact.getSha1Hash())))
                .andExpect(
                        jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[0]._links.download-http.href",
                                contains(HTTP_LOCALHOST + tenantAware.getCurrentTenant()
                                        + "/controller/v1/4712/softwaremodules/"
                                        + getOsModule(findDistributionSetByAction) + "/artifacts/test1")))
                .andExpect(
                        jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[0]._links.md5sum-http.href",
                                contains(HTTP_LOCALHOST + tenantAware.getCurrentTenant()
                                        + "/controller/v1/4712/softwaremodules/"
                                        + getOsModule(findDistributionSetByAction) + "/artifacts/test1.MD5SUM")))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[1].size", contains(5 * 1024)))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[1].filename",
                        contains("test1.signature")))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[1].hashes.md5",
                        contains(artifactSignature.getMd5Hash())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[1].hashes.sha1",
                        contains(artifactSignature.getSha1Hash())))
                .andExpect(
                        jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[1]._links.download-http.href",
                                contains(HTTP_LOCALHOST + tenantAware.getCurrentTenant()
                                        + "/controller/v1/4712/softwaremodules/"
                                        + getOsModule(findDistributionSetByAction) + "/artifacts/test1.signature")))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[1]._links.md5sum-http.href",
                        contains(HTTP_LOCALHOST + tenantAware.getCurrentTenant()
                                + "/controller/v1/4712/softwaremodules/" + getOsModule(findDistributionSetByAction)
                                + "/artifacts/test1.signature.MD5SUM")))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==bApp)].version",
                        contains(ds.findFirstModuleByType(appType).get().getVersion())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==bApp)].metadata").doesNotExist())
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==bApp)].name")
                        .value(ds.findFirstModuleByType(appType).get().getName()));

        // Retrieved is reported
        final List<ActionStatus> actionStatusMessages = deploymentManagement
                .findActionStatusByAction(new PageRequest(0, 100, Direction.DESC, "id"), uaction.getId()).getContent();
        assertThat(actionStatusMessages).hasSize(2);
        final ActionStatus actionStatusMessage = actionStatusMessages.iterator().next();
        assertThat(actionStatusMessage.getStatus()).isEqualTo(Status.RETRIEVED);
    }

    @Test
    @Description("Attempt/soft deployment to a controller including automated switch to hard. Checks if the resource reponse payload  for a given deployment is as expected.")
    public void deplomentAutoForceAction() throws Exception {
        // Prepare test data
        final DistributionSet ds = testdataFactory.createDistributionSet("", true);
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true);

        final byte random[] = RandomUtils.nextBytes(5 * 1024);
        final Artifact artifact = artifactManagement.create(new ByteArrayInputStream(random), getOsModule(ds), "test1",
                false);
        final Artifact artifactSignature = artifactManagement.create(new ByteArrayInputStream(random), getOsModule(ds),
                "test1.signature", false);

        final Target savedTarget = testdataFactory.createTarget("4712");

        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).isEmpty();
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(0);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(0);

        List<Target> saved = deploymentManagement.assignDistributionSet(ds.getId(), ActionType.TIMEFORCED,
                System.currentTimeMillis(), Arrays.asList(savedTarget.getControllerId())).getAssignedEntity();
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(1);

        final Action action = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(1);
        saved = assignDistributionSet(ds2, saved).getAssignedEntity();
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(2);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(2);

        final Action uaction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);
        assertThat(uaction.getDistributionSet()).isEqualTo(ds);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(2);

        // Run test

        long current = System.currentTimeMillis();
        mvc.perform(get("/{tenant}/controller/v1/4712", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_HAL_UTF))
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.deploymentBase.href", startsWith("http://localhost/"
                        + tenantAware.getCurrentTenant() + "/controller/v1/4712/deploymentBase/" + uaction.getId())));
        assertThat(targetManagement.getByControllerID("4712").get().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);
        assertThat(targetManagement.getByControllerID("4712").get().getLastTargetQuery())
                .isLessThanOrEqualTo(System.currentTimeMillis());
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(2);

        current = System.currentTimeMillis();

        final DistributionSet findDistributionSetByAction = distributionSetManagement.getByAction(action.getId()).get();

        mvc.perform(get("/{tenant}/controller/v1/4712/deploymentBase/{actionId}", tenantAware.getCurrentTenant(),
                uaction.getId()).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", equalTo(String.valueOf(action.getId()))))
                .andExpect(jsonPath("$.deployment.download", equalTo("forced")))
                .andExpect(jsonPath("$.deployment.update", equalTo("forced")))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==jvm)].name",
                        contains(ds.findFirstModuleByType(runtimeType).get().getName())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==jvm)].version",
                        contains(ds.findFirstModuleByType(runtimeType).get().getVersion())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].name",
                        contains(ds.findFirstModuleByType(osType).get().getName())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].version",
                        contains(ds.findFirstModuleByType(osType).get().getVersion())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[0].size", contains(5 * 1024)))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[0].filename", contains("test1")))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[0].hashes.md5",
                        contains(artifact.getMd5Hash())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[0].hashes.sha1",
                        contains(artifact.getSha1Hash())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[0]._links.download-http.href",
                        contains(
                                HTTP_LOCALHOST + tenantAware.getCurrentTenant() + "/controller/v1/4712/softwaremodules/"
                                        + findDistributionSetByAction.findFirstModuleByType(osType).get().getId()
                                        + "/artifacts/test1")))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[0]._links.md5sum-http.href",
                        contains(
                                HTTP_LOCALHOST + tenantAware.getCurrentTenant() + "/controller/v1/4712/softwaremodules/"
                                        + findDistributionSetByAction.findFirstModuleByType(osType).get().getId()
                                        + "/artifacts/test1.MD5SUM")))

                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[1].size", contains(5 * 1024)))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[1].filename",
                        contains("test1.signature")))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[1].hashes.md5",
                        contains(artifactSignature.getMd5Hash())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[1].hashes.sha1",
                        contains(artifactSignature.getSha1Hash())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[1]._links.download-http.href",
                        contains(
                                HTTP_LOCALHOST + tenantAware.getCurrentTenant() + "/controller/v1/4712/softwaremodules/"
                                        + findDistributionSetByAction.findFirstModuleByType(osType).get().getId()
                                        + "/artifacts/test1.signature")))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].artifacts[1]._links.md5sum-http.href",
                        contains(
                                HTTP_LOCALHOST + tenantAware.getCurrentTenant() + "/controller/v1/4712/softwaremodules/"
                                        + findDistributionSetByAction.findFirstModuleByType(osType).get().getId()
                                        + "/artifacts/test1.signature.MD5SUM")))

                .andExpect(jsonPath("$.deployment.chunks[?(@.part==bApp)].version",
                        contains(ds.findFirstModuleByType(appType).get().getVersion())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==bApp)].name",
                        contains(ds.findFirstModuleByType(appType).get().getName())));

        // Retrieved is reported
        final Iterable<ActionStatus> actionStatusMessages = deploymentManagement
                .findActionStatusByAction(new PageRequest(0, 100, Direction.DESC, "id"), uaction.getId()).getContent();
        assertThat(actionStatusMessages).hasSize(2);
        final ActionStatus actionStatusMessage = actionStatusMessages.iterator().next();
        assertThat(actionStatusMessage.getStatus()).isEqualTo(Status.RETRIEVED);
    }

    @Test
    @Description("Test various invalid access attempts to the deployment resource und the expected behaviour of the server.")
    public void badDeploymentAction() throws Exception {
        final Target target = testdataFactory.createTarget("4712");

        // not allowed methods
        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/1", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        mvc.perform(put("/{tenant}/controller/v1/4712/deploymentBase/1", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        mvc.perform(delete("/{tenant}/controller/v1/4712/deploymentBase/1", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        // non existing target
        mvc.perform(get("/{tenant}/controller/v1/4715/deploymentBase/1", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // no deployment
        mvc.perform(get("/controller/v1/4712/deploymentBase/1", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // wrong media type
        final List<Target> toAssign = Arrays.asList(target);
        final DistributionSet savedSet = testdataFactory.createDistributionSet("");

        final Long actionId = assignDistributionSet(savedSet, toAssign).getActions().get(0);
        mvc.perform(get("/{tenant}/controller/v1/4712/deploymentBase/" + actionId, tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        mvc.perform(get("/{tenant}/controller/v1/4712/deploymentBase/" + actionId, tenantAware.getCurrentTenant())
                .accept(MediaType.APPLICATION_ATOM_XML)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotAcceptable());
    }

    @Test
    @Description("The server protects itself against to many feedback upload attempts. The test verfies that "
            + "it is not possible to exceed the configured maximum number of feedback uplods.")
    public void tooMuchDeplomentActionFeedback() throws Exception {
        final Target target = testdataFactory.createTarget("4712");
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        assignDistributionSet(ds.getId(), "4712");
        final Action action = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent()
                .get(0);

        final String feedback = JsonBuilder.deploymentActionFeedback(action.getId().toString(), "proceeding");
        // assign distribution set creates an action status, so only 99 left
        for (int i = 0; i < 99; i++) {
            mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/" + action.getId() + "/feedback",
                    tenantAware.getCurrentTenant()).content(feedback).contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))
                    .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        }

        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/" + action.getId() + "/feedback",
                tenantAware.getCurrentTenant()).content(feedback).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isForbidden());
    }

    @Test
    @Description("The server protects itself against too large feedback bodies. The test verifies that "
            + "it is not possible to exceed the configured maximum number of feedback details.")
    public void tooMuchDeploymentActionMessagesInFeedback() throws Exception {
        final Target target = testdataFactory.createTarget("4712");
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        assignDistributionSet(ds.getId(), "4712");
        final Action action = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent()
                .get(0);

        final List<String> messages = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            messages.add(String.valueOf(i));
        }

        final String feedback = JsonBuilder.deploymentActionFeedback(action.getId().toString(), "proceeding", "none",
                messages);
        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/" + action.getId() + "/feedback",
                tenantAware.getCurrentTenant()).content(feedback).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isForbidden());

    }

    @Test
    @Description("Multiple uploads of deployment status feedback to the server.")
    public void multipleDeplomentActionFeedback() throws Exception {
        final Target savedTarget1 = testdataFactory.createTarget("4712");
        testdataFactory.createTarget("4713");
        testdataFactory.createTarget("4714");

        final DistributionSet ds1 = testdataFactory.createDistributionSet("1", true);
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true);
        final DistributionSet ds3 = testdataFactory.createDistributionSet("3", true);

        final List<Target> toAssign = new ArrayList<>();
        toAssign.add(savedTarget1);

        final Long actionId1 = assignDistributionSet(ds1.getId(), "4712").getActions().get(0);
        final Long actionId2 = assignDistributionSet(ds2.getId(), "4712").getActions().get(0);
        final Long actionId3 = assignDistributionSet(ds3.getId(), "4712").getActions().get(0);

        Target myT = targetManagement.getByControllerID("4712").get();
        assertThat(myT.getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, myT.getControllerId())).hasSize(3);
        assertThat(deploymentManagement.getAssignedDistributionSet(myT.getControllerId()).get()).isEqualTo(ds3);
        assertThat(deploymentManagement.getInstalledDistributionSet(myT.getControllerId())).isNotPresent();
        assertThat(targetManagement.findByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.UNKNOWN)).hasSize(2);

        // action1 done

        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/" + actionId1 + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.deploymentActionFeedback(actionId1.toString(), "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        myT = targetManagement.getByControllerID("4712").get();
        assertThat(myT.getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, myT.getControllerId())).hasSize(2);
        assertThat(deploymentManagement.getAssignedDistributionSet(myT.getControllerId()).get()).isEqualTo(ds3);
        assertThat(deploymentManagement.getInstalledDistributionSet(myT.getControllerId()).get()).isEqualTo(ds1);

        Iterable<ActionStatus> actionStatusMessages = deploymentManagement
                .findActionStatusAll(new PageRequest(0, 100, Direction.DESC, "id")).getContent();
        assertThat(actionStatusMessages).hasSize(4);
        assertThat(actionStatusMessages.iterator().next().getStatus()).isEqualTo(Status.FINISHED);

        // action2 done
        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/" + actionId2 + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.deploymentActionFeedback(actionId2.toString(), "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        myT = targetManagement.getByControllerID("4712").get();

        assertThat(myT.getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, myT.getControllerId())).hasSize(1);
        assertThat(deploymentManagement.getAssignedDistributionSet(myT.getControllerId()).get()).isEqualTo(ds3);
        assertThat(deploymentManagement.getInstalledDistributionSet(myT.getControllerId()).get()).isEqualTo(ds2);
        actionStatusMessages = deploymentManagement.findActionStatusAll(new PageRequest(0, 100, Direction.DESC, "id"))
                .getContent();
        assertThat(actionStatusMessages).hasSize(5);
        assertThat(actionStatusMessages).haveAtLeast(1, new ActionStatusCondition(Status.FINISHED));

        // action3 done
        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/" + actionId3 + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.deploymentActionFeedback(actionId3.toString(), "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        myT = targetManagement.getByControllerID("4712").get();
        assertThat(myT.getUpdateStatus()).isEqualTo(TargetUpdateStatus.IN_SYNC);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, myT.getControllerId())).hasSize(0);
        assertThat(deploymentManagement.getAssignedDistributionSet(myT.getControllerId()).get()).isEqualTo(ds3);
        assertThat(deploymentManagement.getInstalledDistributionSet(myT.getControllerId()).get()).isEqualTo(ds3);
        actionStatusMessages = deploymentManagement.findActionStatusAll(new PageRequest(0, 100, Direction.DESC, "id"))
                .getContent();
        assertThat(actionStatusMessages).hasSize(6);
        assertThat(actionStatusMessages).haveAtLeast(1, new ActionStatusCondition(Status.FINISHED));

    }

    @Test
    @Description("Verfies that an update action is correctly set to error if the controller provides error feedback.")
    public void rootRsSingleDeplomentActionWithErrorFeedback() throws Exception {
        DistributionSet ds = testdataFactory.createDistributionSet("");
        final Target savedTarget = testdataFactory.createTarget("4712");

        final List<Target> toAssign = new ArrayList<>();
        toAssign.add(savedTarget);

        assertThat(targetManagement.getByControllerID("4712").get().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.UNKNOWN);
        assignDistributionSet(ds, toAssign);
        final Action action = deploymentManagement.findActionsByDistributionSet(PAGE, ds.getId()).getContent().get(0);

        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/" + action.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.deploymentActionFeedback(action.getId().toString(), "closed", "failure",
                                "error message"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        Target myT = targetManagement.getByControllerID("4712").get();
        assertThat(targetManagement.getByControllerID("4712").get().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.ERROR);
        assertThat(targetManagement.findByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.PENDING)).hasSize(0);
        assertThat(targetManagement.findByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.ERROR)).hasSize(1);
        assertThat(targetManagement.findByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.IN_SYNC)).hasSize(0);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, myT.getControllerId())).hasSize(0);
        assertThat(deploymentManagement.countActionsByTarget(myT.getControllerId())).isEqualTo(1);
        final Iterable<ActionStatus> actionStatusMessages = deploymentManagement
                .findActionStatusAll(new PageRequest(0, 100, Direction.DESC, "id")).getContent();
        assertThat(actionStatusMessages).hasSize(2);
        assertThat(actionStatusMessages).haveAtLeast(1, new ActionStatusCondition(Status.ERROR));

        // redo
        ds = distributionSetManagement.getWithDetails(ds.getId()).get();
        assignDistributionSet(ds, Arrays.asList(targetManagement.getByControllerID("4712").get()));
        final Action action2 = deploymentManagement.findActiveActionsByTarget(PAGE, myT.getControllerId()).getContent()
                .get(0);

        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/" + action2.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.deploymentActionFeedback(action2.getId().toString(), "closed", "success"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        myT = targetManagement.getByControllerID("4712").get();
        assertThat(myT.getUpdateStatus()).isEqualTo(TargetUpdateStatus.IN_SYNC);
        assertThat(targetManagement.findByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.PENDING)).hasSize(0);
        assertThat(targetManagement.findByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.ERROR)).hasSize(0);
        assertThat(targetManagement.findByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.IN_SYNC)).hasSize(1);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, myT.getControllerId())).hasSize(0);
        assertThat(deploymentManagement.findInActiveActionsByTarget(PAGE, myT.getControllerId())).hasSize(2);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(4);
        assertThat(deploymentManagement.findActionStatusByAction(PAGE, action.getId()).getContent()).haveAtLeast(1,
                new ActionStatusCondition(Status.ERROR));
        assertThat(deploymentManagement.findActionStatusByAction(PAGE, action2.getId()).getContent()).haveAtLeast(1,
                new ActionStatusCondition(Status.FINISHED));

    }

    @Test
    @Description("Verfies that the controller can provided as much feedback entries as necessry as long as it is in the configured limites.")
    public void rootRsSingleDeplomentActionFeedback() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        final Target savedTarget = testdataFactory.createTarget("4712");

        final List<Target> toAssign = Arrays.asList(savedTarget);

        Target myT = targetManagement.getByControllerID("4712").get();
        assertThat(myT.getUpdateStatus()).isEqualTo(TargetUpdateStatus.UNKNOWN);
        assignDistributionSet(ds, toAssign);
        final Action action = deploymentManagement.findActionsByDistributionSet(PAGE, ds.getId()).getContent().get(0);

        myT = targetManagement.getByControllerID("4712").get();
        assertThat(myT.getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(targetManagement.findByInstalledDistributionSet(PAGE, ds.getId())).hasSize(0);
        assertThat(targetManagement.findByAssignedDistributionSet(PAGE, ds.getId())).hasSize(1);

        // Now valid Feedback
        for (int i = 0; i < 4; i++) {
            mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/" + action.getId() + "/feedback",
                    tenantAware.getCurrentTenant())
                            .content(JsonBuilder.deploymentActionFeedback(action.getId().toString(), "proceeding"))
                            .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                    .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        }

        myT = targetManagement.getByControllerID("4712").get();
        assertThat(myT.getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(targetManagement.findByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.PENDING)).hasSize(1);
        assertThat(targetManagement.findByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.ERROR)).hasSize(0);
        assertThat(targetManagement.findByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.IN_SYNC)).hasSize(0);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, myT.getControllerId())).hasSize(1);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(5);
        assertThat(deploymentManagement.findActionStatusAll(PAGE).getContent()).haveAtLeast(5,
                new ActionStatusCondition(Status.RUNNING));

        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/" + action.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.deploymentActionFeedback(action.getId().toString(), "scheduled"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        myT = targetManagement.getByControllerID("4712").get();
        assertThat(myT.getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(targetManagement.findByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.PENDING)).hasSize(1);
        assertThat(targetManagement.findByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.ERROR)).hasSize(0);
        assertThat(targetManagement.findByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.IN_SYNC)).hasSize(0);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, myT.getControllerId())).hasSize(1);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(6);
        assertThat(deploymentManagement.findActionStatusAll(PAGE).getContent()).haveAtLeast(5,
                new ActionStatusCondition(Status.RUNNING));

        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/" + action.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.deploymentActionFeedback(action.getId().toString(), "resumed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        myT = targetManagement.getByControllerID("4712").get();
        assertThat(myT.getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(targetManagement.findByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.PENDING)).hasSize(1);
        assertThat(targetManagement.findByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.ERROR)).hasSize(0);
        assertThat(targetManagement.findByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.IN_SYNC)).hasSize(0);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, myT.getControllerId())).hasSize(1);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(7);
        assertThat(deploymentManagement.findActionStatusAll(PAGE).getContent()).haveAtLeast(6,
                new ActionStatusCondition(Status.RUNNING));

        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/" + action.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.deploymentActionFeedback(action.getId().toString(), "canceled"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        myT = targetManagement.getByControllerID("4712").get();
        assertThat(myT.getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, myT.getControllerId())).hasSize(1);
        assertThat(targetManagement.findByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.PENDING)).hasSize(1);
        assertThat(targetManagement.findByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.ERROR)).hasSize(0);
        assertThat(targetManagement.findByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.IN_SYNC)).hasSize(0);

        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(8);
        assertThat(deploymentManagement.findActionStatusAll(PAGE).getContent()).haveAtLeast(7,
                new ActionStatusCondition(Status.RUNNING));
        assertThat(deploymentManagement.findActionStatusAll(PAGE).getContent()).haveAtLeast(1,
                new ActionStatusCondition(Status.CANCELED));

        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/" + action.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.deploymentActionFeedback(action.getId().toString(), "rejected"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        myT = targetManagement.getByControllerID("4712").get();
        assertThat(myT.getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, myT.getControllerId())).hasSize(1);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(9);
        assertThat(deploymentManagement.findActionStatusAll(PAGE).getContent()).haveAtLeast(6,
                new ActionStatusCondition(Status.RUNNING));
        assertThat(deploymentManagement.findActionStatusAll(PAGE).getContent()).haveAtLeast(1,
                new ActionStatusCondition(Status.WARNING));
        assertThat(deploymentManagement.findActionStatusAll(PAGE).getContent()).haveAtLeast(1,
                new ActionStatusCondition(Status.CANCELED));

        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/" + action.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.deploymentActionFeedback(action.getId().toString(), "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        myT = targetManagement.getByControllerID("4712").get();
        assertThat(myT.getUpdateStatus()).isEqualTo(TargetUpdateStatus.IN_SYNC);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, myT.getControllerId())).hasSize(0);
        assertThat(targetManagement.findByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.ERROR)).hasSize(0);
        assertThat(targetManagement.findByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.IN_SYNC)).hasSize(1);

        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(10);
        assertThat(deploymentManagement.findActionStatusAll(PAGE).getContent()).haveAtLeast(7,
                new ActionStatusCondition(Status.RUNNING));
        assertThat(deploymentManagement.findActionStatusAll(PAGE).getContent()).haveAtLeast(1,
                new ActionStatusCondition(Status.WARNING));
        assertThat(deploymentManagement.findActionStatusAll(PAGE).getContent()).haveAtLeast(1,
                new ActionStatusCondition(Status.CANCELED));
        assertThat(deploymentManagement.findActionStatusAll(PAGE).getContent()).haveAtLeast(1,
                new ActionStatusCondition(Status.FINISHED));

        assertThat(targetManagement.findByInstalledDistributionSet(PAGE, ds.getId())).hasSize(1);
        assertThat(targetManagement.findByAssignedDistributionSet(PAGE, ds.getId())).hasSize(1);
    }

    @Test
    @Description("Various forbidden request appempts on the feedback resource. Ensures correct answering behaviour as expected to these kind of errors.")
    public void badDeplomentActionFeedback() throws Exception {
        final DistributionSet savedSet = testdataFactory.createDistributionSet("");
        final DistributionSet savedSet2 = testdataFactory.createDistributionSet("1");

        // target does not exist
        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/1234/feedback", tenantAware.getCurrentTenant())
                .content(JsonBuilder.deploymentActionInProgressFeedback("1234")).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        Target savedTarget = testdataFactory.createTarget("4712");
        final Target savedTarget2 = testdataFactory.createTarget("4713");

        // Action does not exists
        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/1234/feedback", tenantAware.getCurrentTenant())
                .content(JsonBuilder.deploymentActionInProgressFeedback("1234")).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        final List<Target> toAssign = Arrays.asList(savedTarget);
        final List<Target> toAssign2 = Arrays.asList(savedTarget2);

        savedTarget = assignDistributionSet(savedSet, toAssign).getAssignedEntity().iterator().next();
        assignDistributionSet(savedSet2, toAssign2);

        final Action updateAction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);

        // action exists but is not assigned to this target
        mvc.perform(post("/{tenant}/controller/v1/4713/deploymentBase/" + updateAction.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.deploymentActionInProgressFeedback(updateAction.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // not allowed methods
        mvc.perform(get("/{tenant}/controller/v1/4712/deploymentBase/2/feedback", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        mvc.perform(put("/{tenant}/controller/v1/4712/deploymentBase/2/feedback", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        mvc.perform(delete("/{tenant}/controller/v1/4712/deploymentBase/2/feedback", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

    }

    @Test
    @Description("Ensures that an invalid id in feedback body returns a bad request.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 1) })
    public void invalidIdInFeedbackReturnsBadRequest() throws Exception {
        final Target target = testdataFactory.createTarget("1080");
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        assignDistributionSet(ds.getId(), "1080");
        final Action action = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent()
                .get(0);

        mvc.perform(post("/{tenant}/controller/v1/1080/deploymentBase/" + action.getId() + "/feedback",
                tenantAware.getCurrentTenant()).content(JsonBuilder.deploymentActionInProgressFeedback("AAAA"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ensures that a missing feedback result in feedback body returns a bad request.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 1) })
    public void missingResultAttributeInFeedbackReturnsBadRequest() throws Exception {

        final Target target = testdataFactory.createTarget("1080");
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        assignDistributionSet(ds.getId(), "1080");
        final Action action = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent()
                .get(0);
        final String missingResultInFeedback = JsonBuilder.missingResultInFeedback(action.getId().toString(), "closed",
                "test");
        mvc.perform(post("/{tenant}/controller/v1/1080/deploymentBase/" + action.getId() + "/feedback",
                tenantAware.getCurrentTenant()).content(missingResultInFeedback).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

    }

    @Test
    @Description("Ensures that a missing finished result in feedback body returns a bad request.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 1) })
    public void missingFinishedAttributeInFeedbackReturnsBadRequest() throws Exception {

        final Target target = testdataFactory.createTarget("1080");
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        assignDistributionSet(ds.getId(), "1080");

        final Action action = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent()
                .get(0);
        final String missingFinishedResultInFeedback = JsonBuilder
                .missingFinishedResultInFeedback(action.getId().toString(), "closed", "test");
        mvc.perform(post("/{tenant}/controller/v1/1080/deploymentBase/" + action.getId() + "/feedback",
                tenantAware.getCurrentTenant()).content(missingFinishedResultInFeedback)
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    private class ActionStatusCondition extends Condition<ActionStatus> {
        private final Status status;

        public ActionStatusCondition(final Status status) {
            this.status = status;
        }

        @Override
        public boolean matches(final ActionStatus actionStatus) {
            return actionStatus.getStatus() == status;
        }
    }
}
